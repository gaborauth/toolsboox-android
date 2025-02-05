package com.toolsboox.plugin.cloud.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.textfield.TextInputEditText
import com.google.api.services.drive.DriveScopes
import com.google.firebase.analytics.FirebaseAnalytics
import com.squareup.moshi.Moshi
import com.toolsboox.R
import com.toolsboox.da.Credential
import com.toolsboox.databinding.FragmentCloudBinding
import com.toolsboox.fi.BillingClientService
import com.toolsboox.ot.CryptoUtils
import com.toolsboox.plugin.cloud.da.Purchase
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.time.Instant
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Cloud main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CloudFragment @Inject constructor() : ScreenFragment() {

    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

    /**
     * The injected presenter.
     */
    @Inject
    lateinit var presenter: CloudPresenter

    /**
     * The Firebase analytics.
     */
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The injected shared preferences.
     */
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_cloud

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCloudBinding

    // Flag of product details finished.
    private var productDetailsFinished = false

    // Flag of subscription list finished.
    private var subscriptionListFinished = false

    // The map of subscriptions (productId - token).
    private val subscriptions: MutableMap<String, String> = mutableMapOf()

    // Access token for Google Drive.
    private var googleAccount: GoogleSignInAccount? = null

    // Google sign-in client.
    private var signInClient: GoogleSignInClient? = null

    // Google sign-in options.
    private val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA), Scope(DriveScopes.DRIVE_FILE))
        .requestEmail()
        .build()

    private val connectResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .addOnSuccessListener {
                    googleAccount = it
                    Timber.i("Signed into a GoogleAccount: ${it.email}")
                    requireActivity().runOnUiThread { updateButtons() }
                }
        } else {
            googleAccount = null
            val message = getString(R.string.cloud_google_drive_connection_failed_toast).format("failed")
            Toast.makeText(this.context, message, Toast.LENGTH_LONG).show()
            updateButtons()
        }
    }

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCloudBinding.bind(view)

        binding.cloudAccountSignUpButton.setOnClickListener {
            signUpDialog()
        }

        binding.cloudAccountLoginButton.setOnClickListener {
            val refreshToken = sharedPreferences.getString("refreshToken", null)
            if (refreshToken == null) {
                loginDialog()
            } else {
                logoutDialog()
            }
        }

        binding.cloudGoogleDriveConnectButton.setOnClickListener {
            signInClient = GoogleSignIn.getClient(this.requireContext(), googleSignInOptions)
            connectResult.launch(signInClient!!.signInIntent)
        }

        binding.cloudGoogleDriveDisconnectButton.setOnClickListener {
            if (signInClient == null) return@setOnClickListener

            signInClient!!.revokeAccess().addOnCompleteListener {
                Timber.i("Google Drive scopes revoked")
                googleAccount = null
                requireActivity().runOnUiThread { updateButtons() }
            }
        }

        binding.cloudMonthlyButton.setOnClickListener {
            subscriptionFlow("monthly")
        }

        binding.cloudYearlyButton.setOnClickListener {
            subscriptionFlow("yearly")
        }

        // Test of crypto utility compatibility.
        lifecycleScope.launchWhenResumed {
            val encrypted = CryptoUtils.encrypt("test-data".toByteArray(), "pass1234")
            Timber.e("Encrypted Android:    " + Base64.getEncoder().encodeToString(encrypted))
            val decrypted = CryptoUtils.decrypt(encrypted, "pass1234")
            Timber.e("Decrypted Android:    " + String(decrypted))
            val encryptedJavaScript = "U2FsdGVkX19+eYEXdhMkJPCnPpCCU125gBbr+6/voJU="
            val decryptedJavaScript = CryptoUtils.decrypt(Base64.getDecoder().decode(encryptedJavaScript), "pass1234")
            Timber.e("Decrypted JavaScript: " + String(decryptedJavaScript))
            val encryptedOpenSSL = "U2FsdGVkX19Ofjk/W1o+wr8TlKyVB+0XU1WbSkLTFvw="
            val decryptedOpenSSL = CryptoUtils.decrypt(Base64.getDecoder().decode(encryptedOpenSSL), "pass1234")
            Timber.e("Decrypted OpenSSL:    " + String(decryptedOpenSSL))
        }

        htmlLinks(
            binding.cloudSubscriptionMessage, R.string.cloud_subscription_message,
            "https://discord.gg/S3sKsbmaSk"
        )
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.cloud_title))

        // Update state of the buttons.
        updateButtons()

        BillingClientService.connectClient(
            requireActivity(),
            { billingClient ->
                BillingClientService.subscriptionDetails(
                    billingClient, "cloud_v1",
                    { productDetails ->
                        if (productDetails.isNotEmpty()) {
                            Timber.i("ProductDetails: $productDetails")
                            requireActivity().runOnUiThread {
                                productDetails[0].subscriptionOfferDetails?.forEach { offer ->
                                    val price = offer.pricingPhases.pricingPhaseList[0].formattedPrice
                                    if (offer.basePlanId == "monthly") {
                                        val buttonText = getString(R.string.cloud_subscription_monthly_button).format(price)
                                        binding.cloudMonthlyButton.text = buttonText
                                    }
                                    if (offer.basePlanId == "yearly") {
                                        val buttonText = getString(R.string.cloud_subscription_yearly_button).format(price)
                                        binding.cloudYearlyButton.text = buttonText
                                    }
                                }
                            }
                        }
                        productDetailsFinished = true
                        requireActivity().runOnUiThread { updateButtons() }
                    },
                    { result ->
                        Timber.w("OnFailed: $result")
                        productDetailsFinished = false
                        requireActivity().runOnUiThread { updateButtons() }
                    })

                BillingClientService.subscriptions(
                    billingClient,
                    { purchaseList ->
                        Timber.i("Subscriptions: $purchaseList")
                        var status = getString(R.string.cloud_subscription_status_no_subs)
                        if (purchaseList.isNotEmpty()) {
                            status = getString(R.string.cloud_subscription_status_subs)
                            val purchase = purchaseList[0]
                            Timber.i("Purchase: $purchase")
                            val productId = purchase.products[0]
                            val purchaseToken = purchase.purchaseToken
                            Timber.i("ProductId: $productId, purchaseToken: $purchaseToken")
                            subscriptions[productId] = purchaseToken

                            // Acknowledge the purchase.
                            if (!purchase.isAcknowledged) {
                                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchase.purchaseToken)
                                    .build()

                                billingClient.acknowledgePurchase(acknowledgePurchaseParams, { acknowledgePurchaseResult ->
                                    if (acknowledgePurchaseResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                        Timber.i("Purchase acknowledged: $purchaseToken")
                                        firebaseAnalytics.logEvent("purchaseAcknowledged", null)
                                    }
                                })
                            }

                            val userId = sharedPreferences.getString("userId", null)
                            if (userId != null) {
                                moshi.adapter(Purchase::class.java).fromJson(purchase.originalJson)?.let {
                                    presenter.updatePurchase(this@CloudFragment, UUID.fromString(userId), it)
                                }
                            }
                        }
                        subscriptionListFinished = true
                        requireActivity().runOnUiThread {
                            binding.cloudSubscriptionStatusMessage.text = getString(R.string.cloud_subscription_status_message).format(status)
                            updateButtons()
                        }
                    },
                    { result ->
                        Timber.w("OnFailed: $result")
                        subscriptionListFinished = false
                        requireActivity().runOnUiThread { updateButtons() }
                    })
            },
            { result ->
                Timber.w("OnFailed: $result")
                productDetailsFinished = false
                subscriptionListFinished = false
                requireActivity().runOnUiThread { updateButtons() }
            }
        )

        // Check Google Drive connection.
        signInClient = GoogleSignIn.getClient(this.requireContext(), googleSignInOptions)
        signInClient!!.silentSignIn()
            .addOnSuccessListener { result ->
                Timber.i("Silent-signed into a GoogleAccount: $result.id")
                googleAccount = result
                requireActivity().runOnUiThread {
                    updateButtons()
                }
            }
            .addOnFailureListener {
                Timber.e("Silent-sign-in failed: $it")
                googleAccount = null
                requireActivity().runOnUiThread {
                    updateButtons()
                }
            }
    }

    /**
     * Create credential response (success).
     *
     * @param signUpDialog the sign-up dialog.
     * @param signUpView the sign-up view.
     * @param result the created credential result.
     */
    fun createCredentialResult(signUpDialog: DialogInterface, signUpView: View, result: Credential) {
        Timber.i("Create credential result: $result")

        val usernameEditText = signUpView.findViewById<TextInputEditText>(R.id.username_edit_text)
        usernameEditText.error = null

        sharedPreferences.edit().putString("userId", result.userId.toString()).apply()
        sharedPreferences.edit().putString("userIdKey", CryptoUtils.getKey(result.userId)).apply()

        sharedPreferences.edit().putString("username", result.username).apply()

        updateButtons()
        signUpDialog.dismiss()
    }

    /**
     * Create credential response (conflict).
     *
     * @param signUpView the sign-up view.
     */
    fun createCredentialConflict(signUpView: View) {
        Timber.i("Create credential result: conflict")

        val usernameEditText = signUpView.findViewById<TextInputEditText>(R.id.username_edit_text)
        usernameEditText.error = getString(R.string.cloud_account_sign_up_view_username_conflict_error)
        usernameEditText.requestFocus()

        updateButtons()
    }

    /**
     * Login credential response (success).
     *
     * @param loginDialog the login dialog.
     * @param loginView the login view.
     * @param credential the login result
     */
    fun loginCredentialResult(loginDialog: DialogInterface, loginView: View, credential: Credential) {
        Timber.i("Login credential result: $credential")

        val usernameEditText = loginView.findViewById<TextInputEditText>(R.id.username_edit_text)
        usernameEditText.error = null

        sharedPreferences.edit().putString("userId", credential.userId.toString()).apply()
        sharedPreferences.edit().putString("userIdKey", CryptoUtils.getKey(credential.userId)).apply()

        sharedPreferences.edit().putString("username", credential.username).apply()

        sharedPreferences.edit().putString("refreshToken", credential.refreshToken).apply()
        sharedPreferences.edit().putLong("refreshTokenLastUpdate", Date.from(Instant.now()).time).apply()
        sharedPreferences.edit().putString("accessToken", credential.accessToken).apply()
        sharedPreferences.edit().putLong("accessTokenLastUpdate", Date.from(Instant.now()).time).apply()

        updateButtons()
        loginDialog.dismiss()
    }

    /**
     * Login credential failed.
     *
     * @param loginView the login view.
     */
    fun loginCredentialFailed(loginView: View, result: Credential?) {
        Timber.i("Login credential result: failed ($result)")

        val usernameEditText = loginView.findViewById<TextInputEditText>(R.id.username_edit_text)
        usernameEditText.error = getString(R.string.cloud_account_log_in_failed_dialog_message)
        usernameEditText.requestFocus()

        updateButtons()
    }

    /**
     * Update purchase response.
     *
     * @param result the purchase result.
     */
    fun updatePurchaseResult(result: Purchase) {
        Timber.i("Update result: $result")
    }

    /**
     * Update button states.
     */
    private fun updateButtons() {
        val loading = getString(R.string.cloud_loading)
        val loggedIn = getString(R.string.cloud_logged_in)
        val notLoggedIn = getString(R.string.cloud_not_logged_in)

        val userIdKey = sharedPreferences.getString("userIdKey", null)
        val username = sharedPreferences.getString("username", null)
        if (userIdKey == null || username == null) {
            binding.cloudAccountLoginUserIdMessage.text = getString(R.string.cloud_account_login_user_id_message).format(notLoggedIn)
            binding.cloudAccountLoginUsernameMessage.text = getString(R.string.cloud_account_login_username_message).format(notLoggedIn)
        } else {
            binding.cloudAccountLoginUserIdMessage.text = getString(R.string.cloud_account_login_user_id_message).format(userIdKey)
            binding.cloudAccountLoginUsernameMessage.text = getString(R.string.cloud_account_login_username_message).format(username)
        }

        val refreshToken = sharedPreferences.getString("refreshToken", null)
        if (refreshToken == null) {
            binding.cloudAccountLoginStatusMessage.text = getString(R.string.cloud_account_login_status_message).format(notLoggedIn)
            binding.cloudAccountLoginButton.text = getString(R.string.cloud_account_log_in_button)
            binding.cloudAccountSignUpButton.isEnabled = true
            binding.cloudAccountSignUpButton.alpha = 1.0f
        } else {
            binding.cloudAccountLoginStatusMessage.text = getString(R.string.cloud_account_login_status_message).format(loggedIn)
            binding.cloudAccountLoginButton.text = getString(R.string.cloud_account_log_out_button)
            binding.cloudAccountSignUpButton.isEnabled = false
            binding.cloudAccountSignUpButton.alpha = 0.5f
        }

        if (googleAccount == null) {
            binding.cloudGoogleDriveStatusMessage.text = getString(R.string.cloud_google_drive_status_not_connected)
            binding.cloudGoogleDriveConnectButton.isEnabled = true
            binding.cloudGoogleDriveConnectButton.alpha = 1.0f
            binding.cloudGoogleDriveDisconnectButton.isEnabled = false
            binding.cloudGoogleDriveDisconnectButton.alpha = 0.5f
        } else {
            binding.cloudGoogleDriveStatusMessage.text = getString(R.string.cloud_google_drive_status_connected)
            binding.cloudGoogleDriveConnectButton.isEnabled = false
            binding.cloudGoogleDriveConnectButton.alpha = 0.5f
            binding.cloudGoogleDriveDisconnectButton.isEnabled = true
            binding.cloudGoogleDriveDisconnectButton.alpha = 1.0f
        }

        if (!productDetailsFinished) {
            binding.cloudMonthlyButton.text = getString(R.string.cloud_subscription_monthly_button).format(loading)
            binding.cloudYearlyButton.text = getString(R.string.cloud_subscription_yearly_button).format(loading)
        }
        if (!subscriptionListFinished) {
            binding.cloudSubscriptionStatusMessage.text = getString(R.string.cloud_subscription_status_message).format(loading)
        }

        binding.cloudMonthlyButton.isEnabled = productDetailsFinished && !subscriptions.contains("cloud_v1")
        binding.cloudMonthlyButton.alpha = if (binding.cloudMonthlyButton.isEnabled) 1.0f else 0.5f

        binding.cloudYearlyButton.isEnabled = productDetailsFinished && !subscriptions.contains("cloud_v1")
        binding.cloudYearlyButton.alpha = if (binding.cloudYearlyButton.isEnabled) 1.0f else 0.5f
    }

    /**
     * Start the subscription flow.
     *
     * @param basePlan the name of the base plan
     */
    private fun subscriptionFlow(basePlan: String) {
        Timber.i("Checking '%s' offer of cloud_v1 product...", basePlan)
        BillingClientService.connectClient(
            requireActivity(),
            { billingClient ->
                BillingClientService.subscriptionDetails(
                    billingClient, "cloud_v1",
                    { productDetails ->
                        if (productDetails.isNotEmpty()) {
                            val offers = productDetails[0].subscriptionOfferDetails ?: return@subscriptionDetails
                            val offer = offers.firstOrNull { offer -> offer.basePlanId == basePlan } ?: return@subscriptionDetails

                            val productDetailsParamsList = listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails[0])
                                    .setOfferToken(offer.offerToken)
                                    .build()
                            )

                            Timber.i("Starting billing flow...")
                            val billingFlowParams = BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(productDetailsParamsList)
                                .build()

                            val billingResult = billingClient.launchBillingFlow(requireActivity(), billingFlowParams)
                            Timber.i("BillingResult: $billingResult")
                        }
                    },
                    { _ -> }
                )
            },
            { _ -> }
        )
    }

    /**
     * Displays the sign-up dialog.
     */
    private fun signUpDialog() {
        val signUpDialog = AlertDialog.Builder(requireContext())

        val signUpView = requireActivity().layoutInflater.inflate(R.layout.fragment_cloud_sign_up_view, null)
        val usernameEditText = signUpView.findViewById<TextInputEditText>(R.id.username_edit_text)
        val passwordEditText = signUpView.findViewById<TextInputEditText>(R.id.password_edit_text)

        signUpDialog.setTitle(R.string.cloud_account_sign_up_dialog_title)
        signUpDialog.setView(signUpView)

        signUpDialog.setPositiveButton(R.string.cloud_account_sign_up_button) { _, _ -> }
        signUpDialog.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }

        val dialog = signUpDialog.show()
        val signUpButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        signUpButton.isEnabled = false

        // Redefine onClick to prevent dialog from closing
        signUpButton.setOnClickListener {
            val password = passwordEditText.text.toString()

            val username = usernameEditText.text.toString()
            val hash = CryptoUtils.md5Hash(password.toByteArray(Charsets.UTF_8))
            presenter.createCredential(this@CloudFragment, dialog, signUpView, username, hash)
        }

        val usernamePattern = Pattern.compile("^[a-z]{5}[a-z0-9]{0,20}$")
        val passwordPattern = Pattern.compile("^.{8,}$")

        usernameEditText.addTextChangedListener {
            signUpButton.isEnabled = usernamePattern.matcher(usernameEditText.text.toString()).matches() &&
                    passwordPattern.matcher(passwordEditText.text.toString()).matches()
        }
        passwordEditText.addTextChangedListener {
            signUpButton.isEnabled = usernamePattern.matcher(usernameEditText.text.toString()).matches() &&
                    passwordPattern.matcher(passwordEditText.text.toString()).matches()
        }
    }

    /**
     * Displays the log in dialog.
     */
    private fun loginDialog() {
        val loginDialog = AlertDialog.Builder(requireContext())

        val loginView = requireActivity().layoutInflater.inflate(R.layout.fragment_cloud_log_in_view, null)
        val usernameEditText = loginView.findViewById<TextInputEditText>(R.id.username_edit_text)
        val passwordEditText = loginView.findViewById<TextInputEditText>(R.id.password_edit_text)

        sharedPreferences.getString("username", null)?.let { usernameEditText.setText(it) }

        loginDialog.setTitle(R.string.cloud_account_log_in_dialog_title)
        loginDialog.setView(loginView)

        loginDialog.setPositiveButton(R.string.cloud_account_log_in_button) { _, _ -> }
        loginDialog.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }

        val dialog = loginDialog.show()
        val loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        loginButton.isEnabled = false

        // Redefine onClick to prevent dialog from closing
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            val hash = CryptoUtils.md5Hash(password.toByteArray(Charsets.UTF_8))
            presenter.loginCredential(this@CloudFragment, dialog, loginView, username, hash)
        }

        val usernamePattern = Pattern.compile("^[a-z]{5}[a-z0-9]{0,20}$")
        val passwordPattern = Pattern.compile("^.{8,}$")

        usernameEditText.addTextChangedListener {
            loginButton.isEnabled = usernamePattern.matcher(usernameEditText.text.toString()).matches() &&
                    passwordPattern.matcher(passwordEditText.text.toString()).matches()
        }
        passwordEditText.addTextChangedListener {
            loginButton.isEnabled = usernamePattern.matcher(usernameEditText.text.toString()).matches() &&
                    passwordPattern.matcher(passwordEditText.text.toString()).matches()
        }
    }

    /**
     * Displays the log-out dialog.
     */
    private fun logoutDialog() {
        val logOutDialog = AlertDialog.Builder(requireContext())

        logOutDialog.setTitle(R.string.cloud_account_log_out_dialog_title)
        logOutDialog.setMessage(R.string.cloud_account_log_out_dialog_message)

        logOutDialog.setPositiveButton(R.string.cloud_account_log_out_button) { dialog, _ ->
            sharedPreferences.edit().remove("refreshToken").apply()
            sharedPreferences.edit().remove("refreshTokenLastUpdate").apply()
            sharedPreferences.edit().remove("accessToken").apply()
            sharedPreferences.edit().remove("accessTokenLastUpdate").apply()

            dialog.dismiss()

            updateButtons()
        }

        logOutDialog.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }

        logOutDialog.show()
    }

    /**
     * Update textView content to clickable link.
     *
     * @param linkView the link holder textView
     * @param messageResId the message resource id
     * @param link the link
     */
    private fun htmlLinks(linkView: TextView, messageResId: Int, link: String) {
        val linkMessage = getString(messageResId)
        val linkHtml = "$linkMessage <a href=\"$link\">$link</a>"
        linkView.text = Html.fromHtml(linkHtml, Html.FROM_HTML_MODE_COMPACT)
        linkView.setOnClickListener {
            this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }
    }

    /**
     * Show the progress bar.
     */
    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    /**
     * Hide the progress bar.
     */
    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }
}

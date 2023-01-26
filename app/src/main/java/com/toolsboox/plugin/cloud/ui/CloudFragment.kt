package com.toolsboox.plugin.cloud.ui

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.analytics.FirebaseAnalytics
import com.squareup.moshi.Moshi
import com.toolsboox.R
import com.toolsboox.da.Credential
import com.toolsboox.databinding.FragmentCloudBinding
import com.toolsboox.ot.CryptoUtils
import com.toolsboox.plugin.cloud.da.Purchase
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.internal.immutableListOf
import timber.log.Timber
import java.util.*
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

    /**
     * The purchases updated listener.
     */
    private lateinit var purchasesUpdatedListener: PurchasesUpdatedListener

    /**
     * The product details params.
     */
    private lateinit var queryProductDetailsParams: QueryProductDetailsParams

    /**
     * The product details.
     */
    private lateinit var productDetails: ProductDetails

    /**
     * The billing client.
     */
    private lateinit var billingClient: BillingClient

    /**
     * Flag of billing client finished.
     */
    private var billingClientFinished = false

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCloudBinding.bind(view)

        purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                Timber.i("PurchasesUpdatedListener: $purchases")
            } else {
                Timber.w("PurchasesUpdatedListener: $billingResult")
            }
        }

        billingClient = BillingClient.newBuilder(requireContext())
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        queryProductDetailsParams = QueryProductDetailsParams.newBuilder().setProductList(
            immutableListOf(
                Product.newBuilder().setProductId("cloud_v1").setProductType(ProductType.SUBS).build()
            )
        ).build()

        binding.accountSignUpButton.setOnClickListener {
            signUpDialog()
        }

        binding.accountLogInButton.setOnClickListener {
            val refreshToken = sharedPreferences.getString("refreshToken", null)
            if (refreshToken == null) {
                logInDialog()
            } else {
                logOutDialog()
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
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.cloud_title))

        // Update state of the buttons.
        updateButtons()

        // Start the billing client async connection.
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Timber.i("onBillingSetupFinished: OK")
                    billingClient.queryProductDetailsAsync(queryProductDetailsParams) { queryResult, productDetailsList ->
                        if (queryResult.responseCode == BillingResponseCode.OK) {
                            Timber.i("queryProductDetailsAsync: $productDetailsList")
                            if (productDetailsList.isNotEmpty()) {
                                productDetails = productDetailsList[0]
                                Timber.i("ProductDetails: $productDetails")
                                requireActivity().runOnUiThread {
                                    productDetails.subscriptionOfferDetails?.forEach { offer ->
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
                            billingClientFinished = true
                        } else {
                            Timber.w("queryProductDetailsAsync: $queryResult")
                            billingClientFinished = false
                        }

                        requireActivity().runOnUiThread { updateButtons() }
                    }

                    billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build())
                    { purchasesResult, purchaseList ->
                        if (purchasesResult.responseCode == BillingResponseCode.OK) {
                            Timber.i("queryPurchasesAsync: $purchaseList")
                            val status: String
                            if (purchaseList.isNotEmpty()) {
                                status = getString(R.string.cloud_subscription_status_subs)
                                val purchase = purchaseList[0]
                                Timber.i("Purchase: $purchase")
                                val purchaseToken = purchase.purchaseToken
                                val productId = purchase.products[0]
                                Timber.i("ProductId: $productId, purchaseToken: $purchaseToken")

                                moshi.adapter(Purchase::class.java).fromJson(purchase.originalJson)?.let {
                                    presenter.updatePurchase(this@CloudFragment, UUID.fromString("a01e8f50-9654-11ed-a7cc-dd953a7e666b"), it)
                                }
                            } else {
                                status = getString(R.string.cloud_subscription_status_no_subs)
                            }
                            binding.cloudSubscriptionStatusMessage.text = getString(R.string.cloud_subscription_status_message).format(status)
                        } else {
                            Timber.w("queryPurchasesAsync: $purchasesResult")
                        }
                    }
                } else {
                    Timber.w("onBillingSetupFinished: $billingResult")
                }

                requireActivity().runOnUiThread { updateButtons() }
            }

            override fun onBillingServiceDisconnected() {
                Timber.i("onBillingServiceDisconnected")
            }
        })
    }

    /**
     * Create credential response.
     *
     * @param result the created credential result.
     */
    fun createCredentialResult(result: Credential) {
        Timber.i("Create credential result: $result")

        sharedPreferences.edit().putString("userId", result.userId.toString()).apply()
        sharedPreferences.edit().putString("userIdKey", CryptoUtils.getKey(result.userId)).apply()
        updateButtons()
    }

    /**
     * Login credential failed.
     *
     * @param result the login result.
     */
    fun loginCredentialFailed(result: String?) {
        Timber.i("Create credential result: $result")

        val failedDialog = AlertDialog.Builder(requireContext())
        failedDialog.setTitle(R.string.cloud_account_log_in_failed_dialog_title)
        failedDialog.setMessage(R.string.cloud_account_log_in_failed_dialog_message)
        failedDialog.setNeutralButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
        failedDialog.show()
    }

    /**
     * Login credential response.
     *
     * @param userId the user ID
     * @param result the login result.
     */
    fun loginCredentialResult(userId: UUID, result: String) {
        Timber.i("Create credential result: $result")

        sharedPreferences.edit().putString("userId", userId.toString()).apply()
        sharedPreferences.edit().putString("userIdKey", CryptoUtils.getKey(userId)).apply()
        sharedPreferences.edit().putString("refreshToken", result).apply()

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
        if (userIdKey == null) {
            binding.accountLoginUserIdMessage.text = getString(R.string.cloud_account_login_user_id_message).format(notLoggedIn)
            binding.accountLoginStatusMessage.text = getString(R.string.cloud_account_login_status_message).format(notLoggedIn)
            binding.accountSignUpButton.isEnabled = true
            binding.accountSignUpButton.alpha = 1.0f
        } else {
            binding.accountLoginUserIdMessage.text = getString(R.string.cloud_account_login_user_id_message).format(userIdKey)
            binding.accountLoginStatusMessage.text = getString(R.string.cloud_account_login_status_message).format(notLoggedIn)
            binding.accountSignUpButton.isEnabled = false
            binding.accountSignUpButton.alpha = 0.5f
        }

        val refreshToken = sharedPreferences.getString("refreshToken", null)
        if (refreshToken == null) {
            binding.accountLoginStatusMessage.text = getString(R.string.cloud_account_login_status_message).format(notLoggedIn)
            binding.accountLogInButton.text = getString(R.string.cloud_account_log_in_button)
        } else {
            binding.accountLoginStatusMessage.text = getString(R.string.cloud_account_login_status_message).format(loggedIn)
            binding.accountLogInButton.text = getString(R.string.cloud_account_log_out_button)
        }

        if (!billingClientFinished) {
            binding.cloudMonthlyButton.isEnabled = false
            binding.cloudMonthlyButton.alpha = 0.5f
            binding.cloudMonthlyButton.text = getString(R.string.cloud_subscription_monthly_button).format(loading)

            binding.cloudYearlyButton.isEnabled = false
            binding.cloudYearlyButton.alpha = 0.5f
            binding.cloudYearlyButton.text = getString(R.string.cloud_subscription_yearly_button).format(loading)

            binding.cloudSubscriptionStatusMessage.text = getString(R.string.cloud_subscription_status_message).format(loading)
        } else {
            if (refreshToken == null) {
                binding.cloudMonthlyButton.isEnabled = false
                binding.cloudMonthlyButton.alpha = 0.5f

                binding.cloudYearlyButton.isEnabled = false
                binding.cloudYearlyButton.alpha = 0.5f
            } else {
                binding.cloudMonthlyButton.isEnabled = true
                binding.cloudMonthlyButton.alpha = 1.0f

                binding.cloudYearlyButton.isEnabled = true
                binding.cloudYearlyButton.alpha = 1.0f
            }
        }
    }

    /**
     * Start the subscription flow.
     *
     * @param basePlan the name of the base plan
     */
    private fun subscriptionFlow(basePlan: String) {
        Timber.i("Checking '%s' offer of cloud_v1 product...", basePlan)
        val offers = productDetails.subscriptionOfferDetails ?: return
        val offer = offers.firstOrNull { offer -> offer.basePlanId == basePlan } ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
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

    /**
     * Displays the sign-up dialog.
     */
    private fun signUpDialog() {
        val signUpDialog = AlertDialog.Builder(requireContext())

        val passwordView = requireActivity().layoutInflater.inflate(R.layout.fragment_cloud_sign_in_view, null)
        val passwordEditText = passwordView.findViewById<TextInputEditText>(R.id.password_edit_text)

        signUpDialog.setTitle(R.string.cloud_account_sign_up_dialog_title)
        signUpDialog.setView(passwordView)

        signUpDialog.setPositiveButton(R.string.cloud_account_sign_up_button) { dialog, _ ->
            val password = passwordEditText.text.toString()

            val hash = CryptoUtils.md5Hash(password.toByteArray(Charsets.UTF_8))
            presenter.createCredential(this@CloudFragment, hash)

            dialog.dismiss()
            updateButtons()
        }

        signUpDialog.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }

        val dialog = signUpDialog.show()
        val signUpButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        signUpButton.isEnabled = false

        passwordEditText.addTextChangedListener {
            val password = passwordEditText.text.toString()
            signUpButton.isEnabled = password.length >= 8
        }
    }

    /**
     * Displays the log in dialog.
     */
    private fun logInDialog() {
        val logInDialog = AlertDialog.Builder(requireContext())

        val logInView = requireActivity().layoutInflater.inflate(R.layout.fragment_cloud_log_in_view, null)
        val userIdKeyEditText = logInView.findViewById<TextInputEditText>(R.id.user_id_key_edit_text)
        val passwordEditText = logInView.findViewById<TextInputEditText>(R.id.password_edit_text)

        sharedPreferences.getString("userIdKey", null)?.let { userIdKeyEditText.setText(it) }

        logInDialog.setTitle(R.string.cloud_account_log_in_dialog_title)
        logInDialog.setView(logInView)

        logInDialog.setPositiveButton(R.string.cloud_account_log_in_button) { dialog, _ ->
            val userIdKey = userIdKeyEditText.text.toString()
            val password = passwordEditText.text.toString()

            val userId = CryptoUtils.getUUID(userIdKey)
            val hash = CryptoUtils.md5Hash(password.toByteArray(Charsets.UTF_8))
            presenter.loginCredential(this@CloudFragment, userId!!, hash)

            dialog.dismiss()
            updateButtons()
        }

        logInDialog.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }

        val dialog = logInDialog.show()
        val logInButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        logInButton.isEnabled = false

        userIdKeyEditText.addTextChangedListener {
            val userIdKey = userIdKeyEditText.text.toString()
            val userId = CryptoUtils.getUUID(userIdKey)
            val password = passwordEditText.text.toString()

            logInButton.isEnabled = (password.length >= 8) and (userId != null)
        }
        passwordEditText.addTextChangedListener {
            val userIdKey = userIdKeyEditText.text.toString()
            val userId = CryptoUtils.getUUID(userIdKey)
            val password = passwordEditText.text.toString()

            logInButton.isEnabled = (password.length >= 8) and (userId != null)
        }
    }

    /**
     * Displays the log-out dialog.
     */
    private fun logOutDialog() {
        val logOutDialog = AlertDialog.Builder(requireContext())

        logOutDialog.setTitle(R.string.cloud_account_log_out_dialog_title)
        logOutDialog.setMessage(R.string.cloud_account_log_out_dialog_message)

        logOutDialog.setPositiveButton(R.string.cloud_account_log_out_button) { dialog, _ ->
            sharedPreferences.edit().remove("userId").apply()
            sharedPreferences.edit().remove("userIdKey").apply()
            sharedPreferences.edit().remove("refreshToken").apply()
            sharedPreferences.edit().remove("accessToken").apply()

            dialog.dismiss()

            updateButtons()
        }

        logOutDialog.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }

        logOutDialog.show()
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

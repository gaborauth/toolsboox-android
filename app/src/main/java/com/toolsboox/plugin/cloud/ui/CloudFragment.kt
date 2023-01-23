package com.toolsboox.plugin.cloud.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.view.View
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.google.firebase.analytics.FirebaseAnalytics
import com.squareup.moshi.Moshi
import com.toolsboox.R
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
                //[Purchase. Json: {"orderId":"GPA.3324-7264-6091-73699","packageName":"com.toolsboox","productId":"cloud_v1","purchaseTime":1674239658527,"purchaseState":0,"purchaseToken":"ckkhmodnlblplnpbinnebhod.AO-J1OzDkvfLPTaQeINPPQUuVIAxs_GN17Gq6y6EailfVOTPX0N1hm9-YBwkACbukFXo0StrvuB1phlcPobgw9gulhEUEVegZw","quantity":1,"autoRenewing":true,"acknowledged":false}]
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

        binding.cloudIntegrationMonthlyButton.setOnClickListener {
            Timber.i("Checking monthly offer of cloud_v1 product...")
            val offers = productDetails.subscriptionOfferDetails ?: return@setOnClickListener
            val offer = offers.firstOrNull { offer -> offer.basePlanId == "monthly" } ?: return@setOnClickListener

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

        binding.cloudIntegrationYearlyButton.setOnClickListener {
            Timber.i("Checking yearly offer of cloud_v1 product...")
            val offers = productDetails.subscriptionOfferDetails ?: return@setOnClickListener
            val offer = offers.firstOrNull { offer -> offer.basePlanId == "yearly" } ?: return@setOnClickListener

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

        // Test of crypto utility compatibility.
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

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.cloud_title))

        val loading = getString(R.string.cloud_loading)
        binding.cloudIntegrationMonthlyButton.isEnabled = false
        binding.cloudIntegrationMonthlyButton.text = getString(R.string.cloud_monthly_button).format(loading)
        binding.cloudIntegrationYearlyButton.isEnabled = false
        binding.cloudIntegrationYearlyButton.text = getString(R.string.cloud_yearly_button).format(loading)
        binding.cloudIntegrationSubscriptionStatus.text = getString(R.string.cloud_subscription_status).format(loading)

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
                                            val buttonText = getString(R.string.cloud_monthly_button).format(price)
                                            binding.cloudIntegrationMonthlyButton.isEnabled = true
                                            binding.cloudIntegrationMonthlyButton.text = buttonText

                                        }
                                        if (offer.basePlanId == "yearly") {
                                            val buttonText = getString(R.string.cloud_yearly_button).format(price)
                                            binding.cloudIntegrationYearlyButton.isEnabled = true
                                            binding.cloudIntegrationYearlyButton.text = buttonText
                                        }
                                    }
                                }
                            }
                        } else {
                            Timber.w("queryProductDetailsAsync: $queryResult")
                        }
                    }

                    billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build())
                    { purchasesResult, purchaseList ->
                        if (purchasesResult.responseCode == BillingResponseCode.OK) {
                            Timber.i("queryPurchasesAsync: $purchaseList")
                            var status = ""
                            if (purchaseList.isNotEmpty()) {
                                status = getString(R.string.cloud_subscription_status_subs)
                                val purchase = purchaseList[0]
                                Timber.i("Purchase: $purchase")
                                val purchaseToken = purchase.purchaseToken
                                val productId = purchase.products[0]
                                Timber.i("ProductId: $productId, purchaseToken: $purchaseToken")

                                moshi.adapter(Purchase::class.java).fromJson(purchase.originalJson)?.let {
                                    presenter.update(this@CloudFragment, UUID.fromString("a01e8f50-9654-11ed-a7cc-dd953a7e666b"), it)
                                }
                            } else {
                                status = getString(R.string.cloud_subscription_status_no_subs)
                            }
                            val html = getString(R.string.cloud_subscription_status).format(status)
                            binding.cloudIntegrationSubscriptionStatus.text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
                        } else {
                            Timber.w("queryPurchasesAsync: $purchasesResult")
                        }
                    }
                } else {
                    Timber.w("onBillingSetupFinished: $billingResult")
                }
            }

            override fun onBillingServiceDisconnected() {
                Timber.i("onBillingServiceDisconnected")
            }
        })
    }

    /**
     * Update purchase response.
     *
     * @param result the purchase result.
     */
    fun updateResult(result: Purchase) {
        Timber.i("Update result: $result")
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

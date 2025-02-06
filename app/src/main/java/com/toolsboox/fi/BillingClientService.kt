package com.toolsboox.fi

import android.app.Activity
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.QueryProductDetailsParams.Product
import okhttp3.internal.immutableListOf
import timber.log.Timber
import javax.inject.Inject

/**
 * Billing client services.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class BillingClientService @Inject constructor() {
    companion object {

        // The billing client.
        private var billingClient: BillingClient? = null

        /**
         * Connect the billing client.
         *
         * @param activity the activity
         * @param onSuccess the success callback
         * @param onFailed the failed callback
         */
        fun connectClient(activity: Activity, onSuccess: (BillingClient) -> Unit, onFailed: (BillingResult) -> Unit) {
            // If the billing client is already connected, return with it.
            if (billingClient?.isReady == true) {
                onSuccess(billingClient!!)
            }

            // Create the listener for the purchases.
            val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Timber.i("PurchasesUpdatedListener: $purchases")
                } else {
                    Timber.w("PurchasesUpdatedListener: $billingResult")
                }
            }

            // Create the billing client.
            billingClient = BillingClient.newBuilder(activity)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build()

            // Start the billing client async connection.
            billingClient!!.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        onSuccess(billingClient!!)
                    } else {
                        onFailed(billingResult)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Timber.i("onBillingServiceDisconnected")
                }
            })
        }

        /**
         * Query the details of the subscription.
         *
         * @param billingClient the billing client
         * @param subscriptionId the subscription ID
         * @param onSuccess the success callback
         * @param onFailed the failed callback
         */
        fun subscriptionDetails(
            billingClient: BillingClient, subscriptionId: String,
            onSuccess: (List<ProductDetails>) -> Unit, onFailed: (BillingResult) -> Unit
        ) {
            // Create the query product details params.
            val queryProductDetailsParams = QueryProductDetailsParams.newBuilder().setProductList(
                immutableListOf(
                    Product.newBuilder().setProductId(subscriptionId).setProductType(ProductType.SUBS).build()
                )
            ).build()

            // Query the subscription details.
            billingClient.queryProductDetailsAsync(queryProductDetailsParams) { queryResult, productDetailsList ->
                if (queryResult.responseCode == BillingResponseCode.OK) {
                    onSuccess(productDetailsList)
                } else {
                    onFailed(queryResult)
                }
            }
        }

        /**
         * Query the subscriptions as purchases.
         *
         * @param billingClient the billing client
         * @param onSuccess the success callback
         * @param onFailed the failed callback
         */
        fun subscriptions(billingClient: BillingClient, onSuccess: (List<Purchase>) -> Unit, onFailed: (BillingResult) -> Unit) {
            billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build())
            { purchasesResult, purchaseList ->
                if (purchasesResult.responseCode == BillingResponseCode.OK) {
                    onSuccess(purchaseList)
                } else {
                    onFailed(purchasesResult)
                }
            }
        }
    }
}

package com.toolsboox.plugin.cloud.ui

import com.toolsboox.nw.CredentialService
import com.toolsboox.plugin.cloud.da.Purchase
import com.toolsboox.plugin.cloud.nw.PurchaseService
import com.toolsboox.ui.plugin.FragmentPresenter
import java.util.*
import javax.inject.Inject

/**
 * Cloud presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CloudPresenter @Inject constructor() : FragmentPresenter() {
    /**
     * The credential service.
     */
    @Inject
    lateinit var credentialService: CredentialService

    /**
     * The purchase service.
     */
    @Inject
    lateinit var purchaseService: PurchaseService

    /**
     * Create an account.
     *
     * @param fragment the fragment
     * @param hash the MD5 hash of the password
     * @return the created credential
     */
    fun createCredential(fragment: CloudFragment, hash: String) {
        coroutinesCallHelper(
            fragment,
            { credentialService.createAsync(hash) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(true)
                        } else {
                            fragment.createCredentialResult(body)
                        }
                    }

                    else -> fragment.somethingHappened(true)
                }
            },
            true
        )
    }

    /**
     * Log in.
     *
     * @param fragment the fragment
     * @param userId the user ID
     * @param hash the MD5 hash of the password
     * @return the created credential
     */
    fun loginCredential(fragment: CloudFragment, userId: UUID, hash: String) {
        coroutinesCallHelper(
            fragment,
            { credentialService.loginAsync(userId, hash) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(true)
                        } else {
                            fragment.loginCredentialResult(userId, body)
                        }
                    }

                    403 -> {
                        fragment.loginCredentialFailed(response.body())
                    }

                    else -> fragment.somethingHappened()
                }
            },
            true
        )
    }

    /**
     * Update the purchase.
     *
     * @param fragment the fragment
     * @param userId the user ID
     * @param purchase the purchase
     * @return the saved purchase
     */
    fun updatePurchase(fragment: CloudFragment, userId: UUID, purchase: Purchase) {
        coroutinesCallHelper(
            fragment,
            { purchaseService.updateAsync(userId, purchase) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(true)
                        } else {
                            fragment.updatePurchaseResult(body)
                        }
                    }

                    else -> fragment.somethingHappened(true)
                }
            },
            true
        )
    }
}

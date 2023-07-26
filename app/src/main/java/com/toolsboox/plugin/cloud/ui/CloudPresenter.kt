package com.toolsboox.plugin.cloud.ui

import android.content.DialogInterface
import android.view.View
import com.toolsboox.nw.CredentialService
import com.toolsboox.plugin.cloud.da.Purchase
import com.toolsboox.plugin.cloud.nw.PurchaseService
import com.toolsboox.ui.plugin.FragmentPresenter
import timber.log.Timber
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
     * @param signUpDialog the sign-up dialog
     * @param signUpView the sign-up view
     * @param username the username
     * @param hash the MD5 hash of the password
     * @return the created credential
     */
    fun createCredential(fragment: CloudFragment, signUpDialog: DialogInterface, signUpView: View, username: String, hash: String) {
        coroutinesCallHelper(
            fragment,
            { credentialService.createAsync(username, hash) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        Timber.i("Body: $body")
                        if (body == null) {
                            fragment.somethingHappened(false)
                            signUpDialog.dismiss()
                        } else {
                            fragment.createCredentialResult(signUpDialog, signUpView, body)
                        }
                    }

                    409 -> {
                        fragment.createCredentialConflict(signUpView)
                    }

                    else -> {
                        fragment.somethingHappened(false)
                        signUpDialog.dismiss()
                    }
                }
            },
            true
        )
    }

    /**
     * Log in.
     *
     * @param fragment the fragment
     * @param loginDialog the login dialog
     * @param loginView the login view
     * @param username the username
     * @param hash the MD5 hash of the password
     * @return the created credential
     */
    fun loginCredential(fragment: CloudFragment, loginDialog: DialogInterface, loginView: View, username: String, hash: String) {
        coroutinesCallHelper(
            fragment,
            { credentialService.loginAsync(username, hash) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(false)
                        } else {
                            fragment.loginCredentialResult(loginDialog, loginView, body)
                        }
                    }

                    403 -> {
                        fragment.loginCredentialFailed(loginView, response.body())
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

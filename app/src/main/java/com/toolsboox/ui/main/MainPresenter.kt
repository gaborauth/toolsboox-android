package com.toolsboox.ui.main

import com.toolsboox.nw.CredentialService
import com.toolsboox.ui.BasePresenter

/**
 * Presenter of main.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class MainPresenter(mainView: MainView, credentialService: CredentialService) : BasePresenter<MainView>(mainView, credentialService) {

    /**
     * Refresh the access token.
     *
     * @param activity the activity
     * @param refreshToken refresh token
     * @return the new access token
     */
    fun accessToken(activity: MainActivity, refreshToken: String) {
        coroutinesCallHelper(
            { credentialService.accessTokenAsync("Bearer $refreshToken") },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            activity.somethingHappened()
                        } else {
                            activity.accessTokenResult(body)
                        }
                    }

                    else -> activity.somethingHappened()
                }
            }
        )
    }

    /**
     * Refresh the refresh token.
     *
     * @param activity the activity
     * @param refreshToken refresh token
     * @return the new refresh token
     */
    fun refreshToken(activity: MainActivity, refreshToken: String) {
        coroutinesCallHelper(
            { credentialService.refreshTokenAsync("Bearer $refreshToken") },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            activity.somethingHappened()
                        } else {
                            activity.refreshTokenResult(body)
                        }
                    }

                    else -> activity.somethingHappened()
                }
            }
        )
    }
}

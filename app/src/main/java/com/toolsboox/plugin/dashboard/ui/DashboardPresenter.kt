package com.toolsboox.plugin.dashboard.ui

import com.toolsboox.BuildConfig
import com.toolsboox.nw.CredentialService
import com.toolsboox.plugin.dashboard.nw.DashboardService
import com.toolsboox.ui.plugin.FragmentPresenter
import javax.inject.Inject

/**
 * Dashboard presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class DashboardPresenter @Inject constructor() : FragmentPresenter() {

    /**
     * The credential service.
     */
    @Inject
    lateinit var credentialService: CredentialService
    /**
     * The dashboard service.
     */
    @Inject
    lateinit var dashboardService: DashboardService

    /**
     * Refresh the acess token.
     *
     * @param fragment the fragment
     * @param refreshToken refresh token
     * @return the new access token
     */
    fun accessTokenCredential(fragment: DashboardFragment, refreshToken: String) {
        coroutinesCallHelper(
            fragment,
            { credentialService.accessTokenAsync("Bearer $refreshToken") },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(true)
                        } else {
                            fragment.accessTokenCredentialResult(body)
                        }
                    }

                    else -> fragment.somethingHappened(true)
                }
            },
            true
        )
    }

    /**
     * Refresh the refresh token.
     *
     * @param fragment the fragment
     * @param refreshToken refresh token
     * @return the new refresh token
     */
    fun refreshTokenCredential(fragment: DashboardFragment, refreshToken: String) {
        coroutinesCallHelper(
            fragment,
            { credentialService.refreshTokenAsync("Bearer $refreshToken") },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(true)
                        } else {
                            fragment.refreshTokenCredentialResult(body)
                        }
                    }

                    else -> fragment.somethingHappened(true)
                }
            },
            true
        )
    }

    /**
     * Get a value of the parameter by key.
     *
     * @param fragment the fragment
     * @param key the key
     * @return the value of the parameter
     */
    fun parameter(fragment: DashboardFragment, key: String) {
        coroutinesCallHelper(
            fragment,
            { dashboardService.parameterAsync(key) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(true)
                        } else {
                            fragment.parameterResult(key, body)
                        }
                    }

                    else -> fragment.somethingHappened(true)
                }
            },
            true
        )
    }

    /**
     * Get the server API version.
     *
     * @param fragment the fragment
     * @return the server API version
     */
    fun version(fragment: DashboardFragment) {
        coroutinesCallHelper(
            fragment,
            { dashboardService.versionAsync("${BuildConfig.VERSION_CODE}") },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(true)
                        } else {
                            fragment.versionResult(body)
                        }
                    }

                    else -> fragment.somethingHappened(true)
                }
            },
            true
        )
    }
}

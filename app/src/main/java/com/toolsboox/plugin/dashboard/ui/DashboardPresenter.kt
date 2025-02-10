package com.toolsboox.plugin.dashboard.ui

import com.toolsboox.BuildConfig
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
     * The dashboard service.
     */
    @Inject
    lateinit var dashboardService: DashboardService

    /**
     * Get a value of the parameter by key.
     *
     * @param fragment the fragment
     * @return the value of the parameters
     */
    fun parameters(fragment: DashboardFragment) {
        coroutinesCallHelper(
            fragment,
            { dashboardService.parametersAsync() },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(true)
                        } else {
                            fragment.parameterResult(body)
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

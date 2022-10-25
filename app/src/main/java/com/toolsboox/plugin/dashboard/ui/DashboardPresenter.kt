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

    @Inject
    lateinit var dashboardService: DashboardService

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
                            fragment.somethingHappened()
                        } else {
                            fragment.versionResult(body)
                        }
                    }

                    else -> fragment.somethingHappened()
                }
            }
        )
    }
}

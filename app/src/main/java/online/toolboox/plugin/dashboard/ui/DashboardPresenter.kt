package online.toolboox.plugin.dashboard.ui

import online.toolboox.plugin.dashboard.nw.DashboardService
import online.toolboox.ui.plugin.FragmentPresenter
import javax.inject.Inject

/**
 * Dashboard presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class DashboardPresenter @Inject constructor(
    private val dashboardService: DashboardService
) : FragmentPresenter() {
    /**
     * Get the server API version.
     *
     * @param fragment the fragment
     * @return the server API version
     */
    fun version(fragment: DashboardFragment) {
        coroutinesCallHelper(
            fragment,
            { dashboardService.versionAsync() },
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

package online.toolboox.plugin.dashboard.ui

import online.toolboox.plugin.dashboard.nw.DashboardService
import online.toolboox.ui.plugin.FragmentPresenter
import javax.inject.Inject

/**
 * Dashboard presenter.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class DashboardPresenter @Inject constructor(
    private val dashboardService: DashboardService
) : FragmentPresenter()

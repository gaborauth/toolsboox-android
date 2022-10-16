package com.toolsboox.plugin.dashboard

import com.toolsboox.plugin.dashboard.ui.DashboardFragment
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment
import javax.inject.Inject

/**
 * Dashboard plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class DashboardPlugin @Inject constructor() : Plugin {

    override fun getRoute(url: String): ScreenFragment? {
        Router.getParameters("/", url).let {
            if (it is Router.Parameters.Match) return DashboardFragment().setParameters(it.parameters)
        }

        return null
    }
}

package com.toolsboox.plugin.dashboard

import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.dashboard.di.DashboardServiceModule
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment

/**
 * Dashboard plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class DashboardPlugin(private val router: Router) : Plugin {

    private val component = DaggerDashboardComponent.builder()
        .dashboardServiceModule(DashboardServiceModule)
        .dashboardModule(DashboardModule(this, router))
        .networkModule(NetworkModule)
        .build()

    override fun getRoute(url: String): ScreenFragment? {
        router.getParameters("/", url).let {
            if (it is Router.Parameters.Match) return component.fragment().setParameters(it.parameters)
        }

        return null
    }
}

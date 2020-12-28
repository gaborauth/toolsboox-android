package online.toolboox.plugin.dashboard

import online.toolboox.di.NetworkModule
import online.toolboox.plugin.dashboard.di.DashboardServiceModule
import online.toolboox.ui.plugin.Plugin
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment

/**
 * Dashboard plugin.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
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

package online.toolboox.plugin.teamdrawer

import online.toolboox.di.NetworkModule
import online.toolboox.plugin.teamdrawer.di.TeamDrawerServiceModule
import online.toolboox.ui.plugin.Plugin
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment

/**
 * Team drawer plugin.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class TeamDrawerPlugin(private val router: Router) : Plugin {

    private val component = DaggerTeamDrawerComponent.builder()
        .teamDrawerServiceModule(TeamDrawerServiceModule)
        .networkModule(NetworkModule)
        .build()

    override fun getRoute(url: String): ScreenFragment? {
        router.getParameters("/teamDrawer/(?<pageId>.*)", url).let {
            if (it is Router.Parameters.Match) return component.fragment().setParameters(it.parameters)
        }

        return null
    }
}

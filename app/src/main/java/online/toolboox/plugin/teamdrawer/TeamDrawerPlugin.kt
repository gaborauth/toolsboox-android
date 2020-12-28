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
        .teamDrawerModule(TeamDrawerModule(this, router))
        .networkModule(NetworkModule)
        .build()

    override fun getRoute(url: String): ScreenFragment? {
        router.getParameters("/teamDrawer/(?<roomId>.*)/(?<noteId>.*)/(?<pageId>.*)", url).let {
            if (it is Router.Parameters.Match) return component.pageFragment().setParameters(it.parameters)
        }
        router.getParameters("/teamDrawer/(?<roomId>.*)", url).let {
            if (it is Router.Parameters.Match) return component.noteFragment().setParameters(it.parameters)
        }
        router.getParameters("/teamDrawer", url).let {
            if (it is Router.Parameters.Match) return component.roomFragment().setParameters(it.parameters)
        }

        return null
    }
}

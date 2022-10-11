package com.toolsboox.plugin.teamdrawer

import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.teamdrawer.di.TeamDrawerServiceModule
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment

/**
 * Team drawer plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
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

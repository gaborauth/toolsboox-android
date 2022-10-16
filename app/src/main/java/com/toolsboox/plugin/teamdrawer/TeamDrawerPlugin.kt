package com.toolsboox.plugin.teamdrawer

import com.toolsboox.plugin.teamdrawer.ui.NoteFragment
import com.toolsboox.plugin.teamdrawer.ui.PageFragment
import com.toolsboox.plugin.teamdrawer.ui.RoomFragment
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment
import javax.inject.Inject

/**
 * Team drawer plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class TeamDrawerPlugin @Inject constructor() : Plugin {

    override fun getRoute(url: String): ScreenFragment? {
        Router.getParameters("/teamDrawer/(?<roomId>.*)/(?<noteId>.*)/(?<pageId>.*)", url).let {
            if (it is Router.Parameters.Match) return PageFragment().setParameters(it.parameters)
        }
        Router.getParameters("/teamDrawer/(?<roomId>.*)", url).let {
            if (it is Router.Parameters.Match) return NoteFragment().setParameters(it.parameters)
        }
        Router.getParameters("/teamDrawer", url).let {
            if (it is Router.Parameters.Match) return RoomFragment().setParameters(it.parameters)
        }

        return null
    }
}

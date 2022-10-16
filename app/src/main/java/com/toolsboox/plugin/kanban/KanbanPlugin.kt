package com.toolsboox.plugin.kanban

import com.toolsboox.plugin.kanban.ui.KanbanMainFragment
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment
import javax.inject.Inject

/**
 * Kanban plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class KanbanPlugin @Inject constructor() : Plugin {

    override fun getRoute(url: String): ScreenFragment? {
        Router.getParameters("/kanbanPlanner", url).let {
            if (it is Router.Parameters.Match) return KanbanMainFragment().setParameters(it.parameters)
        }

        return null
    }
}

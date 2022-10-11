package com.toolsboox.plugin.kanban

import com.toolsboox.di.NetworkModule
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment

/**
 * Kanban plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class KanbanPlugin(private val router: Router) : Plugin {

    private val component = DaggerKanbanComponent.builder()
        .kanbanModule(KanbanModule(this, router))
        .networkModule(NetworkModule)
        .build()

    override fun getRoute(url: String): ScreenFragment? {
        router.getParameters("/kanbanPlanner", url).let {
            if (it is Router.Parameters.Match) return component.mainFragment().setParameters(it.parameters)
        }

        return null
    }
}

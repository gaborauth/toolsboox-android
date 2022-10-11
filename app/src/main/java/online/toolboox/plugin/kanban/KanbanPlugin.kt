package online.toolboox.plugin.kanban

import online.toolboox.di.NetworkModule
import online.toolboox.ui.plugin.Plugin
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment

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

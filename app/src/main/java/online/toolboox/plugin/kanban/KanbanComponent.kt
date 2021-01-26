package online.toolboox.plugin.kanban

import dagger.Component
import online.toolboox.di.NetworkModule
import online.toolboox.plugin.kanban.ui.MainFragment
import javax.inject.Singleton

/**
 * Kanban planner plugin component.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
@Singleton
@Component(
    modules = [
        NetworkModule::class,

        KanbanModule::class,
    ]
)
interface KanbanComponent {
    fun inject(plugin: KanbanPlugin)

    fun mainFragment(): MainFragment
}

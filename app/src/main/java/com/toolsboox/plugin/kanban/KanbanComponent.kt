package com.toolsboox.plugin.kanban

import dagger.Component
import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.kanban.ui.MainFragment
import javax.inject.Singleton

/**
 * Kanban planner plugin component.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
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

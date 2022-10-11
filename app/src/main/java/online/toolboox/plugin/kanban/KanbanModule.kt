package online.toolboox.plugin.kanban

import dagger.Module
import dagger.Provides
import online.toolboox.ui.plugin.Router
import javax.inject.Singleton

/**
 * Kanban plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
class KanbanModule(private val plugin: KanbanPlugin, private val router: Router) {
    @Provides
    @Singleton
    fun providePlugin() = plugin

    @Provides
    @Singleton
    fun provideRouter(): Router = router
}

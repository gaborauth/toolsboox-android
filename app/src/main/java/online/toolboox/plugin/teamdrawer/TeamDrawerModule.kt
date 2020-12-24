package online.toolboox.plugin.teamdrawer

import dagger.Module
import dagger.Provides
import online.toolboox.ui.plugin.Router
import javax.inject.Singleton

/**
 * Team drawer plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
@Module
class TeamDrawerModule(private val plugin: TeamDrawerPlugin, private val router: Router) {
    @Provides
    @Singleton
    fun providePlugin() = plugin

    @Provides
    @Singleton
    fun provideRouter(): Router = router
}

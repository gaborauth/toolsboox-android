package com.toolsboox.plugin.teamdrawer

import dagger.Module
import dagger.Provides
import com.toolsboox.ui.plugin.Router
import javax.inject.Singleton

/**
 * Team drawer plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
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

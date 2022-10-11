package com.toolsboox.plugin.dashboard

import dagger.Module
import dagger.Provides
import com.toolsboox.ui.plugin.Router
import javax.inject.Singleton

/**
 * Dashboard plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
class DashboardModule(private val plugin: DashboardPlugin, private val router: Router) {
    @Provides
    @Singleton
    fun providePlugin() = plugin

    @Provides
    @Singleton
    fun provideRouter(): Router = router
}

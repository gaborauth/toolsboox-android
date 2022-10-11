package com.toolsboox.plugin.dashboard

import dagger.Component
import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.dashboard.di.DashboardServiceModule
import com.toolsboox.plugin.dashboard.ui.DashboardFragment
import javax.inject.Singleton

/**
 * Dashboard plugin component.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Singleton
@Component(
    modules = [
        DashboardModule::class,
        DashboardServiceModule::class,
        NetworkModule::class
    ]
)
interface DashboardComponent {
    fun inject(plugin: DashboardPlugin)
    fun fragment(): DashboardFragment
}

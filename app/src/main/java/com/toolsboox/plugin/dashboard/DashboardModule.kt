package com.toolsboox.plugin.dashboard

import com.toolsboox.plugin.dashboard.ui.DashboardPresenter
import com.toolsboox.ui.plugin.FragmentPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * Dashboard plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class DashboardModule {

    @Binds
    abstract fun bindDashboardPresenter(dashboardPresenter: DashboardPresenter): FragmentPresenter
}

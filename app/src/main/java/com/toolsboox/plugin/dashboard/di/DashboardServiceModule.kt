package com.toolsboox.plugin.dashboard.di

import com.toolsboox.plugin.dashboard.nw.DashboardService
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import retrofit2.Retrofit

/**
 * Dashboard service module, provides services.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
object DashboardServiceModule {
    /**
     * Provides the dashboard service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun provideDashboardService(retrofit: Retrofit): DashboardService {
        return retrofit.create(DashboardService::class.java)
    }
}

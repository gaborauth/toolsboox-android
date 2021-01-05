package online.toolboox.plugin.dashboard.di

import dagger.Module
import dagger.Provides
import dagger.Reusable
import online.toolboox.plugin.dashboard.nw.DashboardService
import retrofit2.Retrofit

/**
 * Dashboard service module, provides services.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
@Module
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
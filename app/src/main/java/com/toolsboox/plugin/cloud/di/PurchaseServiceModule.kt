package com.toolsboox.plugin.cloud.di

import com.toolsboox.plugin.cloud.nw.PurchaseService
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import retrofit2.Retrofit

/**
 * Purchase service module, provides services.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
object PurchaseServiceModule {
    /**
     * Provides the purchase service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun providePurchaseService(retrofit: Retrofit): PurchaseService {
        return retrofit.create(PurchaseService::class.java)
    }
}

package com.toolsboox.plugin.templates.di

import com.toolsboox.plugin.templates.nw.CommunityTemplatesRepository
import com.toolsboox.plugin.templates.nw.TemplatesService
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named

/**
 * Templates service module, provides services.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object TemplatesServiceModule {
    /**
     * Provides the templates service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun provideTemplatesService(@Named("gitHubRaw") retrofit: Retrofit): TemplatesService {
        return retrofit.create(TemplatesService::class.java)
    }

    /**
     * Provides the repository.
     *
     * @return the repository
     */
    @Provides
    @Reusable
    fun provideRepository(): CommunityTemplatesRepository {
        return CommunityTemplatesRepository()
    }
}

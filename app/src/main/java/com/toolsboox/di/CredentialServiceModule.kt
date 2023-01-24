package com.toolsboox.di

import com.toolsboox.nw.CredentialService
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import retrofit2.Retrofit

/**
 * Credential service module, provides services.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
object CredentialServiceModule {
    /**
     * Provides the credential service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun provideCredentialService(retrofit: Retrofit): CredentialService {
        return retrofit.create(CredentialService::class.java)
    }
}

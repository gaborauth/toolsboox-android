package online.toolboox.main.di

import dagger.Module
import dagger.Provides
import dagger.Reusable
import online.toolboox.main.nw.MainService
import retrofit2.Retrofit
import javax.inject.Named

/**
 * Main service module, provides main service.
 *
 * @author <a href="mailto:auth.gabor@gmail.com">Gábor AUTH</a>
 */
@Module
object MainServiceModule {

    /**
     * Provides the service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun provideService(@Named("plain") retrofit: Retrofit): MainService {
        return retrofit.create(MainService::class.java)
    }
}

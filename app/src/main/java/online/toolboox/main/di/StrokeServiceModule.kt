package online.toolboox.main.di

import dagger.Module
import dagger.Provides
import dagger.Reusable
import online.toolboox.main.nw.StrokeService
import retrofit2.Retrofit

/**
 * Stroke service module, provides stroke service.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
@Module
object StrokeServiceModule {

    /**
     * Provides the service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun provideService(retrofit: Retrofit): StrokeService {
        return retrofit.create(StrokeService::class.java)
    }
}

package online.toolboox.plugin.templates.di

import dagger.Module
import dagger.Provides
import dagger.Reusable
import online.toolboox.plugin.templates.nw.TemplatesService
import retrofit2.Retrofit

/**
 * Templates service module, provides services.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
@Module
object TemplatesServiceModule {
    /**
     * Provides the templates service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun provideTemplatesService(retrofit: Retrofit): TemplatesService {
        return retrofit.create(TemplatesService::class.java)
    }
}

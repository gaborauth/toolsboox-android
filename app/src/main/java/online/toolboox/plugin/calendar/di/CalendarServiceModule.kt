package online.toolboox.plugin.calendar.di

import dagger.Module
import dagger.Provides
import dagger.Reusable
import online.toolboox.plugin.calendar.nw.CalendarService
import retrofit2.Retrofit

/**
 * Calendar service module, provides services.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
@Module
object CalendarServiceModule {
    /**
     * Provides the calendar service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun provideCalendarService(retrofit: Retrofit): CalendarService {
        return retrofit.create(CalendarService::class.java)
    }
}

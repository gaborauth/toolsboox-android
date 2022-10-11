package com.toolsboox.plugin.calendar.di

import dagger.Module
import dagger.Provides
import dagger.Reusable
import com.toolsboox.plugin.calendar.nw.CalendarService
import retrofit2.Retrofit

/**
 * Calendar service module, provides services.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
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

package com.toolsboox.plugin.calendar.di

import com.toolsboox.plugin.calendar.nw.CalendarService
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import retrofit2.Retrofit
import javax.inject.Named

/**
 * Calendar service module, provides services.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
object CalendarServiceModule {
    /**
     * Provides the calendar service.
     *
     * @param retrofit the Retrofit instance
     * @return the service
     */
    @Provides
    @Reusable
    fun provideCalendarService(@Named("accessToken") retrofit: Retrofit): CalendarService {
        return retrofit.create(CalendarService::class.java)
    }
}

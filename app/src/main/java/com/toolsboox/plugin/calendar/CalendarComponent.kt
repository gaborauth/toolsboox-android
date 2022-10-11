package com.toolsboox.plugin.calendar

import dagger.Component
import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.calendar.di.CalendarServiceModule
import com.toolsboox.plugin.calendar.ui.MainFragment
import javax.inject.Singleton

/**
 * Calendar plugin component.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Singleton
@Component(
    modules = [
        CalendarModule::class,
        CalendarServiceModule::class,
        NetworkModule::class
    ]
)
interface CalendarComponent {
    fun inject(plugin: CalendarPlugin)
    fun fragment(): MainFragment
}

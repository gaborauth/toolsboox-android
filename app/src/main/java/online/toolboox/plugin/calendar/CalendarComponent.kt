package online.toolboox.plugin.calendar

import dagger.Component
import online.toolboox.di.NetworkModule
import online.toolboox.plugin.calendar.di.CalendarServiceModule
import online.toolboox.plugin.calendar.ui.MainFragment
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

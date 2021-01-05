package online.toolboox.plugin.calendar

import online.toolboox.di.NetworkModule
import online.toolboox.plugin.calendar.di.CalendarServiceModule
import online.toolboox.ui.plugin.Plugin
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment

/**
 * Calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class CalendarPlugin(private val router: Router) : Plugin {

    private val component = DaggerCalendarComponent.builder()
        .calendarServiceModule(CalendarServiceModule)
        .calendarModule(CalendarModule(this, router))
        .networkModule(NetworkModule)
        .build()

    override fun getRoute(url: String): ScreenFragment? {
        router.getParameters("/calendar", url).let {
            if (it is Router.Parameters.Match) return component.fragment().setParameters(it.parameters)
        }

        return null
    }
}

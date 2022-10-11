package com.toolsboox.plugin.calendar

import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.calendar.di.CalendarServiceModule
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment

/**
 * Calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
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

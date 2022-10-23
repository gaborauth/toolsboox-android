package com.toolsboox.plugin.calendar

import com.toolsboox.plugin.calendar.ui.*
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment
import javax.inject.Inject

/**
 * Calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarPlugin @Inject constructor() : Plugin {

    override fun getRoute(url: String): ScreenFragment? {
        Router.getParameters("/calendar/year/(?<year>.*)", url).let {
            if (it is Router.Parameters.Match) return CalendarYearFragment().setParameters(it.parameters)
        }
        Router.getParameters("/calendar/year", url).let {
            if (it is Router.Parameters.Match) return CalendarYearFragment().setParameters(it.parameters)
        }

        Router.getParameters("/calendar/quarter/(?<year>.*)/(?<quarter>.*)", url).let {
            if (it is Router.Parameters.Match) return CalendarQuarterFragment().setParameters(it.parameters)
        }
        Router.getParameters("/calendar/quarter", url).let {
            if (it is Router.Parameters.Match) return CalendarQuarterFragment().setParameters(it.parameters)
        }

        Router.getParameters("/calendar/month/(?<year>.*)/(?<month>.*)", url).let {
            if (it is Router.Parameters.Match) return CalendarMonthFragment().setParameters(it.parameters)
        }
        Router.getParameters("/calendar/month", url).let {
            if (it is Router.Parameters.Match) return CalendarMonthFragment().setParameters(it.parameters)
        }

        Router.getParameters("/calendar/week/(?<year>.*)/(?<week>.*)", url).let {
            if (it is Router.Parameters.Match) return CalendarWeekFragment().setParameters(it.parameters)
        }
        Router.getParameters("/calendar/week", url).let {
            if (it is Router.Parameters.Match) return CalendarWeekFragment().setParameters(it.parameters)
        }

        Router.getParameters("/calendar", url).let {
            if (it is Router.Parameters.Match) return CalendarMainFragment().setParameters(it.parameters)
        }

        return null
    }
}

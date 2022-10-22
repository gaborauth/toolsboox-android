package com.toolsboox.plugin.calendar

import com.toolsboox.plugin.calendar.ui.CalendarMainFragment
import com.toolsboox.plugin.calendar.ui.CalendarQuarterFragment
import com.toolsboox.plugin.calendar.ui.CalendarYearFragment
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

        Router.getParameters("/calendar", url).let {
            if (it is Router.Parameters.Match) return CalendarMainFragment().setParameters(it.parameters)
        }

        return null
    }
}

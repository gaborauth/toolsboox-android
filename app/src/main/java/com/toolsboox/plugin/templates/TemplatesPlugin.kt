package com.toolsboox.plugin.templates

import com.toolsboox.plugin.templates.ui.*
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment
import javax.inject.Inject

/**
 * Templates plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class TemplatesPlugin @Inject constructor() : Plugin {

    override fun getRoute(url: String): ScreenFragment? {
        Router.getParameters("/templates", url).let {
            if (it is Router.Parameters.Match) return MainFragment().setParameters(it.parameters)
        }
        Router.getParameters("/templates/boxedDaysCalendar", url).let {
            if (it is Router.Parameters.Match) return BoxedDaysCalendarFragment().setParameters(it.parameters)
        }
        Router.getParameters("/templates/boxedWeeksCalendar", url).let {
            if (it is Router.Parameters.Match) return BoxedWeeksCalendarFragment().setParameters(it.parameters)
        }
        Router.getParameters("/templates/community", url).let {
            if (it is Router.Parameters.Match) return CommunityFragment().setParameters(it.parameters)
        }
        Router.getParameters("/templates/flatWeeksCalendar", url).let {
            if (it is Router.Parameters.Match) return FlatWeeksCalendarFragment().setParameters(it.parameters)
        }
        Router.getParameters("/templates/thisWeeksCalendar", url).let {
            if (it is Router.Parameters.Match) return ThisWeeksCalendarFragment().setParameters(it.parameters)
        }

        return null
    }
}

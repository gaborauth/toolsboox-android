package com.toolsboox.plugin.templates

import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.templates.di.TemplatesServiceModule
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment

/**
 * Templates plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class TemplatesPlugin(private val router: Router) : Plugin {

    private val component = DaggerTemplatesComponent.builder()
        .templatesServiceModule(TemplatesServiceModule)
        .templatesModule(TemplatesModule(this, router))
        .networkModule(NetworkModule)
        .build()

    override fun getRoute(url: String): ScreenFragment? {
        router.getParameters("/templates", url).let {
            if (it is Router.Parameters.Match) return component.mainFragment().setParameters(it.parameters)
        }
        router.getParameters("/templates/boxedDaysCalendar", url).let {
            if (it is Router.Parameters.Match) return component.boxedDaysCalendarFragment().setParameters(it.parameters)
        }
        router.getParameters("/templates/boxedWeeksCalendar", url).let {
            if (it is Router.Parameters.Match) return component.boxedWeeksCalendarFragment().setParameters(it.parameters)
        }
        router.getParameters("/templates/community", url).let {
            if (it is Router.Parameters.Match) return component.communityFragment().setParameters(it.parameters)
        }
        router.getParameters("/templates/flatWeeksCalendar", url).let {
            if (it is Router.Parameters.Match) return component.flatWeeksCalendarFragment().setParameters(it.parameters)
        }
        router.getParameters("/templates/thisWeeksCalendar", url).let {
            if (it is Router.Parameters.Match) return component.thisWeeksCalendarFragment().setParameters(it.parameters)
        }

        return null
    }
}

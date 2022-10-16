package com.toolsboox.ui

import androidx.fragment.app.FragmentActivity
import com.toolsboox.plugin.calendar.CalendarPlugin
import com.toolsboox.plugin.dashboard.DashboardPlugin
import com.toolsboox.plugin.kanban.KanbanPlugin
import com.toolsboox.plugin.teamdrawer.TeamDrawerPlugin
import com.toolsboox.plugin.templates.TemplatesPlugin
import com.toolsboox.ui.main.MainActivity
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router
import javax.inject.Inject

/**
 * Default fragment router.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class DefaultRouter @Inject constructor(private val activity: FragmentActivity) : Router {

    @Inject
    lateinit var calendarPlugin: CalendarPlugin

    @Inject
    lateinit var dashboardPlugin: DashboardPlugin

    @Inject
    lateinit var kanbanPlugin: KanbanPlugin

    @Inject
    lateinit var teamDrawerPlugin: TeamDrawerPlugin

    @Inject
    lateinit var templatesPlugin: TemplatesPlugin

    /**
     * Dispatch the specified URL.
     *
     * @param url the URL of the route
     * @param replace replace the fragment
     */
    override fun dispatch(url: String, replace: Boolean) {
        if (!(activity is MainActivity)) return

        val plugins: List<Plugin> = listOf(
            calendarPlugin,
            dashboardPlugin,
            kanbanPlugin,
            teamDrawerPlugin,
            templatesPlugin,
        )

        plugins.forEach { plugin ->
            plugin.getRoute(url)?.let { fragment -> activity.addFragment(fragment, replace); return }
        }

        activity.showRouterNotImplemented(url)
    }
}

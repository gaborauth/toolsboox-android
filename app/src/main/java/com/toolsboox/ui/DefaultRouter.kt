package com.toolsboox.ui

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.code.regexp.Pattern
import com.toolsboox.R
import com.toolsboox.ui.main.MainActivity
import com.toolsboox.plugin.calendar.CalendarPlugin
import com.toolsboox.plugin.dashboard.DashboardPlugin
import com.toolsboox.plugin.kanban.KanbanPlugin
import com.toolsboox.plugin.teamdrawer.TeamDrawerPlugin
import com.toolsboox.plugin.templates.TemplatesPlugin
import com.toolsboox.ui.plugin.Plugin
import com.toolsboox.ui.plugin.Router

/**
 * Default fragment router.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class DefaultRouter(private val main: MainActivity, private val view: View) : Router {

    private val plugins: List<Plugin> = listOf(
        CalendarPlugin(this),
        DashboardPlugin(this),
        KanbanPlugin(this),
        TeamDrawerPlugin(this),
        TemplatesPlugin(this),
    )

    /**
     * Dispatch the specified URL.
     *
     * @param url the URL
     */
    override fun dispatch(url: String, replace: Boolean) {
        plugins.forEach { plugin ->
            plugin.getRoute(url)?.let { fragment -> main.addFragment(fragment, replace); return }
        }

        val message = main.getString(R.string.router_not_implemented_yet).format(url)
        Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.router_not_implemented_yet_action) {}
            .show()
    }

    /**
     * Return with the parameter of the URL.
     */
    override fun getParameters(pattern: String, url: String): Router.Parameters =
        Pattern.compile(pattern).matcher(url).let {
            when {
                it.matches() && it.groupCount() > 0 -> {
                    Router.Parameters.Match(it.namedGroups().firstOrNull() ?: mapOf())
                }
                it.matches() -> {
                    Router.Parameters.Match(mapOf())
                }
                else -> {
                    Router.Parameters.NoMatch(mapOf())
                }
            }
        }
}

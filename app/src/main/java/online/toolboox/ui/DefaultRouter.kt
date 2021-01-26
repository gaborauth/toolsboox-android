package online.toolboox.ui

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.code.regexp.Pattern
import online.toolboox.R
import online.toolboox.ui.main.MainActivity
import online.toolboox.plugin.calendar.CalendarPlugin
import online.toolboox.plugin.dashboard.DashboardPlugin
import online.toolboox.plugin.kanban.KanbanPlugin
import online.toolboox.plugin.teamdrawer.TeamDrawerPlugin
import online.toolboox.plugin.templates.TemplatesPlugin
import online.toolboox.ui.plugin.Plugin
import online.toolboox.ui.plugin.Router

/**
 * Default fragment router.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
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

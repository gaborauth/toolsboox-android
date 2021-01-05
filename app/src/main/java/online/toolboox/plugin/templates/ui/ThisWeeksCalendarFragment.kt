package online.toolboox.plugin.templates.ui

import android.os.Bundle
import android.view.View
import online.toolboox.R
import online.toolboox.databinding.FragmentTemplatesThisWeeksCalendarBinding
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment
import javax.inject.Inject

/**
 * Templates 'this week's calendar' fragment.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class ThisWeeksCalendarFragment @Inject constructor(
    private val presenter: ThisWeeksCalendarPresenter,
    private val router: Router
) : ScreenFragment() {

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_templates_this_weeks_calendar

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTemplatesThisWeeksCalendarBinding

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTemplatesThisWeeksCalendarBinding.bind(view)
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.templates_this_weeks_calendar_title))
    }

    /**
     * Show the progress bar.
     */
    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    /**
     * Hide the progress bar.
     */
    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }
}

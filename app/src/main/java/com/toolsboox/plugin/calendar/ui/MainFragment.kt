package com.toolsboox.plugin.calendar.ui

import android.os.Bundle
import android.view.View
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarMainBinding
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment
import javax.inject.Inject

/**
 * Calendar main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class MainFragment @Inject constructor(
    private val presenter: MainPresenter,
    private val router: Router
) : ScreenFragment() {

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_calendar_main

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarMainBinding

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarMainBinding.bind(view)
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.calendar_main_title))
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

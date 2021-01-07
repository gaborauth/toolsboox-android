package online.toolboox.plugin.templates.ui

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import online.toolboox.R
import online.toolboox.da.SquareItem
import online.toolboox.databinding.FragmentTemplatesMainBinding
import online.toolboox.ot.SquareItemAdapter
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment
import timber.log.Timber
import javax.inject.Inject

/**
 * Templates main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class MainFragment @Inject constructor(
    private val presenter: MainPresenter,
    private val router: Router
) : ScreenFragment() {

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_templates_main

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTemplatesMainBinding

    /**
     * The dashboard item adapter.
     */
    private lateinit var adapter: SquareItemAdapter

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTemplatesMainBinding.bind(view)

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainFragment.requireContext(), 4)
        }

        val squareItems = mutableListOf<SquareItem>()
        squareItems.add(
            SquareItem(
                "This week's\ncalendar", R.drawable.ic_dashboard_item_calendar,
                "/templates/thisWeeksCalendar"
            )
        )
        squareItems.add(
            SquareItem(
                "Weeks\ncalendar", R.drawable.ic_dashboard_item_calendar,
                "/templates/weeksCalendar"
            )
        )

        val clickListener = object : SquareItemAdapter.OnItemClickListener {
            override fun onItemClicked(squareItem: SquareItem) {
                Timber.i("Route to ${squareItem.routeUrl}")
                router.dispatch(squareItem.routeUrl, false)
            }
        }

        adapter = SquareItemAdapter(this.requireContext(), squareItems, clickListener)
        binding.recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.templates_main_title))
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

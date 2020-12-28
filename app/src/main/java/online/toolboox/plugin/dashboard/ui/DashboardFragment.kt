package online.toolboox.plugin.dashboard.ui

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import online.toolboox.R
import online.toolboox.databinding.FragmentDashboardBinding
import online.toolboox.plugin.dashboard.da.DashboardItem
import online.toolboox.plugin.dashboard.ot.DashboardItemAdapter
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment
import timber.log.Timber
import javax.inject.Inject

/**
 * Dashboard main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class DashboardFragment @Inject constructor(
    private val presenter: DashboardPresenter,
    private val router: Router
) : ScreenFragment() {

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_dashboard

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentDashboardBinding

    /**
     * The dashboard item adapter.
     */
    private lateinit var adapter: DashboardItemAdapter

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentDashboardBinding.bind(view)

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@DashboardFragment.requireContext(), 4)
        }

        val dashboardItems = mutableListOf<DashboardItem>()
        dashboardItems.add(
            DashboardItem(
                "Calendar", R.drawable.ic_dashboard_item_calendar,
                "/calendar"
            )
        )
        dashboardItems.add(
            DashboardItem(
                "Templates", R.drawable.ic_dashboard_item_templates,
                "/templates"
            )
        )
        dashboardItems.add(
            DashboardItem(
                "TeamDrawer", R.drawable.ic_dashboard_item_teamdrawer,
                "/teamDrawer/178e0a77-d9d2-4a88-b29c-b09007972b53"
            )
        )

        val clickListener = object: DashboardItemAdapter.OnItemClickListener {
            override fun onItemClicked(dashboardItem: DashboardItem) {
                Timber.i("Route to ${dashboardItem.routeUrl}")
                router.dispatch(dashboardItem.routeUrl, false)
            }
        }

        adapter = DashboardItemAdapter(this.requireContext(), dashboardItems, clickListener)
        binding.recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.dashboard_title))
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

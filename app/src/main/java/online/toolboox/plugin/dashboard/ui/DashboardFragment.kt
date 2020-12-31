package online.toolboox.plugin.dashboard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import online.toolboox.BuildConfig
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
                "/teamDrawer"
            )
        )

        val clickListener = object : DashboardItemAdapter.OnItemClickListener {
            override fun onItemClicked(dashboardItem: DashboardItem) {
                Timber.i("Route to ${dashboardItem.routeUrl}")
                router.dispatch(dashboardItem.routeUrl, false)
            }
        }

        adapter = DashboardItemAdapter(this.requireContext(), dashboardItems, clickListener)
        binding.recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()

        presenter.version(this)
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.dashboard_title))
    }

    /**
     * Render the result of 'version' service call.
     *
     * @param version the version code
     */
    fun versionResult(version: Int) {
        if (BuildConfig.VERSION_CODE < version) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
            builder.setTitle(R.string.dashboard_new_version_title)
                .setMessage(R.string.dashboard_new_version_message)
                .setPositiveButton( R.string.main_update
                ) { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://toolboox.online/changelog"))
                    this.startActivity(intent)
                }
                .setNegativeButton( R.string.main_update_not_now
                ) { dialog, _ ->
                    dialog.cancel()
                }
            builder.create().show()
        }
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

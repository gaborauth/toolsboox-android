package com.toolsboox.plugin.dashboard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.toolsboox.BuildConfig
import com.toolsboox.R
import com.toolsboox.databinding.FragmentDashboardBinding
import com.toolsboox.da.SquareItem
import com.toolsboox.ot.SquareItemAdapter
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment
import timber.log.Timber
import javax.inject.Inject

/**
 * Dashboard main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
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
    private lateinit var adapter: SquareItemAdapter

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

        val squareItems = mutableListOf<SquareItem>()
        squareItems.add(
            SquareItem(
                "Calendar", R.drawable.ic_dashboard_item_calendar,
                "/calendar"
            )
        )
        squareItems.add(
            SquareItem(
                "Templates", R.drawable.ic_dashboard_item_templates,
                "/templates"
            )
        )
        squareItems.add(
            SquareItem(
                "TeamDrawer", R.drawable.ic_dashboard_item_teamdrawer,
                "/teamDrawer"
            )
        )
        squareItems.add(
            SquareItem(
                "Kanban\nplanner", R.drawable.ic_dashboard_item_kanban,
                "/kanbanPlanner"
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
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://toolsboox.com/changelog"))
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

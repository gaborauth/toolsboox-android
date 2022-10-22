package com.toolsboox.plugin.dashboard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.toolsboox.BuildConfig
import com.toolsboox.R
import com.toolsboox.da.SquareItem
import com.toolsboox.databinding.FragmentDashboardBinding
import com.toolsboox.ot.SquareItemAdapter
import com.toolsboox.plugin.dashboard.da.Version
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Dashboard main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class DashboardFragment @Inject constructor() : ScreenFragment() {

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var presenter: DashboardPresenter

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
     * User already notified about device mismatch.
     */
    private var notifiedAboutDeviceMismatch: Boolean = false

    /**
     * User already notified about new version.
     */
    private var notifiedAboutNewVersion: Boolean = false

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
                "/calendar/year"
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

        askAppPermissions()
        presenter.version(this)
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.dashboard_title))

        deviceCheck()
    }

    /**
     * Render the result of brand mismatch dialog.
     */
    private fun deviceCheck() {
        val brand = android.os.Build.BRAND.lowercase().contains("onyx")
        val device = android.os.Build.DEVICE.lowercase().contains("onyx")
        val manufacturer = android.os.Build.MANUFACTURER.lowercase().contains("onyx")
        if (brand || device || manufacturer || notifiedAboutDeviceMismatch) return

        val message = getString(R.string.dashboard_device_mismatch_message)
            .format(android.os.Build.BRAND, android.os.Build.DEVICE)

        notifiedAboutDeviceMismatch = true
        val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
        builder.setTitle(R.string.dashboard_device_mismatch_title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.cancel()
            }
        builder.create().show()
    }

    /**
     * Render the result of 'version' service call.
     *
     * @param version the version code
     */
    fun versionResult(version: Version) {
        val installer = requireContext().packageManager.getInstallerPackageName(requireContext().packageName)
        Timber.e("Installer package: $installer")
        if (installer != null) return
        if (notifiedAboutNewVersion) return

        if (BuildConfig.VERSION_CODE < version.versionCode) {
            notifiedAboutNewVersion = true

            val filename = "toolboox-prod-release-${version.versionName}.apk"
            val url = "https://github.com/gaborauth/toolsboox-android/releases/latest/download/$filename"

            val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
            builder.setTitle(R.string.dashboard_new_version_title)
                .setMessage(R.string.dashboard_new_version_message)
                .setPositiveButton(R.string.main_update) { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    this.startActivity(intent)
                }
                .setNegativeButton(
                    R.string.main_update_not_now
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

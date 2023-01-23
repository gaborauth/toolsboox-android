package com.toolsboox.plugin.dashboard.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.input.InputManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.InputDevice
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.toolsboox.BuildConfig
import com.toolsboox.R
import com.toolsboox.da.SquareItem
import com.toolsboox.databinding.FragmentDashboardBinding
import com.toolsboox.ot.SquareItemAdapter
import com.toolsboox.plugin.dashboard.da.Version
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

    companion object {
        /**
         * User already notified about device mismatch.
         */
        private var notifiedAboutDeviceMismatch: Boolean = false

        /**
         * User already notified about new version.
         */
        private var notifiedAboutNewVersion: Boolean = false
    }

    /**
     * The Firebase analytics.
     */
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The injected presenter.
     */
    @Inject
    lateinit var presenter: DashboardPresenter

    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

    /**
     * The injected presenter.
     */
    @Inject
    lateinit var sharedPreferences: SharedPreferences

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

        val calendarStartActionId = when (sharedPreferences.getInt("calendarStartView", 0)) {
            0 -> R.id.action_to_calendar_day
            1 -> R.id.action_to_calendar_week
            2 -> R.id.action_to_calendar_month
            3 -> R.id.action_to_calendar_quarter
            4 -> R.id.action_to_calendar_year
            else -> R.id.action_to_calendar_day
        }

        val androidId = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        sharedPreferences.edit().putString("androidId", androidId).apply()
        Timber.i("Using AndroidId: $androidId")
        val earlyAdopterDeviceIdsJson = sharedPreferences.getString("earlyAdopterDeviceIds", "[]")

        val earlyAdopterDeviceIdsType = Types.newParameterizedType(MutableList::class.java, String::class.java)
        val jsonAdapter = moshi.adapter<List<String>>(earlyAdopterDeviceIdsType)
        val earlyAdopterDeviceIds = jsonAdapter.fromJson(earlyAdopterDeviceIdsJson!!)

        val squareItems = mutableListOf<SquareItem>()
        squareItems.add(
            SquareItem(
                getString(R.string.dashboard_item_calendar_title), R.drawable.ic_dashboard_item_calendar,
                calendarStartActionId, bundleOf()
            )
        )
        squareItems.add(
            SquareItem(
                getString(R.string.dashboard_item_templates_title), R.drawable.ic_dashboard_item_templates,
                R.id.action_to_templates_main, bundleOf()
            )
        )
        squareItems.add(
            SquareItem(
                getString(R.string.dashboard_item_teamdrawer_title), R.drawable.ic_dashboard_item_teamdrawer,
                R.id.action_to_teamdrawer_room, bundleOf()
            )
        )
        squareItems.add(
            SquareItem(
                getString(R.string.dashboard_item_kanban_planner_title), R.drawable.ic_dashboard_item_kanban,
                R.id.action_to_kanban_main, bundleOf()
            )
        )
        squareItems.add(
            SquareItem(
                getString(R.string.dashboard_item_about_title), R.drawable.ic_dashboard_item_about,
                R.id.action_to_about, bundleOf()
            )
        )

        // Hide the cloud feature in case of regular users.
        if (earlyAdopterDeviceIds?.contains(androidId) == true) {
            squareItems.add(
                SquareItem(
                    getString(R.string.dashboard_item_cloud_title), R.drawable.ic_dashboard_item_cloud,
                    R.id.action_to_cloud, bundleOf()
                )
            )
        }

        val clickListener = object : SquareItemAdapter.OnItemClickListener {
            override fun onItemClicked(squareItem: SquareItem) {
                Timber.i("Route to ${squareItem.title}")
                findNavController().navigate(squareItem.actionId, squareItem.bundle)
            }
        }

        adapter = SquareItemAdapter(this.requireContext(), squareItems, clickListener)
        binding.recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()

        presenter.parameter(this, "earlyAdopterDeviceIds")
        presenter.version(this)

        val inputManager = requireContext().getSystemService(Context.INPUT_SERVICE) as InputManager?
        val inputs = inputManager!!.inputDeviceIds
        for (i in inputs.indices) {
            val inputDevice = inputManager.getInputDevice(inputs[i])
            if (inputDevice.supportsSource(InputDevice.SOURCE_STYLUS)) {
                Timber.e("Input %s supports stylus input", inputDevice.name)
            }
        }

        binding.buttonToggleAd.setOnClickListener {
            if (sharedPreferences.getBoolean("advertisements", true)) {
                sharedPreferences.edit().putBoolean("advertisements", false).apply()
                firebaseAnalytics.logEvent("advertisementSwitchOff") {}
            } else {
                sharedPreferences.edit().putBoolean("advertisements", true).apply()
                firebaseAnalytics.logEvent("advertisementSwitchOn") {}
            }

            updateAdButton()
        }
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.dashboard_title))

        firebaseAnalytics.logEvent("dashboard") {}

        askAppPermissions()
        deviceCheck()
        apiLevelCheck()

        updateAdButton()
    }

    /**
     * Render the result of API level check dialog.
     */
    private fun apiLevelCheck() {
        val notifiedAboutApiLevelWarning = sharedPreferences.getBoolean("notifiedAboutApiLevelWarning", false)
        if (notifiedAboutApiLevelWarning) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            sharedPreferences.edit().putBoolean("notifiedAboutApiLevelWarning", true).apply()

            val androidVersion = when (Build.VERSION.SDK_INT) {
                Build.VERSION_CODES.R -> "11"
                Build.VERSION_CODES.S -> "12"
                else -> "13+"
            }

            val message = getString(R.string.dashboard_api_level_warning_message).format(androidVersion)

            val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
            builder.setTitle(R.string.dashboard_api_level_warning_title)
                .setMessage(message)
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    dialog.cancel()
                }
            builder.create().show()
        }
    }

    /**
     * Render the result of brand mismatch dialog.
     */
    private fun deviceCheck() {
        val brand = Build.BRAND.lowercase().contains("onyx")
        val device = Build.DEVICE.lowercase().contains("onyx")
        val manufacturer = Build.MANUFACTURER.lowercase().contains("onyx")
        if (brand || device || manufacturer || notifiedAboutDeviceMismatch) return

        val message = getString(R.string.dashboard_device_mismatch_message).format(Build.BRAND, Build.DEVICE)

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
     * Render the result of 'parameter' service call.
     *
     * @param key the key
     * @param value the value
     */
    fun parameterResult(key: String, value: String) {
        Timber.i("Store parameter in shared preferences: $key - $value")
        sharedPreferences.edit().putString(key, value).apply()
    }

    /**
     * Render the result of 'version' service call.
     *
     * @param version the version code
     */
    fun versionResult(version: Version) {
        val installer = requireContext().packageManager.getInstallerPackageName(requireContext().packageName)

        if (BuildConfig.VERSION_CODE >= version.versionCode) return
        if (notifiedAboutNewVersion) return
        notifiedAboutNewVersion = true

        if (installer == null) {
            val filename = "toolboox-prod-release-${version.versionName}.apk"
            val url = "https://github.com/gaborauth/toolsboox-android/releases/latest/download/$filename"
            Timber.i("The update URL is '$url'")

            val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
            builder.setTitle(R.string.dashboard_new_version_title)
                .setMessage(R.string.dashboard_new_version_message)
                .setPositiveButton(R.string.main_update) { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    this.startActivity(intent)
                }
                .setNegativeButton(R.string.main_update_not_now) { dialog, _ -> dialog.cancel() }
            builder.create().show()
        } else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
            builder.setTitle(R.string.dashboard_new_version_title)
                .setMessage(R.string.dashboard_new_version_message)
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.cancel() }
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

    /**
     * Update the state of the advertisement enable-disable button.
     */
    private fun updateAdButton() {
        if (sharedPreferences.getBoolean("advertisements", true)) {
            binding.buttonToggleAd.text = getString(R.string.dashboard_ad_settings_button_hide)
            binding.adView.loadAd(AdRequest.Builder().build())
            binding.adView.visibility = View.VISIBLE
        } else {
            binding.buttonToggleAd.text = getString(R.string.dashboard_ad_settings_button_show)
            binding.adView.visibility = View.GONE
            binding.adView.destroy()
        }
    }
}

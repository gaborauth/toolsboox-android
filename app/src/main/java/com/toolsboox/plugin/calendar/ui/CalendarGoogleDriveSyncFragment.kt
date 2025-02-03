package com.toolsboox.plugin.calendar.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.api.services.drive.Drive
import com.google.firebase.analytics.FirebaseAnalytics
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarGoogleDriveSyncBinding
import com.toolsboox.di.GoogleDriveModule
import com.toolsboox.ot.StringArrayAdapter
import com.toolsboox.plugin.calendar.da.v1.CalendarItem
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Calendar Google Drive sync fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarGoogleDriveSyncFragment @Inject constructor() : ScreenFragment() {

    /**
     * The shared preferences.
     */
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    /**
     * The Firebase analytics.
     */
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarGoogleDriveSyncPresenter

    // Google sign-in client.
    @Inject
    lateinit var googleSignInClient: GoogleSignInClient

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_calendar_google_drive_sync

    /**
     * Stored items in the cloud.
     */
    private val cloudList: MutableList<CalendarItem> = mutableListOf()

    /**
     * Stored items in the device.
     */
    private val fileList: MutableList<CalendarItem> = mutableListOf()

    /**
     * Calendar items selected to sync from cloud.
     */
    private val fromCloudList: MutableList<CalendarItem> = mutableListOf()

    /**
     * Calendar items selected to sync to cloud.
     */
    private val toCloudList: MutableList<CalendarItem> = mutableListOf()

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarGoogleDriveSyncBinding

    // Access token for Google Drive.
    private var googleAccount: GoogleSignInAccount? = null

    // Google Drive service.
    private var googleDrive: Drive? = null

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarGoogleDriveSyncBinding.bind(view)

        toolbar.toolbarPager.visibility = View.GONE

        // Set the compare button, to compare the cloud and the device.
        binding.buttonCompare.setOnClickListener {
            if (googleAccount != null && googleDrive != null) {
                presenter.fileList(this@CalendarGoogleDriveSyncFragment, UUID.randomUUID(), binding)
            }
        }

        // Set the to-cloud list adapter.
        binding.toCloudListView.setOnItemClickListener { _, _, position, _ ->
            if (toCloudList.isEmpty()) return@setOnItemClickListener
            Timber.i("Try to upload: %s", toCloudList[position])
            presenter.fileLoad(this@CalendarGoogleDriveSyncFragment, toCloudList[position], binding)
        }

        // Set the from-cloud list adapter.
        binding.fromCloudListView.setOnItemClickListener { _, _, position, _ ->
            if (fromCloudList.isEmpty()) return@setOnItemClickListener
            Timber.i("Try to download: %s", fromCloudList[position])
            presenter.cloudLoad(this@CalendarGoogleDriveSyncFragment, googleDrive!!, fromCloudList[position])
        }

        binding.buttonCompare.isEnabled = false
        binding.buttonCompare.alpha = 0.5f

        binding.toCloudText.text = resources.getString(R.string.calendar_google_drive_to_cloud_text)
        binding.toCloudListView.emptyView = binding.toCloudListEmpty
        binding.fromCloudText.text = resources.getString(R.string.calendar_google_drive_from_cloud_text)
        binding.fromCloudListView.emptyView = binding.fromCloudListEmpty
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.calendar_google_drive_title))

        googleAccount = null
        binding.googleDriveStatusMessage.text = getString(R.string.calendar_google_drive_disconnected)
        binding.buttonCompare.isEnabled = false
        binding.buttonCompare.alpha = 0.5f

        // Check Google Drive connection.
        googleSignInClient.silentSignIn()
            .addOnSuccessListener { result ->
                Timber.i("Silent-signed into a GoogleAccount: ${result.id}")
                googleAccount = result

                requireActivity().runOnUiThread {
                    binding.googleDriveStatusMessage.text = getString(R.string.calendar_google_drive_connected)
                    binding.buttonCompare.isEnabled = true
                    binding.buttonCompare.alpha = 1.0f

                    GoogleDriveModule.provideCredential(this.requireContext(), googleAccount!!)
                        .let { credential ->
                            googleDrive = GoogleDriveModule.provideDrive(credential)
                        }
                }
            }
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        toolbar.toolbarPager.visibility = View.GONE
    }

    /**
     * File items list result.
     *
     * @param calendarItems the calendar items on the device
     */
    fun fileListResult(calendarItems: List<CalendarItem>) {
        fileList.clear()
        fileList.addAll(calendarItems.distinctBy { cis -> cis.baseName })

        presenter.cloudList(this@CalendarGoogleDriveSyncFragment, googleDrive!!)
    }

    /**
     * Cloud items list result.
     *
     * @param calendarItems the calendar items in the cloud
     */
    fun cloudListResult(calendarItems: List<CalendarItem>) {
        cloudList.clear()
        cloudList.addAll(calendarItems)

        updateListViews()
    }

    /**
     * Result of the JSON file load from filesystem.
     *
     * @param calendarItem the calendar item
     */
    fun fileLoadResult(calendarItem: CalendarItem) {
        Timber.i("FileLoadResult: $calendarItem")
        presenter.cloudUpdate(this@CalendarGoogleDriveSyncFragment, googleDrive!!, calendarItem)
    }

    /**
     * Result of the JSON file load from cloud.
     *
     * @param calendarItem the calendar item
     */
    fun cloudLoadResult(calendarItem: CalendarItem) {
        Timber.i("CloudLoadResult: $calendarItem")
        presenter.fileUpdate(this@CalendarGoogleDriveSyncFragment, calendarItem, binding)
    }

    /**
     * Result of the file update process.
     *
     * @param calendarItem the calendar item
     */
    fun fileUpdateResult(calendarItem: CalendarItem) {
        Timber.i("$calendarItem - $calendarItem")
    }

    /**
     * Result of the cloud update process.
     *
     * @param calendarItem the calendar item
     *
     */
    fun cloudUpdateResult(calendarItem: CalendarItem) {
        Timber.i("$calendarItem")
    }

    /**
     * Update the list view of sync items.
     */
    private fun updateListViews() {
        toCloudList.clear()
        fileList.forEach { fci ->
            // If the file is not in the cloud, add it.
            if (fci.updated == null) {
                toCloudList.add(fci)
                return@forEach
            }

            // If the file is in the cloud, checks the path, the baseName and the version.
            val cloudItem = cloudList
                .filter { it.path == fci.path }
                .filter { it.baseName == fci.baseName }
                .firstOrNull { it.version == fci.version }

            // If the file is not in the cloud, add it.
            if (cloudItem == null) {
                toCloudList.add(fci)
                return@forEach
            }

            if ((cloudItem.updated != null) and (cloudItem.updated!!.time < fci.updated.time)) {
                toCloudList.add(fci)
                return@forEach
            }
        }

        toCloudList.sortWith { ci1, ci2 -> ci2.baseName.compareTo(ci1.baseName) }
        Timber.i("To cloud list: $toCloudList")
        requireActivity().runOnUiThread {
            val toCloudAdapter = StringArrayAdapter(
                this.requireContext(),
                R.layout.list_item_locale, toCloudList
            ) { i -> "${i.path}${i.baseName}-${i.version}\n(${i.updated})" }
            binding.toCloudListView.adapter = toCloudAdapter
            toCloudAdapter.notifyDataSetChanged()
        }

        fromCloudList.clear()
        cloudList.forEach { cci ->
            // It's a bug?!
            if (cci.updated == null) {
                return@forEach
            }

            val fileItem = fileList
                .filter { it.path == cci.path }
                .filter { it.baseName == cci.baseName }
                .firstOrNull { it.version == cci.version }

            if (fileItem == null) {
                fromCloudList.add(cci)
            } else if (fileItem.updated != null) {
                if (fileItem.updated.time < cci.updated.time) {
                    fromCloudList.add(cci)
                }
            }
        }

        fromCloudList.sortWith { ci1, ci2 -> ci2.baseName.compareTo(ci1.baseName) }
        Timber.i("From cloud list: $fromCloudList")
        requireActivity().runOnUiThread {
            val fromCloudAdapter = StringArrayAdapter(
                this.requireContext(),
                R.layout.list_item_locale, fromCloudList
            ) { i -> "${i.path}${i.baseName}-${i.version}\n(${i.updated})" }
            binding.fromCloudListView.adapter = fromCloudAdapter
            fromCloudAdapter.notifyDataSetChanged()
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

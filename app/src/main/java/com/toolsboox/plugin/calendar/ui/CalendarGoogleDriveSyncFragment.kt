package com.toolsboox.plugin.calendar.ui

import android.app.ProgressDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.api.services.drive.Drive
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarGoogleDriveSyncBinding
import com.toolsboox.di.GoogleDriveModule
import com.toolsboox.plugin.calendar.da.v1.CalendarSyncItem
import com.toolsboox.plugin.calendar.da.v1.CalendarSyncViewItem
import com.toolsboox.plugin.calendar.ot.CalendarSyncViewAdapter
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
    private val cloudList: MutableList<CalendarSyncItem> = mutableListOf()

    /**
     * Stored items in the device.
     */
    private val fileList: MutableList<CalendarSyncItem> = mutableListOf()

    /**
     * Calendar items selected to sync from and to cloud.
     */
    private val syncList: MutableList<CalendarSyncViewItem> = mutableListOf()

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarGoogleDriveSyncBinding

    // Progress dialog.
    private lateinit var progressDialog: ProgressDialog

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

        progressDialog = ProgressDialog(this.requireContext())

        // Set the compare button, to compare the cloud and the device.
        binding.buttonCompare.setOnClickListener {
            if (googleAccount != null && googleDrive != null) {
                firebaseAnalytics.logEvent("googleDriveSync") {
                    param("method", "compareButton")
                }
                presenter.fileList(this@CalendarGoogleDriveSyncFragment, UUID.randomUUID(), binding)
            }
        }

        // Set the sync list adapter.
        val syncViewAdapter = CalendarSyncViewAdapter(requireContext(), syncList, { item ->
            val fileLastModified = item.file?.updated?.time ?: 0
            val cloudLastModified = item.cloud?.updated?.time ?: 0
            if (fileLastModified < cloudLastModified) {
                presenter.cloudLoad(this@CalendarGoogleDriveSyncFragment, googleDrive!!, item.cloud!!)
            } else {
                presenter.fileLoad(this@CalendarGoogleDriveSyncFragment, item.file!!, binding)
            }
        })

        binding.syncListView.adapter = syncViewAdapter
        binding.syncListView.layoutManager = LinearLayoutManager(this.requireContext())

        binding.buttonCompare.isEnabled = false
        binding.buttonCompare.alpha = 0.5f
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

        val googleDriveAutoSyncOptIn = sharedPreferences.getString("googleDriveAutoSyncOptIn", "false").toBoolean()
        binding.googleDriveBackgroundSync.isChecked = googleDriveAutoSyncOptIn

        binding.googleDriveBackgroundSync.setOnClickListener { _ ->
            val isChecked = binding.googleDriveBackgroundSync.isChecked
            sharedPreferences.edit().putString("googleDriveAutoSyncOptIn", isChecked.toString()).apply()
        }

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
                            firebaseAnalytics.logEvent("googleDriveSync") {
                                param("method", "autoStart")
                            }
                            presenter.fileList(this@CalendarGoogleDriveSyncFragment, UUID.randomUUID(), binding)
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
     * @param calendarSyncItems the calendar sync items on the device
     */
    fun fileListResult(calendarSyncItems: List<CalendarSyncItem>) {
        fileList.clear()
        fileList.addAll(calendarSyncItems.distinctBy { cis -> cis.baseName })

        presenter.cloudList(this@CalendarGoogleDriveSyncFragment, googleDrive!!)
    }

    /**
     * Cloud items list result.
     *
     * @param calendarSyncItems the calendar sync items in the cloud
     */
    fun cloudListResult(calendarSyncItems: List<CalendarSyncItem>) {
        cloudList.clear()
        cloudList.addAll(calendarSyncItems)

        updateListViews()
    }

    /**
     * Result of the JSON file load from filesystem.
     *
     * @param calendarSyncItem the calendar sync item
     */
    fun fileLoadResult(calendarSyncItem: CalendarSyncItem) {
        Timber.i("FileLoadResult: $calendarSyncItem")
        presenter.cloudUpdate(this@CalendarGoogleDriveSyncFragment, googleDrive!!, calendarSyncItem)
    }

    /**
     * Result of the JSON file load from cloud.
     *
     * @param calendarSyncItem the calendar sync item
     */
    fun cloudLoadResult(calendarSyncItem: CalendarSyncItem) {
        Timber.i("CloudLoadResult: $calendarSyncItem")
        presenter.fileUpdate(this@CalendarGoogleDriveSyncFragment, calendarSyncItem, binding)
    }

    /**
     * Result of the file update process.
     *
     * @param calendarSyncItem the calendar sync item
     */
    fun fileUpdateResult(calendarSyncItem: CalendarSyncItem) {
        Timber.i("$calendarSyncItem - $calendarSyncItem")
        firebaseAnalytics.logEvent("googleDriveSync") {
            param("method", "fileUpdateResult")
        }
        presenter.fileList(this@CalendarGoogleDriveSyncFragment, UUID.randomUUID(), binding)
    }

    /**
     * Result of the cloud update process.
     *
     * @param calendarSyncItem the calendar sync item
     *
     */
    fun cloudUpdateResult(calendarSyncItem: CalendarSyncItem) {
        Timber.i("$calendarSyncItem")
        firebaseAnalytics.logEvent("googleDriveSync") {
            param("method", "cloudUpdateResult")
        }
        presenter.fileList(this@CalendarGoogleDriveSyncFragment, UUID.randomUUID(), binding)
    }

    /**
     * Update the list view of sync items.
     */
    private fun updateListViews() {
        syncList.clear()
        syncList.addAll(presenter.calculateSyncList(fileList, cloudList))

        if (syncList.isEmpty()) {
            binding.syncListEmpty.visibility = View.VISIBLE
        } else {
            binding.syncListEmpty.visibility = View.GONE
        }

        syncList.sortByDescending { it.title.baseName }
        requireActivity().runOnUiThread {
            binding.syncListView.adapter?.notifyDataSetChanged()
        }
    }

    /**
     * Show the progress bar.
     */
    override fun showLoading() {
        progressDialog.setTitle("Working...")
        progressDialog.show()
        binding.mainProgress.visibility = View.VISIBLE
    }

    /**
     * Hide the progress bar.
     */
    override fun hideLoading() {
        progressDialog.dismiss()
        binding.mainProgress.visibility = View.INVISIBLE
    }
}
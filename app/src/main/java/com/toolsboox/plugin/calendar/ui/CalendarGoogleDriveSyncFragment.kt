package com.toolsboox.plugin.calendar.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.firebase.analytics.FirebaseAnalytics
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarGoogleDriveSyncBinding
import com.toolsboox.ot.StringArrayAdapter
import com.toolsboox.plugin.calendar.da.v1.CalendarItem
import com.toolsboox.plugin.calendar.da.v2.Calendar
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

    // Google sign-in options.
    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA), Scope(DriveScopes.DRIVE_FILE))
        .requestEmail()
        .build()

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarGoogleDriveSyncBinding

    /**
     * Flag of cloud list finished.
     */
    private var cloudListFinished = false

    /**
     * Flag of file list finished.
     */
    private var fileListFinished = false

    // Access token for Google Drive.
    private var googleAccount: GoogleSignInAccount? = null

    // Google sign-in client.
    private var signInClient: GoogleSignInClient? = null

    // Google Drive service.
    private var googleDriveService: Drive? = null

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

        sharedPreferences.getString("userId", null)?.let { userId ->
            requireActivity().runOnUiThread {
                presenter.fileList(this@CalendarGoogleDriveSyncFragment, UUID.fromString(userId), binding)
            }
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

        // Query the encrypted credential content from the cloud when compare button is clicked.
        binding.buttonCompare.setOnClickListener {
            if (googleAccount != null && googleDriveService != null) {
                presenter.cloudList(this@CalendarGoogleDriveSyncFragment, googleDriveService!!)
                presenter.fileList(this@CalendarGoogleDriveSyncFragment, UUID.randomUUID(), binding)
            }
        }

        // Set the to-cloud list adapter.
        binding.toCloudListView.setOnItemClickListener { parent, view, position, id ->
            if (toCloudList.isEmpty()) return@setOnItemClickListener
            Timber.i("Try to upload: %s", toCloudList[position])
            presenter.fileLoadJson(this@CalendarGoogleDriveSyncFragment, toCloudList[position], binding)
        }

        // Set the from-cloud list adapter.
        binding.fromCloudListView.setOnItemClickListener { parent, view, position, id ->
            if (fromCloudList.isEmpty()) return@setOnItemClickListener
            Timber.i("Try to download: %s", fromCloudList[position])
            // TODO: presenter method to download and update the calendar item
        }

        googleAccount = null
        binding.googleDriveStatusMessage.text = getString(R.string.calendar_google_drive_disconnected)
        binding.buttonCompare.isEnabled = false
        binding.buttonCompare.alpha = 0.5f

        // Check Google Drive connection.
        signInClient = GoogleSignIn.getClient(this.requireContext(), googleSignInOptions)
        signInClient!!.silentSignIn()
            .addOnSuccessListener { result ->
                Timber.i("Silent-signed into a GoogleAccount: $result.id")
                googleAccount = result
                requireActivity().runOnUiThread {
                    binding.googleDriveStatusMessage.text = getString(R.string.calendar_google_drive_connected)
                    binding.buttonCompare.isEnabled = true
                    binding.buttonCompare.alpha = 1.0f

                    val credential: GoogleAccountCredential = GoogleAccountCredential.usingOAuth2(
                        this.requireContext(), setOf(DriveScopes.DRIVE_APPDATA, DriveScopes.DRIVE_FILE)
                    )
                    credential.setSelectedAccount(googleAccount!!.account)
                    googleDriveService = Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        GsonFactory(),
                        credential
                    ).setApplicationName(R.string.app_name.toString()).build()
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
     * Cloud items list result.
     *
     * @param calendarItems the calendar items in the cloud
     */
    fun cloudListResult(calendarItems: List<CalendarItem>) {
        cloudList.clear()
        cloudList.addAll(calendarItems)

        cloudListFinished = true
        updateListViews()
    }

    /**
     * File items list result.
     *
     * @param calendarItems the calendar items on the device
     */
    fun fileListResult(calendarItems: List<CalendarItem>) {
        fileList.clear()
        fileList.addAll(calendarItems.distinctBy { cis -> cis.baseName })

        fileListFinished = true
        updateListViews()
    }

    /**
     * Result of the JSON file load.
     *
     * @param calendarItem the calendar item
     * @param json the JSON of the calendar item
     */
    fun fileLoadJsonResult(calendarItem: CalendarItem, json: String) {
        Timber.i("Data to encrypt: $json")
        presenter.cloudUpdate(this@CalendarGoogleDriveSyncFragment, googleDriveService!!, calendarItem)
    }

    /**
     * Result of the cloud update process.
     *
     * @param calendarItem the calendar item
     *
     */
    fun cloudUpdateResult(calendarItem: CalendarItem) {
        Timber.i("$calendarItem")
        presenter.fileUpdate(this@CalendarGoogleDriveSyncFragment, calendarItem, binding)
    }

    fun fileUpdateResult(calendarItem: CalendarItem, calendar: Calendar) {
        Timber.i("$calendarItem - $calendar")

        sharedPreferences.getString("userId", null)?.let { userId ->
            requireActivity().runOnUiThread {
                presenter.cloudList(this@CalendarGoogleDriveSyncFragment, googleDriveService!!)
                presenter.fileList(this@CalendarGoogleDriveSyncFragment, UUID.fromString(userId), binding)
            }
        }
    }

    /**
     * Update the list view of sync items.
     */
    private fun updateListViews() {
        if (!cloudListFinished or !fileListFinished) return

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

            // If the file is in the cloud, checks the updated date.
            if ((cloudItem.updated != null) and (cloudItem.updated!! < fci.updated)) {
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
            ) { i -> "${i.path}${i.baseName}.${i.version}" }
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
                if (fileItem.updated < cci.updated) {
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
            ) { i -> "${i.path}${i.baseName}.${i.version}" }
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

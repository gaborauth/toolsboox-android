package com.toolsboox.plugin.calendar.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.api.services.drive.Drive
import com.google.firebase.analytics.FirebaseAnalytics
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarGoogleDriveSyncBinding
import com.toolsboox.di.GoogleDriveModule
import com.toolsboox.plugin.calendar.da.v1.CalendarSyncItem
import com.toolsboox.ui.plugin.ScreenFragment
import com.toolsboox.utils.FluentDuration
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.time.Instant
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
    private val syncList: MutableList<ViewHolderItem> = mutableListOf()

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

        // Set the sync list adapter.
        val syncViewAdapter = SyncViewAdapter(requireContext(), syncList, { item ->
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
        // TODO: Remove the item from the sync list.
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
        // TODO: Remove the item from the sync list.
        presenter.fileList(this@CalendarGoogleDriveSyncFragment, UUID.randomUUID(), binding)
    }

    /**
     * Update the list view of sync items.
     */
    private fun updateListViews() {
        syncList.clear()
        fileList.forEach { fci ->
            // If the file is not in the cloud, add it.
            if (fci.updated == null) {
                syncList.add(ViewHolderItem(fci, null))
                return@forEach
            }

            // If the file is in the cloud, checks the path, the baseName and the version.
            val cloudItem = cloudList
                .filter { it.path == fci.path }
                .filter { it.baseName == fci.baseName }
                .firstOrNull { it.version == fci.version }

            // If the file is not in the cloud, add it.
            if (cloudItem == null) {
                syncList.add(ViewHolderItem(fci, null))
                return@forEach
            }

            if ((cloudItem.updated != null) and (cloudItem.updated!!.time < fci.updated.time)) {
                syncList.add(ViewHolderItem(fci, cloudItem))
                return@forEach
            }
        }

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
                syncList.add(ViewHolderItem(null, cci))
            } else if (fileItem.updated != null) {
                if (fileItem.updated.time < cci.updated.time) {
                    syncList.add(ViewHolderItem(fileItem, cci))
                }
            }
        }

        if (syncList.isEmpty()) {
            binding.syncListEmpty.visibility = View.VISIBLE
        } else {
            binding.syncListEmpty.visibility = View.GONE
        }

        requireActivity().runOnUiThread {
            binding.syncListView.adapter?.notifyDataSetChanged()
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

    data class ViewHolderItem(
        val file: CalendarSyncItem?,
        val cloud: CalendarSyncItem?
    )

    internal class SyncViewAdapter(
        private val context: Context,
        private val items: List<ViewHolderItem>,
        private val onItemClick: ((ViewHolderItem) -> Unit)
    ) : RecyclerView.Adapter<SyncViewAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_calendar_sync, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindTo(items[position])
        }

        override fun getItemCount(): Int = items.size

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTextView = itemView.findViewById<TextView>(R.id.calendar_sync_name)
            private val syncButton = itemView.findViewById<TextView>(R.id.calendar_sync_button)
            private val fileTextView = itemView.findViewById<TextView>(R.id.calendar_sync_file_last_modification)
            private val cloudTextView = itemView.findViewById<TextView>(R.id.calendar_sync_cloud_last_modification)

            fun bindTo(item: ViewHolderItem) {
                val titleItem = item.file ?: item.cloud ?: return
                nameTextView.text = titleItem.baseName

                val fileLastModified = item.file?.updated?.time ?: 0L
                val fileLastModifiedText = FluentDuration.convert(itemView.context, Instant.now().toEpochMilli() - fileLastModified)
                if (fileLastModified == 0L) {
                    fileTextView.text = context.getString(R.string.calendar_google_drive_sync_file_missing)
                } else {
                    fileTextView.text = context.getString(R.string.calendar_google_drive_sync_file_last_modification, fileLastModifiedText)
                }

                val cloudLastModified = item.cloud?.updated?.time ?: 0L
                val cloudLastModifiedText = FluentDuration.convert(itemView.context, Instant.now().toEpochMilli() - cloudLastModified)
                if (cloudLastModified == 0L) {
                    cloudTextView.text = context.getString(R.string.calendar_google_drive_sync_cloud_missing)
                } else {
                    cloudTextView.text = context.getString(R.string.calendar_google_drive_sync_cloud_last_modification, cloudLastModifiedText)
                }

                if (fileLastModified < cloudLastModified) {
                    syncButton.text = context.getString(R.string.calendar_google_drive_sync_button_download)
                } else {
                    syncButton.text = context.getString(R.string.calendar_google_drive_sync_button_upload)
                }

                syncButton.setOnClickListener {
                    onItemClick.invoke(item)
                }
            }
        }
    }
}

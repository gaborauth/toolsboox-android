package com.toolsboox.plugin.calendar.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarCloudSyncBinding
import com.toolsboox.ot.CryptoUtils
import com.toolsboox.ot.StringArrayAdapter
import com.toolsboox.plugin.calendar.da.v1.CalendarItem
import com.toolsboox.plugin.calendar.da.v2.Calendar
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Calendar cloud sync fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarCloudSyncFragment @Inject constructor() : ScreenFragment() {

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
    lateinit var presenter: CalendarCloudSyncPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_calendar_cloud_sync

    /**
     * Maximum number of items to sync.
     */
    private val maximumSyncItems = 1

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
    private lateinit var binding: FragmentCalendarCloudSyncBinding

    /**
     * The selected passphrase.
     */
    private var passphrase: String? = null

    /**
     * Flag of cloud list finished.
     */
    private var cloudListFinished = false

    /**
     * Flag of file list finished.
     */
    private var fileListFinished = false

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarCloudSyncBinding.bind(view)

        toolbar.toolbarPager.visibility = View.GONE

        sharedPreferences.getString("userId", null)?.let { userId ->
            requireActivity().runOnUiThread {
                presenter.cloudList(this@CalendarCloudSyncFragment)
                presenter.fileList(this@CalendarCloudSyncFragment, UUID.fromString(userId), binding)
            }
        }

        binding.buttonSync.isEnabled = false
        binding.buttonSync.alpha = 0.5f
        binding.passphraseEditText.addTextChangedListener {
            val password = binding.passphraseEditText.text.toString()
            binding.buttonSync.isEnabled = password.length >= 8
            binding.buttonSync.alpha = if (password.length >= 8) 1.0f else 0.5f
        }

        binding.toCloudText.text = resources.getQuantityString(R.plurals.calendar_cloud_to_cloud_text, maximumSyncItems).format(maximumSyncItems)
        binding.toCloudListView.emptyView = binding.toCloudListEmpty
        binding.fromCloudText.text = resources.getQuantityString(R.plurals.calendar_cloud_from_cloud_text, maximumSyncItems).format(maximumSyncItems)
        binding.fromCloudListView.emptyView = binding.fromCloudListEmpty
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        // Sync the calendar
        binding.buttonSync.setOnClickListener {
            if (toCloudList.isEmpty()) return@setOnClickListener

            Timber.i("Try to upload: %s", toCloudList.first())
            presenter.fileLoadJson(this@CalendarCloudSyncFragment, toCloudList.first(), binding)
        }

        passphrase?.let {
            binding.passphraseEditText.setText(it)
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
        val passphrase = binding.passphraseEditText.text.toString()
        val encrypted = CryptoUtils.encrypt(json.toByteArray(Charsets.UTF_8), passphrase)
        val encryptedBase64 = Base64.getEncoder().encodeToString(encrypted)
        presenter.cloudUpdate(this@CalendarCloudSyncFragment, calendarItem, encryptedBase64)
    }

    /**
     * Result of the cloud update process.
     *
     * @param calendarItem the calendar item
     *
     */
    fun cloudUpdateResult(calendarItem: CalendarItem) {
        Timber.i("$calendarItem")
        presenter.fileUpdate(this@CalendarCloudSyncFragment, calendarItem, binding)
    }

    fun fileUpdateResult(calendarItem: CalendarItem, calendar: Calendar) {
        Timber.i("$calendarItem - $calendar")

        sharedPreferences.getString("userId", null)?.let { userId ->
            requireActivity().runOnUiThread {
                presenter.cloudList(this@CalendarCloudSyncFragment)
                presenter.fileList(this@CalendarCloudSyncFragment, UUID.fromString(userId), binding)
            }
        }
    }

    /**
     * Update the list view of sync.
     */
    private fun updateListViews() {
        if (!cloudListFinished or !fileListFinished) return

        toCloudList.clear()
        fileList.forEach { fci ->
            if (fci.updated == null) {
                toCloudList.add(fci)
                return@forEach
            }

            val cloudItem = cloudList
                .filter { it.baseName == fci.baseName }
                .filter { it.version == fci.version }
                .filter { it.updated != null }
                .firstOrNull { it.updated!! < fci.updated }

            if (cloudItem != null) {
                toCloudList.add(fci)
            }
        }

        toCloudList.sortWith { ci1, ci2 -> ci1.baseName.compareTo(ci2.baseName) }
        Timber.i("To cloud list: $toCloudList")
        requireActivity().runOnUiThread {
            val toCloudAdapter = StringArrayAdapter(this.requireContext(), R.layout.list_item_locale, toCloudList) { i -> i.baseName }
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

        fromCloudList.sortWith { ci1, ci2 -> ci1.baseName.compareTo(ci2.baseName) }
        Timber.i("From cloud list: $fromCloudList")
        requireActivity().runOnUiThread {
            val fromCloudAdapter = StringArrayAdapter(this.requireContext(), R.layout.list_item_locale, fromCloudList) { i -> i.baseName }
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

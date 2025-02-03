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
import com.toolsboox.plugin.calendar.da.v1.CalendarSyncItem
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
     * Stored items in the cloud.
     */
    private val cloudList: MutableList<CalendarSyncItem> = mutableListOf()

    /**
     * Stored items in the device.
     */
    private val fileList: MutableList<CalendarSyncItem> = mutableListOf()

    /**
     * Calendar items selected to sync from cloud.
     */
    private val fromCloudList: MutableList<CalendarSyncItem> = mutableListOf()

    /**
     * Calendar items selected to sync to cloud.
     */
    private val toCloudList: MutableList<CalendarSyncItem> = mutableListOf()

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarCloudSyncBinding

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
                presenter.fileList(this@CalendarCloudSyncFragment, UUID.fromString(userId), binding)
            }
        }

        binding.buttonCompare.isEnabled = false
        binding.buttonCompare.alpha = 0.5f
        binding.passphraseEditText.addTextChangedListener {
            val password = binding.passphraseEditText.text.toString()
            binding.buttonCompare.isEnabled = password.length >= 8
            binding.buttonCompare.alpha = if (password.length >= 8) 1.0f else 0.5f
        }

        binding.toCloudText.text = resources.getString(R.string.calendar_cloud_to_cloud_text)
        binding.toCloudListView.emptyView = binding.toCloudListEmpty
        binding.fromCloudText.text = resources.getString(R.string.calendar_cloud_from_cloud_text)
        binding.fromCloudListView.emptyView = binding.fromCloudListEmpty
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        // Query the encrypted credential content from the cloud when compare button is clicked.
        binding.buttonCompare.setOnClickListener {
            presenter.authenticateGet(this@CalendarCloudSyncFragment)

            // Save the remember passphrase checkbox state.
            if (binding.rememberPassphraseCheckbox.isChecked) {
                sharedPreferences.edit().putBoolean("calendarItemRememberPassphrase", true).apply()
            } else {
                sharedPreferences.edit().remove("calendarItemRememberPassphrase").apply()
            }
        }

        // Set the remember passphrase checkbox state.
        binding.rememberPassphraseCheckbox.isChecked = sharedPreferences.getBoolean("calendarItemRememberPassphrase", false)

        // Set the to-cloud list adapter.
        binding.toCloudListView.setOnItemClickListener { parent, view, position, id ->
            if (toCloudList.isEmpty()) return@setOnItemClickListener
            Timber.i("Try to upload: %s", toCloudList[position])
            presenter.fileLoadJson(this@CalendarCloudSyncFragment, toCloudList[position], binding)
        }

        // Set the from-cloud list adapter.
        binding.fromCloudListView.setOnItemClickListener { parent, view, position, id ->
            if (fromCloudList.isEmpty()) return@setOnItemClickListener
            Timber.i("Try to download: %s", fromCloudList[position])
            // TODO: presenter method to download and update the calendar item
        }

        // Prefill the passphrase edit text if it is remembered.
        sharedPreferences.getString("calendarItemPassphrase", null)?.let {
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
     * Result of the authenticate GET method.
     *
     * @param data the encrypted credential content
     */
    fun authenticateGetResult(data: String) {
        try {
            val decrypted = CryptoUtils.decrypt(Base64.getDecoder().decode(data), binding.passphraseEditText.text.toString())
            if (String(decrypted) == "data") {
                binding.passphraseEditText.error = null
                presenter.cloudList(this@CalendarCloudSyncFragment)
                if (binding.rememberPassphraseCheckbox.isChecked) {
                    sharedPreferences.edit().putString("calendarItemPassphrase", binding.passphraseEditText.text.toString()).apply()
                } else {
                    sharedPreferences.edit().remove("calendarItemPassphrase").apply()
                }
            } else {
                binding.passphraseEditText.error = getString(R.string.calendar_cloud_passphrase_mismatch)
            }
        } catch (e: Exception) {
            binding.passphraseEditText.error = getString(R.string.calendar_cloud_passphrase_mismatch)
        }
    }

    /**
     * No-content result of the authenticate GET method.
     */
    fun authenticateGetNoContent() {
        val encrypted = CryptoUtils.encrypt("data".toByteArray(), binding.passphraseEditText.text.toString())
        val encryptedBase64 = Base64.getEncoder().encodeToString(encrypted)
        presenter.authenticatePost(this@CalendarCloudSyncFragment, encryptedBase64)
    }

    /**
     * Result of the authenticate POST method.
     */
    fun authenticatePostResult() {
        presenter.cloudList(this@CalendarCloudSyncFragment)
    }

    /**
     * Cloud sync items list result.
     *
     * @param calendarSyncItems the calendar sync items in the cloud
     */
    fun cloudListResult(calendarSyncItems: List<CalendarSyncItem>) {
        cloudList.clear()
        cloudList.addAll(calendarSyncItems)

        cloudListFinished = true
        updateListViews()
    }

    /**
     * File sync items list result.
     *
     * @param calendarSyncItems the calendar sync items on the device
     */
    fun fileListResult(calendarSyncItems: List<CalendarSyncItem>) {
        fileList.clear()
        fileList.addAll(calendarSyncItems.distinctBy { cis -> cis.baseName })

        fileListFinished = true
        updateListViews()
    }

    /**
     * Result of the JSON file load.
     *
     * @param calendarSyncItem the calendar sync item
     * @param json the JSON of the calendar item
     */
    fun fileLoadJsonResult(calendarSyncItem: CalendarSyncItem, json: String) {
        Timber.i("Data to encrypt: $json")
        val passphrase = binding.passphraseEditText.text.toString()
        val encrypted = CryptoUtils.encrypt(json.toByteArray(Charsets.UTF_8), passphrase)
        val encryptedBase64 = Base64.getEncoder().encodeToString(encrypted)
        presenter.cloudUpdate(this@CalendarCloudSyncFragment, calendarSyncItem, encryptedBase64)
    }

    /**
     * Result of the cloud update process.
     *
     * @param calendarSyncItem the calendar sync item
     *
     */
    fun cloudUpdateResult(calendarSyncItem: CalendarSyncItem) {
        Timber.i("$calendarSyncItem")
        presenter.fileUpdate(this@CalendarCloudSyncFragment, calendarSyncItem, binding)
    }

    fun fileUpdateResult(calendarItem: CalendarSyncItem, calendar: Calendar) {
        Timber.i("$calendarItem - $calendar")

        sharedPreferences.getString("userId", null)?.let { userId ->
            requireActivity().runOnUiThread {
                presenter.cloudList(this@CalendarCloudSyncFragment)
                presenter.fileList(this@CalendarCloudSyncFragment, UUID.fromString(userId), binding)
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

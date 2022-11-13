package com.toolsboox.plugin.calendar.ui

import android.os.Environment
import android.widget.Toast
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarSettingsBinding
import com.toolsboox.ot.ZipManager
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Calendar settings presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarSettingsPresenter @Inject constructor() : FragmentPresenter() {
    /**
     * Export the calendar to the storage.
     *
     * @param fragment the fragment
     * @param binding the data binding
     */
    fun export(fragment: CalendarSettingsFragment, binding: FragmentCalendarSettingsBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    ZipManager.zip(File(rootPath, "calendar"), File(downloads, "toolsBoox-calendar-backup.zip"))

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            fragment.requireContext(), R.string.calendar_settings_backup_done, Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }
}
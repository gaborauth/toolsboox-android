package com.toolsboox.plugin.templates.ui

import android.Manifest
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.toolsboox.R
import com.toolsboox.databinding.FragmentTemplatesFlatWeeksCalendarBinding
import com.toolsboox.plugin.templates.ot.FlatWeekCalendarCreator
import com.toolsboox.ui.plugin.FragmentPresenter
import com.toolsboox.ui.plugin.ScreenFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Templates 'flat week's calendar' presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class FlatWeeksCalendarPresenter @Inject constructor() : FragmentPresenter() {
    /**
     * Export the calendar as PDF.
     *
     * @param fragment the fragment
     * @param binding the data binding
     */
    fun export(fragment: FlatWeeksCalendarFragment, binding: FragmentTemplatesFlatWeeksCalendarBinding) {
        if (!fragment.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            fragment.showError(null, R.string.main_read_external_storage_permission_missing, binding.root)
            return
        }

        if (!fragment.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            fragment.showError(null, R.string.main_write_external_storage_permission_missing, binding.root)
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val doc = PdfDocument()
                for (p in 0L..52L) {
                    val pageInfo = PdfDocument.PageInfo.Builder(1404, 1872, p.toInt() + 1).create()
                    val page = doc.startPage(pageInfo)
                    FlatWeekCalendarCreator.drawPage(
                        fragment.requireContext(),
                        page.canvas,
                        p,
                        0.5f,
                        binding.settingsWithDays.isChecked
                    )
                    doc.finishPage(page)
                }

                try {
                    val file = createPath(fragment, LocalDate.now())
                    FileOutputStream(file).use { out -> doc.writeTo(out) }
                    withContext(Dispatchers.Main) {
                        binding.exportMessage.text = fragment.getString(
                            R.string.templates_flat_weeks_calendar_preview_export_message, file.absoluteFile
                        )
                    }
                    doc.close()
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { binding.exportMessage.text = e.toString() }
                }

            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Create path of the files.
     *
     * @param fragment the fragment
     * @param currentDate the current date
     * @return the path on the filesystem
     */
    private fun createPath(fragment: ScreenFragment, currentDate: LocalDate): File {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))

        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOWNLOADS)
        val path = File(rootPath, "noteTemplate")
        path.mkdirs()

        return File(path, "flat-weeks-calendar-$year.pdf")
    }
}

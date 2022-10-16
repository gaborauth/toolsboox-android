package com.toolsboox.plugin.templates.ui

import android.Manifest
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.toolsboox.R
import com.toolsboox.databinding.FragmentTemplatesBoxedWeeksCalendarBinding
import com.toolsboox.plugin.templates.ot.BoxedWeekCalendarCreator
import com.toolsboox.ui.plugin.FragmentPresenter
import com.toolsboox.ui.plugin.ScreenFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

/**
 * Templates 'boxed week's calendar' presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class BoxedWeeksCalendarPresenter @Inject constructor() : FragmentPresenter() {
    /**
     * Export the calendar as PDF.
     *
     * @param fragment the fragment
     * @param binding the data binding
     */
    fun export(fragment: BoxedWeeksCalendarFragment, binding: FragmentTemplatesBoxedWeeksCalendarBinding) {
        val readPermissionGranted = fragment.checkPermissionGranted(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            ScreenFragment.REQUEST_PERMISSION_READ_EXTERNAL_STORAGE,
            fragment.getString(R.string.main_read_external_storage_permission_title),
            fragment.getString(R.string.main_read_external_storage_permission_message)
        )
        val writePermissionGranted = fragment.checkPermissionGranted(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ScreenFragment.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
            fragment.getString(R.string.main_write_external_storage_permission_title),
            fragment.getString(R.string.main_write_external_storage_permission_message)
        )

        if (!readPermissionGranted) {
            fragment.showError(null, R.string.main_read_external_storage_permission_missing, binding.root)
            return
        }
        if (!writePermissionGranted) {
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
                    BoxedWeekCalendarCreator.drawPage(
                        fragment.requireContext(),
                        page.canvas,
                        p,
                        binding.settingsVerticalDays.isChecked
                    )
                    doc.finishPage(page)
                }

                val rootPath = Environment.getExternalStorageDirectory()
                val title = "boxed-weeks-calendar-${LocalDate.now().year}.pdf"
                val filename = "$rootPath/noteTemplate/$title"
                try {
                    FileOutputStream(filename).use { out -> doc.writeTo(out) }
                    withContext(Dispatchers.Main) {
                        binding.exportMessage.text =
                            fragment.getString(R.string.templates_boxed_weeks_calendar_preview_export_message, title)
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
}

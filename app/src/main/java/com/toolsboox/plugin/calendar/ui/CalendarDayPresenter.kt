package com.toolsboox.plugin.calendar.ui

import android.Manifest
import android.graphics.Rect
import android.os.Environment
import com.google.gson.GsonBuilder
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarDayBinding
import com.toolsboox.plugin.calendar.da.CalendarDay
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

/**
 * Calendar day presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarDayPresenter @Inject constructor() : FragmentPresenter() {
    /**
     * Load the day if available.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     * @param locale the locale
     */
    fun load(
        fragment: CalendarDayFragment, binding: FragmentCalendarDayBinding,
        currentDate: LocalDate, surfaceSize: Rect
    ) {
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

                val year = currentDate.year
                val month = currentDate.monthValue
                val day = currentDate.dayOfMonth
                var calendarDay = CalendarDay(year, month, day, Locale.getDefault(), mutableListOf())

                try {
                    if (File(createPath(currentDate)).exists()) {
                        FileReader(File(createPath(currentDate))).use {
                            calendarDay = GsonBuilder().create().fromJson(it, CalendarDay::class.java)
                            calendarDay.normalizeStrokes(1404, 1872, surfaceSize.width(), surfaceSize.height())
                        }
                    }

                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }

                withContext(Dispatchers.Main) { fragment.renderPage(calendarDay) }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Save the day to the storage.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param calendarDay the data class
     * @param currentDate the current date
     */
    fun save(
        fragment: CalendarDayFragment, binding: FragmentCalendarDayBinding,
        calendarDay: CalendarDay, currentDate: LocalDate
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    PrintWriter(FileWriter(File(createPath(currentDate)))).use {
                        it.write(GsonBuilder().create().toJson(calendarDay).toString())
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Create path of the files.
     *
     * @param currentDate the current date
     * @return the path on the filesystem
     */
    private fun createPath(currentDate: LocalDate): String {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val month = currentDate.format(DateTimeFormatter.ofPattern("MM"))
        val day = currentDate.format(DateTimeFormatter.ofPattern("dd"))

        val rootPath = Environment.getExternalStorageDirectory()
        File("$rootPath/toolsBoox/calendar/$year/$month/").mkdirs()
        return "$rootPath/toolsBoox/calendar/$year/$month/day-$year-$month-$day.json"
    }
}

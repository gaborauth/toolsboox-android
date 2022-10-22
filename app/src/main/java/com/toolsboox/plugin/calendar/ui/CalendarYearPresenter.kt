package com.toolsboox.plugin.calendar.ui

import android.graphics.Rect
import android.os.Environment
import com.google.gson.GsonBuilder
import com.toolsboox.databinding.FragmentCalendarYearBinding
import com.toolsboox.plugin.calendar.da.CalendarYear
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.*
import javax.inject.Inject

/**
 * Calendar main presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarYearPresenter @Inject constructor() : FragmentPresenter() {
    /**
     * Load the year if available.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     */
    fun load(
        fragment: CalendarYearFragment, binding: FragmentCalendarYearBinding,
        currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val year = currentDate.get(ChronoField.YEAR)
                var calendarYear = CalendarYear(year, Locale.getDefault(), mutableListOf())

                val rootPath = Environment.getExternalStorageDirectory()
                File("$rootPath/toolsBoox/").mkdirs()
                val filename = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
                val path = "$rootPath/toolsBoox/year-$filename.json"
                try {
                    if (File(path).exists()) {
                        FileReader(File(path)).use {
                            calendarYear = GsonBuilder().create().fromJson(it, CalendarYear::class.java)
                            calendarYear.normalizeStrokes(1404, 1872, surfaceSize.width(), surfaceSize.height())
                        }
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }

                withContext(Dispatchers.Main) { fragment.renderPage(calendarYear) }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Save the year to the storage.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param calendarYear the data class
     * @param currentDate the current date
     */
    fun save(
        fragment: CalendarYearFragment, binding: FragmentCalendarYearBinding,
        calendarYear: CalendarYear, currentDate: LocalDate
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val rootPath = Environment.getExternalStorageDirectory()
                File("$rootPath/toolsBoox/").mkdirs()
                val filename = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
                val path = "$rootPath/toolsBoox/year-$filename.json"
                try {
                    PrintWriter(FileWriter(File(path))).use {
                        it.write(GsonBuilder().create().toJson(calendarYear).toString())
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

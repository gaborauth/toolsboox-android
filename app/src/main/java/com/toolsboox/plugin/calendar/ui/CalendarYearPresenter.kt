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
import java.util.*
import javax.inject.Inject

/**
 * Calendar year presenter.
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

                val year = currentDate.year
                var calendarYear = CalendarYear(year, Locale.getDefault(), mutableListOf())

                try {
                    if (File(createPath(currentDate)).exists()) {
                        FileReader(File(createPath(currentDate))).use {
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

                try {
                    PrintWriter(FileWriter(File(createPath(currentDate)))).use {
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

    /**
     * Create path of the files.
     *
     * @param currentDate the current date
     * @return the path on the filesystem
     */
    private fun createPath(currentDate: LocalDate): String {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))

        val rootPath = Environment.getExternalStorageDirectory()
        File("$rootPath/toolsBoox/calendar/$year/").mkdirs()
        return "$rootPath/toolsBoox/calendar/$year/year-$year.json"
    }
}

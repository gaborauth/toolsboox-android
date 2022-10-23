package com.toolsboox.plugin.calendar.ui

import android.graphics.Rect
import android.os.Environment
import com.google.gson.GsonBuilder
import com.toolsboox.databinding.FragmentCalendarMonthBinding
import com.toolsboox.plugin.calendar.da.CalendarMonth
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
 * Calendar month presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarMonthPresenter @Inject constructor() : FragmentPresenter() {
    /**
     * Load the month if available.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     */
    fun load(
        fragment: CalendarMonthFragment, binding: FragmentCalendarMonthBinding,
        currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val year = currentDate.year
                val month = currentDate.monthValue
                var calendarMonth = CalendarMonth(year, month, Locale.getDefault(), mutableListOf())

                try {
                    if (File(createPath(currentDate)).exists()) {
                        FileReader(File(createPath(currentDate))).use {
                            calendarMonth = GsonBuilder().create().fromJson(it, CalendarMonth::class.java)
                            calendarMonth.normalizeStrokes(1404, 1872, surfaceSize.width(), surfaceSize.height())
                        }
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }

                withContext(Dispatchers.Main) { fragment.renderPage(calendarMonth) }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Save the month to the storage.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param calendarMonth the data class
     * @param currentDate the current date
     */
    fun save(
        fragment: CalendarMonthFragment, binding: FragmentCalendarMonthBinding,
        calendarMonth: CalendarMonth, currentDate: LocalDate
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    PrintWriter(FileWriter(File(createPath(currentDate)))).use {
                        it.write(GsonBuilder().create().toJson(calendarMonth).toString())
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

        val rootPath = Environment.getExternalStorageDirectory()
        File("$rootPath/toolsBoox/calendar/$year/$month/").mkdirs()
        return "$rootPath/toolsBoox/calendar/$year/$month/month-$year-$month.json"
    }
}

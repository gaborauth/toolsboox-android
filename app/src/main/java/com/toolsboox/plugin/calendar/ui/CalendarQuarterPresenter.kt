package com.toolsboox.plugin.calendar.ui

import android.graphics.Rect
import android.os.Environment
import com.google.gson.GsonBuilder
import com.toolsboox.databinding.FragmentCalendarQuarterBinding
import com.toolsboox.plugin.calendar.da.CalendarQuarter
import com.toolsboox.ui.plugin.FragmentPresenter
import com.toolsboox.ui.plugin.ScreenFragment
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
 * Calendar quarter presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarQuarterPresenter @Inject constructor() : FragmentPresenter() {
    /**
     * Load the quarter if available.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     */
    fun load(
        fragment: CalendarQuarterFragment, binding: FragmentCalendarQuarterBinding,
        currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val year = currentDate.year
                val quarter = (currentDate.monthValue - 1) / 3 + 1
                var calendarQuarter = CalendarQuarter(year, quarter, Locale.getDefault(), mutableListOf())

                try {
                    if (createPath(fragment, currentDate).exists()) {
                        FileReader(createPath(fragment, currentDate)).use {
                            calendarQuarter = GsonBuilder().create().fromJson(it, CalendarQuarter::class.java)
                            calendarQuarter.normalizeStrokes(1404, 1872, surfaceSize.width(), surfaceSize.height())
                        }
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }

                withContext(Dispatchers.Main) { fragment.renderPage(calendarQuarter) }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Save the quarter to the storage.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param calendarQuarter the data class
     * @param currentDate the current date
     */
    fun save(
        fragment: CalendarQuarterFragment, binding: FragmentCalendarQuarterBinding,
        calendarQuarter: CalendarQuarter, currentDate: LocalDate
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    PrintWriter(FileWriter(createPath(fragment, currentDate))).use {
                        it.write(GsonBuilder().create().toJson(calendarQuarter).toString())
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
     * @param fragment the fragment
     * @param currentDate the current date
     * @return the path on the filesystem
     */
    private fun createPath(fragment: ScreenFragment, currentDate: LocalDate): File {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val quarter = currentDate.format(DateTimeFormatter.ofPattern("QQ"))

        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
        val path = File(rootPath, "calendar/$year/")
        path.mkdirs()

        return File(path, "quarter-$year-$quarter.json")
    }
}

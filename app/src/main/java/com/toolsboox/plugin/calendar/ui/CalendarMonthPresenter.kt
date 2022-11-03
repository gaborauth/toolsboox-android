package com.toolsboox.plugin.calendar.ui

import android.graphics.Rect
import android.os.Environment
import com.squareup.moshi.Moshi
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.plugin.calendar.da.CalendarMonth
import com.toolsboox.ui.plugin.FragmentPresenter
import com.toolsboox.ui.plugin.ScreenFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Calendar month presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarMonthPresenter @Inject constructor() : FragmentPresenter() {

    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

    /**
     * Load the month if available.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     */
    fun load(
        fragment: CalendarMonthFragment, binding: FragmentCalendarBinding,
        currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val year = currentDate.year
                val month = currentDate.monthValue
                var calendarMonth = CalendarMonth(year, month)

                try {
                    val adapter = moshi.adapter(CalendarMonth::class.java)
                    if (getPath(fragment, currentDate).exists()) {
                        FileReader(getPath(fragment, currentDate)).use { fileReader ->
                            adapter.fromJson(fileReader.readText())?.let {
                                it.normalizeStrokes(1404, 1872, surfaceSize.width(), surfaceSize.height())
                                calendarMonth = it
                            }
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
     * @param surfaceSize the actual size of surface view
     */
    fun save(
        fragment: CalendarMonthFragment, binding: FragmentCalendarBinding,
        calendarMonth: CalendarMonth, currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val calendarMonthCopy = calendarMonth.deepCopy()
                calendarMonthCopy.normalizeStrokes(surfaceSize.width(), surfaceSize.height(), 1404, 1872)

                try {
                    val adapter = moshi.adapter(CalendarMonth::class.java)
                    PrintWriter(FileWriter(getPath(fragment, currentDate, true))).use {
                        it.write(adapter.toJson(calendarMonthCopy))
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
     * Get path of the files.
     *
     * @param fragment the fragment
     * @param currentDate the current date
     * @param create create folders
     * @return the path on the filesystem
     */
    private fun getPath(fragment: ScreenFragment, currentDate: LocalDate, create: Boolean = false): File {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val month = currentDate.format(DateTimeFormatter.ofPattern("MM"))

        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
        val path = File(rootPath, "calendar/$year/$month/")
        if (create) path.mkdirs()

        return File(path, "month-$year-$month.json")
    }
}

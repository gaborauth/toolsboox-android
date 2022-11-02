package com.toolsboox.plugin.calendar.ui

import android.graphics.Rect
import android.os.Environment
import com.squareup.moshi.Moshi
import com.toolsboox.databinding.FragmentCalendarBinding
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
import javax.inject.Inject

/**
 * Calendar quarter presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarQuarterPresenter @Inject constructor() : FragmentPresenter() {

    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

    /**
     * Load the quarter if available.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     */
    fun load(
        fragment: CalendarQuarterFragment, binding: FragmentCalendarBinding,
        currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val year = currentDate.year
                val quarter = (currentDate.monthValue - 1) / 3 + 1
                var calendarQuarter = CalendarQuarter(year, quarter)

                try {
                    val adapter = moshi.adapter(CalendarQuarter::class.java)
                    if (createPath(fragment, currentDate).exists()) {
                        FileReader(createPath(fragment, currentDate)).use { fileReader ->
                            adapter.fromJson(fileReader.readText())?.let {
                                it.normalizeStrokes(1404, 1872, surfaceSize.width(), surfaceSize.height())
                                calendarQuarter = it
                            }
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
     * @param surfaceSize the actual size of surface view
     */
    fun save(
        fragment: CalendarQuarterFragment, binding: FragmentCalendarBinding,
        calendarQuarter: CalendarQuarter, currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val calendarQuarterCopy = calendarQuarter.deepCopy()
                calendarQuarterCopy.normalizeStrokes(surfaceSize.width(), surfaceSize.height(), 1404, 1872)

                try {
                    val adapter = moshi.adapter(CalendarQuarter::class.java)
                    PrintWriter(FileWriter(createPath(fragment, currentDate))).use {
                        it.write(adapter.toJson(calendarQuarterCopy))
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

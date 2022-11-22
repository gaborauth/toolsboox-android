package com.toolsboox.plugin.calendar.ui

import android.graphics.Rect
import android.os.Environment
import com.squareup.moshi.Moshi
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v1.CalendarQuarter
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
     * @param locale the default locale
     */
    fun load(
        fragment: CalendarQuarterFragment, binding: FragmentCalendarBinding,
        currentDate: LocalDate, surfaceSize: Rect, locale: Locale
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val year = currentDate.year
                val quarter = (currentDate.monthValue - 1) / 3 + 1
                var calendarQuarter = CalendarQuarter(year, quarter, locale)

                try {
                    val adapter = moshi.adapter(CalendarQuarter::class.java)
                    if (getPath(fragment, currentDate).exists()) {
                        FileReader(getPath(fragment, currentDate)).use { fileReader ->
                            adapter.fromJson(fileReader.readText())?.let {
                                it.normalizeStrokes(1404, 1872, surfaceSize.width(), surfaceSize.height())
                                calendarQuarter = it
                            }
                        }
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }

                var calendarPattern = CalendarPattern(year, locale).fill()
                try {
                    val adapter = moshi.adapter(CalendarPattern::class.java)
                    if (getPatternPath(fragment, currentDate).exists()) {
                        FileReader(getPatternPath(fragment, currentDate)).use { fileReader ->
                            adapter.fromJson(fileReader.readText())?.let {
                                calendarPattern = it
                            }
                        }
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }

                withContext(Dispatchers.Main) { fragment.renderPage(calendarQuarter, calendarPattern) }
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
     * @param calendarPattern the pattern data class
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     */
    fun save(
        fragment: CalendarQuarterFragment, binding: FragmentCalendarBinding,
        calendarQuarter: CalendarQuarter, calendarPattern: CalendarPattern, currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val calendarQuarterCopy = calendarQuarter.deepCopy()
                calendarQuarterCopy.normalizeStrokes(surfaceSize.width(), surfaceSize.height(), 1404, 1872)

                try {
                    val adapter = moshi.adapter(CalendarQuarter::class.java)
                    PrintWriter(FileWriter(getPath(fragment, currentDate, true))).use {
                        it.write(adapter.toJson(calendarQuarterCopy))
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }

                try {
                    val adapter = moshi.adapter(CalendarPattern::class.java)
                    PrintWriter(FileWriter(getPatternPath(fragment, currentDate, true))).use {
                        it.write(adapter.toJson(calendarPattern))
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
        val quarter = currentDate.format(DateTimeFormatter.ofPattern("QQ"))

        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
        val path = File(rootPath, "calendar/$year/")
        if (create) path.mkdirs()

        return File(path, "quarter-$year-$quarter.json")
    }

    /**
     * Get path of the files.
     *
     * @param fragment the fragment
     * @param currentDate the current date
     * @param create create folders
     * @return the path on the filesystem
     */
    private fun getPatternPath(fragment: ScreenFragment, currentDate: LocalDate, create: Boolean = false): File {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))

        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
        val path = File(rootPath, "calendar/$year/")
        if (create) path.mkdirs()

        return File(path, "pattern-$year.json")
    }
}

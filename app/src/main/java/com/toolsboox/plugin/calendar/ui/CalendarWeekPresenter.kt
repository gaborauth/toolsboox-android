package com.toolsboox.plugin.calendar.ui

import android.graphics.Rect
import android.os.Environment
import com.squareup.moshi.Moshi
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.plugin.calendar.da.CalendarWeek
import com.toolsboox.ui.plugin.FragmentPresenter
import com.toolsboox.ui.plugin.ScreenFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import javax.inject.Inject

/**
 * Calendar week presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarWeekPresenter @Inject constructor() : FragmentPresenter() {

    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

    /**
     * Load the week if available.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     */
    fun load(
        fragment: CalendarWeekFragment, binding: FragmentCalendarBinding,
        currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val year = currentDate.year
                val weekOfYear = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
                val week = currentDate.plusWeeks(0L).get(weekOfYear)
                var calendarWeek = CalendarWeek(year, week)

                try {
                    val adapter = moshi.adapter(CalendarWeek::class.java)
                    if (getPath(fragment, currentDate, calendarWeek.locale).exists()) {
                        FileReader(getPath(fragment, currentDate, calendarWeek.locale)).use { fileReader ->
                            adapter.fromJson(fileReader.readText())?.let {
                                it.normalizeStrokes(1404, 1872, surfaceSize.width(), surfaceSize.height())
                                calendarWeek = it
                            }
                        }
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }

                withContext(Dispatchers.Main) { fragment.renderPage(calendarWeek) }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Save the week to the storage.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param calendarWeek the data class
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     */
    fun save(
        fragment: CalendarWeekFragment, binding: FragmentCalendarBinding,
        calendarWeek: CalendarWeek, currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        val locale = calendarWeek.locale
        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val calendarWeekCopy = calendarWeek.deepCopy()
                calendarWeekCopy.normalizeStrokes(surfaceSize.width(), surfaceSize.height(), 1404, 1872)
                try {
                    val adapter = moshi.adapter(CalendarWeek::class.java)
                    PrintWriter(FileWriter(getPath(fragment, currentDate, locale, true))).use {
                        it.write(adapter.toJson(calendarWeekCopy))
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
     * @param locale the current locale
     * @param create create folders
     * @return the path on the filesystem
     */
    private fun getPath(
        fragment: ScreenFragment, currentDate: LocalDate, locale: Locale, create: Boolean = false
    ): File {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val week = currentDate.format(DateTimeFormatter.ofPattern("ww", locale))

        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
        val path = File(rootPath, "calendar/$year/")
        if (create) path.mkdirs()

        return File(path, "week-$year-$week.json")
    }
}

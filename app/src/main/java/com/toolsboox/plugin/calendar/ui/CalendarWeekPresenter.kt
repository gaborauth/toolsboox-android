package com.toolsboox.plugin.calendar.ui

import android.graphics.Rect
import android.os.Environment
import com.google.gson.GsonBuilder
import com.toolsboox.databinding.FragmentCalendarWeekBinding
import com.toolsboox.plugin.calendar.da.CalendarWeek
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.time.LocalDate
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
     * Load the week if available.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     * @param locale the locale
     */
    fun load(
        fragment: CalendarWeekFragment, binding: FragmentCalendarWeekBinding,
        currentDate: LocalDate, surfaceSize: Rect, locale: Locale
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val year = currentDate.year
                val weekOfYear = WeekFields.of(locale).weekOfWeekBasedYear()
                val week = currentDate.plusWeeks(0L).get(weekOfYear)
                var calendarWeek = CalendarWeek(year, week, locale, mutableListOf())

                val rootPath = Environment.getExternalStorageDirectory()
                File("$rootPath/toolsBoox/").mkdirs()
                val filename = "$year-$week"
                val path = "$rootPath/toolsBoox/week-$filename.json"
                try {
                    if (File(path).exists()) {
                        FileReader(File(path)).use {
                            calendarWeek = GsonBuilder().create().fromJson(it, CalendarWeek::class.java)
                            calendarWeek.normalizeStrokes(1404, 1872, surfaceSize.width(), surfaceSize.height())
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
     */
    fun save(
        fragment: CalendarWeekFragment, binding: FragmentCalendarWeekBinding,
        calendarWeek: CalendarWeek, currentDate: LocalDate
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val year = currentDate.year
                val weekOfYear = WeekFields.of(calendarWeek.locale).weekOfWeekBasedYear()
                val week = currentDate.plusWeeks(0L).get(weekOfYear)

                val rootPath = Environment.getExternalStorageDirectory()
                File("$rootPath/toolsBoox/").mkdirs()
                val filename = "$year-$week"
                val path = "$rootPath/toolsBoox/week-$filename.json"
                try {
                    PrintWriter(FileWriter(File(path))).use {
                        it.write(GsonBuilder().create().toJson(calendarWeek).toString())
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

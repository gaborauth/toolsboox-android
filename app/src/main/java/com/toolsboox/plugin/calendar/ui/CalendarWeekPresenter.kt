package com.toolsboox.plugin.calendar.ui

import android.os.Environment
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v2.CalendarWeek
import com.toolsboox.plugin.calendar.fi.CalendarPatternService
import com.toolsboox.plugin.calendar.fi.CalendarWeekService
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.util.*
import javax.inject.Inject

/**
 * Calendar week presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarWeekPresenter @Inject constructor() : FragmentPresenter() {

    /**
     * The calendar week service.
     */
    @Inject
    lateinit var calendarWeekService: CalendarWeekService

    /**
     * The calendar pattern service.
     */
    @Inject
    lateinit var calendarPatternService: CalendarPatternService

    /**
     * Load the week if available.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param locale the default locale
     */
    fun load(
        fragment: CalendarWeekFragment, binding: FragmentCalendarBinding,
        currentDate: LocalDate, locale: Locale
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)

                    val calendarWeek = calendarWeekService.load(rootPath, currentDate, locale)
                    val calendarPattern = calendarPatternService.load(rootPath, currentDate, locale)

                    withContext(Dispatchers.Main) {
                        fragment.renderPage(calendarWeek, calendarPattern)
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
     * Save the week to the storage.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param calendarWeek the data class
     * @param calendarPattern the pattern data class
     * @param currentDate the current date
     */
    fun save(
        fragment: CalendarWeekFragment, binding: FragmentCalendarBinding,
        calendarWeek: CalendarWeek, calendarPattern: CalendarPattern, currentDate: LocalDate
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)

                    calendarWeekService.save(rootPath, currentDate, calendarWeek)
                    calendarPatternService.save(rootPath, currentDate, calendarPattern)
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }
}

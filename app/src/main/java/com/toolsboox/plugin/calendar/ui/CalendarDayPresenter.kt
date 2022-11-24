package com.toolsboox.plugin.calendar.ui

import android.Manifest
import android.os.Environment
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import com.toolsboox.plugin.calendar.fi.CalendarDayService
import com.toolsboox.plugin.calendar.fi.CalendarPatternService
import com.toolsboox.plugin.calendar.fi.GoogleCalendarService
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
 * Calendar day presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarDayPresenter @Inject constructor() : FragmentPresenter() {

    /**
     * The calendar day service.
     */
    @Inject
    lateinit var calendarDayService: CalendarDayService

    /**
     * The calendar pattern service.
     */
    @Inject
    lateinit var calendarPatternService: CalendarPatternService

    /**
     * The Google Calendar service.
     */
    @Inject
    lateinit var googleCalendarService: GoogleCalendarService

    /**
     * Load the daily calendar data.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param locale the default locale
     */
    fun load(
        fragment: CalendarDayFragment, binding: FragmentCalendarBinding,
        currentDate: LocalDate, locale: Locale
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        if (!fragment.checkPermission(Manifest.permission.READ_CALENDAR)) {
            fragment.showError(null, R.string.main_read_calendar_permission_missing, binding.root)
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)

                    val calendarDay = calendarDayService.load(rootPath, currentDate, locale)
                    val calendarPattern = calendarPatternService.load(rootPath, currentDate, locale)
                    val googleCalendarEvents = googleCalendarService.loadEvents(fragment, currentDate)

                    withContext(Dispatchers.Main) { fragment.renderPage(calendarDay, calendarPattern, googleCalendarEvents) }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Save the day to the storage.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param calendarDay the data class
     * @param calendarPattern the pattern data class
     * @param currentDate the current date
     */
    fun save(
        fragment: CalendarDayFragment, binding: FragmentCalendarBinding,
        calendarDay: CalendarDay, calendarPattern: CalendarPattern, currentDate: LocalDate
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)

                    calendarDayService.save(rootPath, currentDate, calendarDay)
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

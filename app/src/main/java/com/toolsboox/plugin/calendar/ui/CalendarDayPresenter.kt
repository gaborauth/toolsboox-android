package com.toolsboox.plugin.calendar.ui

import android.Manifest
import android.os.Environment
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import com.toolsboox.plugin.calendar.fi.CalendarDayService
import com.toolsboox.plugin.calendar.fi.CalendarEventsService
import com.toolsboox.plugin.calendar.fi.CalendarPatternService
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
     * The calendar events service.
     */
    @Inject
    lateinit var calendarEventsService: CalendarEventsService

    /**
     * Load the daily calendar data.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param defaultStartHour the default start hour
     * @param locale the default locale
     */
    fun load(
        fragment: CalendarDayFragment, binding: FragmentCalendarBinding,
        currentDate: LocalDate, defaultStartHour: Int, locale: Locale
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

                    val calendarDay = calendarDayService.load(rootPath, currentDate, defaultStartHour, locale)
                    val calendarPattern = calendarPatternService.load(rootPath, currentDate, locale)
                    var calendarEvents = calendarEventsService.loadEvents(fragment, currentDate)
                    calendarDay.startHour = calendarDay.startHour ?: defaultStartHour

                    if (currentDate < LocalDate.now()) {
                        if (calendarDay.events.isEmpty()) {
                            calendarDay.events.addAll(calendarEvents)
                        } else {
                            calendarEvents = calendarDay.events
                        }
                    } else {
                        calendarDay.events.clear()
                        calendarDay.events.addAll(calendarEvents)
                    }

                    withContext(Dispatchers.Main) { fragment.renderPage(calendarDay, calendarPattern, calendarEvents) }
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

                    val emptyStrokes = calendarDay.calendarStrokes[CalendarDay.DEFAULT_STYLE]?.isEmpty() ?: true
                    calendarDay.hasLanes = calendarDay.hasLanes or emptyStrokes

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

package com.toolsboox.plugin.calendar.ui

import android.Manifest
import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import com.toolsboox.R
import com.toolsboox.da.Stroke
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v1.ReadingProgress
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import com.toolsboox.plugin.calendar.fi.CalendarDayService
import com.toolsboox.plugin.calendar.fi.CalendarEventsService
import com.toolsboox.plugin.calendar.fi.CalendarPatternService
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
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

                    if (currentDate >= LocalDate.now()) {
                        calendarDay.readingProgress.clear()
                    }
                    if (calendarDay.readingProgress.isEmpty()) {
                        calendarDay.readingProgress.addAll(readingProgress(fragment, currentDate))
                    }

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

                    CalendarPatternService.mutex.withLock {
                        calendarDayService.save(rootPath, currentDate, calendarDay)
                        calendarPatternService.save(rootPath, currentDate, calendarPattern)
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
     * Procrastinate the strokes to the next day.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param strokes the strokes
     * @param currentDate the current date
     * @param fromCalendarDay the calendar day of the strokes
     * @param fromCalendarStyle the calendar style of the strokes
     */
    fun procrastinate(
        fragment: CalendarDayFragment, binding: FragmentCalendarBinding, strokes: List<Stroke>,
        currentDate: LocalDate, fromCalendarDay: CalendarDay, fromCalendarStyle: String
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        val nextDate = currentDate.plusDays(1)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    CalendarPatternService.mutex.withLock {
                        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                        val calendarDay = calendarDayService.load(rootPath, nextDate, fromCalendarDay.startHour, fromCalendarDay.locale)
                        val calendarPattern = calendarPatternService.load(rootPath, nextDate, fromCalendarDay.locale)

                        val existsStrokes = calendarDay.calendarStrokes[fromCalendarStyle]?.toMutableList() ?: mutableListOf()
                        existsStrokes.addAll(strokes)
                        calendarDay.calendarStrokes[fromCalendarStyle] = existsStrokes.toList()

                        calendarPattern.updateDay(calendarDay)
                        calendarDayService.save(rootPath, nextDate, calendarDay)
                        calendarPatternService.save(rootPath, nextDate, calendarPattern)
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
     * Provides the authors, title, progress and lastAccess fields of reading progress of current day.
     *
     * @param fragment the fragment
     * @param currentDate the current date
     * @return the list of reading progress
     */
    private fun readingProgress(fragment: CalendarDayFragment, currentDate: LocalDate): List<ReadingProgress> {
        val result = mutableListOf<ReadingProgress>()

        val startEpoch = currentDate.atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L
        val endEpoch = startEpoch + 24 * 60 * 60 * 1000L

        val resolver: ContentResolver = fragment.requireContext().contentResolver
        val uri = Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata")
        val projection = arrayOf("authors", "title", "progress", "lastAccess")
        resolver.query(uri, projection, null, null, null)?.use {
            while (it.moveToNext()) {
                val authors = it.getString(0)
                val title = it.getString(1)
                val progress = it.getString(2)
                val lastAccess = it.getLong(3)

                if (title == null) continue
                if (lastAccess < startEpoch) continue
                if (lastAccess >= endEpoch) continue
                result.add(ReadingProgress(authors, title, progress, Date(lastAccess)))
            }
        }

        Timber.i("Reading progress: $result")
        return result
    }
}

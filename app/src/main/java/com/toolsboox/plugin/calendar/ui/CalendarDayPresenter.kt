package com.toolsboox.plugin.calendar.ui

import android.Manifest
import android.content.ContentUris
import android.graphics.Rect
import android.os.Environment
import android.provider.CalendarContract.Instances
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.squareup.moshi.Moshi
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.plugin.calendar.da.v1.CalendarDay
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v1.GoogleCalendarEvent
import com.toolsboox.ui.plugin.FragmentPresenter
import com.toolsboox.ui.plugin.ScreenFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

/**
 * Calendar day presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarDayPresenter @Inject constructor() : FragmentPresenter() {

    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

    /**
     * Load the day if available.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     * @param locale the default locale
     */
    fun load(
        fragment: CalendarDayFragment, binding: FragmentCalendarBinding,
        currentDate: LocalDate, surfaceSize: Rect, locale: Locale
    ) {
        if (!fragment.checkPermission(Manifest.permission.READ_CALENDAR)) {
            fragment.showError(null, R.string.main_read_calendar_permission_missing, binding.root)
            return
        }

        if (!fragment.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            fragment.showError(null, R.string.main_read_external_storage_permission_missing, binding.root)
            return
        }

        if (!fragment.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            fragment.showError(null, R.string.main_write_external_storage_permission_missing, binding.root)
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val year = currentDate.year
                val month = currentDate.monthValue
                val day = currentDate.dayOfMonth
                var calendarDay = CalendarDay(year, month, day, locale)

                try {
                    val adapter = moshi.adapter(CalendarDay::class.java)
                    if (getPath(fragment, currentDate).exists()) {
                        FileReader(getPath(fragment, currentDate)).use { fileReader ->
                            adapter.fromJson(fileReader.readText())?.let {
                                it.normalizeStrokes(1404, 1872, surfaceSize.width(), surfaceSize.height())
                                calendarDay = it
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

                val googleCalendarEvents = loadGoogleCalendarEvents(fragment, currentDate)
                withContext(Dispatchers.Main) {
                    fragment.renderPage(calendarDay, calendarPattern, googleCalendarEvents)
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
     * @param surfaceSize the actual size of surface view
     */
    fun save(
        fragment: CalendarDayFragment, binding: FragmentCalendarBinding,
        calendarDay: CalendarDay, calendarPattern: CalendarPattern, currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val calendarDayCopy = calendarDay.deepCopy()
                calendarDayCopy.normalizeStrokes(surfaceSize.width(), surfaceSize.height(), 1404, 1872)

                try {
                    val adapter = moshi.adapter(CalendarDay::class.java)
                    PrintWriter(FileWriter(getPath(fragment, currentDate, true))).use {
                        it.write(adapter.toJson(calendarDayCopy))
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
     * Load daily calendar events of the user.
     *
     * @param fragment the calendar fragment
     * @param currentDate the current date
     * @return the list of Google Calendar events
     */
    private fun loadGoogleCalendarEvents(
        fragment: CalendarDayFragment, currentDate: LocalDate
    ): List<GoogleCalendarEvent> {
        val googleCalendarEvents = mutableListOf<GoogleCalendarEvent>()
        if (!fragment.checkPermission(Manifest.permission.READ_CALENDAR)) return googleCalendarEvents

        val contentResolver = fragment.requireActivity().contentResolver
        val startDate = currentDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1000
        val endDate = currentDate.plusDays(1L).atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1000

        val uriBuilder = Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, startDate)
        ContentUris.appendId(uriBuilder, endDate)
        val uri = uriBuilder.build()

        val instanceFields = arrayOf(
            Instances._ID, Instances.TITLE, Instances.DESCRIPTION,
            Instances.ALL_DAY, Instances.BEGIN, Instances.END
        )

        val selection = "((${Instances.BEGIN} >= $startDate) " +
                "AND (${Instances.END} <= $endDate) " +
                "AND (${Instances.VISIBLE} = 1))"

        contentResolver.query(uri, instanceFields, selection, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getStringOrNull(0) ?: continue
                val title = cursor.getStringOrNull(1) ?: continue
                val description = cursor.getStringOrNull(2) ?: continue
                val allDay = cursor.getIntOrNull(3) ?: continue
                val dtStart = cursor.getLongOrNull(4) ?: continue
                val dtEnd = cursor.getLongOrNull(5) ?: continue

                googleCalendarEvents.add(
                    GoogleCalendarEvent(
                        id, title, description, allDay > 0,
                        Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                        Instant.ofEpochMilli(dtEnd).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    )
                )
            }
        }

        googleCalendarEvents.sortBy { it.startDate }
        return googleCalendarEvents
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
        val day = currentDate.format(DateTimeFormatter.ofPattern("dd"))

        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
        val path = File(rootPath, "calendar/$year/$month/")
        if (create) path.mkdirs()

        return File(path, "day-$year-$month-$day.json")
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

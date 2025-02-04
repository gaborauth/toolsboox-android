package com.toolsboox.plugin.calendar.fi

import android.Manifest
import android.content.ContentUris
import android.provider.CalendarContract
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.toolsboox.plugin.calendar.da.v1.CalendarEvent
import com.toolsboox.plugin.calendar.ui.CalendarDayFragment
import java.time.LocalDate
import javax.inject.Inject

/**
 * Calendar events service.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarEventsService @Inject constructor() {
    /**
     * Load daily calendar events of the user.
     *
     * @param fragment the calendar fragment
     * @param currentDate the current date
     * @return the list of calendar events
     */
    fun loadEvents(fragment: CalendarDayFragment, currentDate: LocalDate): List<CalendarEvent> {
        val calendarEvents = mutableListOf<CalendarEvent>()
        if (!fragment.checkPermission(Manifest.permission.READ_CALENDAR)) return calendarEvents
        if (!fragment.isAdded) return calendarEvents

        val contentResolver = fragment.requireActivity().contentResolver
        val startDay = 2440588 + currentDate.toEpochDay()
        val endDay = 2440588 + currentDate.toEpochDay()

        val uriBuilder = CalendarContract.Instances.CONTENT_BY_DAY_URI.buildUpon()
        ContentUris.appendId(uriBuilder, startDay)
        ContentUris.appendId(uriBuilder, endDay)
        val uri = uriBuilder.build()

        val instanceFields = arrayOf(
            CalendarContract.Instances._ID, CalendarContract.Instances.EVENT_COLOR, CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.TITLE, CalendarContract.Instances.DESCRIPTION, CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
            CalendarContract.Instances.START_DAY, CalendarContract.Instances.END_DAY
        )

        val selection = CalendarContract.Calendars.VISIBLE + "=?"
        val selectionArgs = arrayOf("1")

        contentResolver.query(uri, instanceFields, selection, selectionArgs, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val eventColor = 0xff000000 or (cursor.getLongOrNull(1) ?: 0)
                val calendarColor = 0xff000000 or (cursor.getLongOrNull(2) ?: 0)
                val title = cursor.getStringOrNull(3) ?: "-no-title-"
                val description = cursor.getStringOrNull(4) ?: "-no-description-"
                val allDay = cursor.getIntOrNull(5) ?: 0
                val dtStart = cursor.getLong(6)
                val dtEnd = cursor.getLong(7)
                val eStartDay = cursor.getLong(8)
                val eEndDay = cursor.getLong(9)

                // Skip events that are not all day events and not in the current day
                if (eStartDay > endDay || eEndDay < startDay) continue

                calendarEvents.add(
                    CalendarEvent(
                        id, title, description, allDay > 0,
                        dtStart, dtEnd, calendarColor, eventColor
                    )
                )
            }
        }

        calendarEvents.sortBy { it.startDate }
        return calendarEvents
    }
}
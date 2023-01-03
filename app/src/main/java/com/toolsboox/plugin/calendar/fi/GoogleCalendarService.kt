package com.toolsboox.plugin.calendar.fi

import android.Manifest
import android.content.ContentUris
import android.provider.CalendarContract
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.toolsboox.plugin.calendar.da.v1.GoogleCalendarEvent
import com.toolsboox.plugin.calendar.ui.CalendarDayFragment
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Google Calendar service.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class GoogleCalendarService @Inject constructor() {
    /**
     * Load daily calendar events of the user.
     *
     * @param fragment the calendar fragment
     * @param currentDate the current date
     * @return the list of Google Calendar events
     */
    fun loadEvents(fragment: CalendarDayFragment, currentDate: LocalDate): List<GoogleCalendarEvent> {
        val googleCalendarEvents = mutableListOf<GoogleCalendarEvent>()
        if (!fragment.checkPermission(Manifest.permission.READ_CALENDAR)) return googleCalendarEvents

        val contentResolver = fragment.requireActivity().contentResolver
        val startDate = currentDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1000
        val endDate = currentDate.plusDays(1L).atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1000

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, startDate)
        ContentUris.appendId(uriBuilder, endDate)
        val uri = uriBuilder.build()

        val instanceFields = arrayOf(
            CalendarContract.Instances._ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END
        )

        val selection = "((${CalendarContract.Instances.BEGIN} >= $startDate) " +
                "AND (${CalendarContract.Instances.END} <= $endDate) " +
                "AND (${CalendarContract.Instances.VISIBLE} = 1))"

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
     * List of calendars.
     *
     * @param fragment the fragment
     */
    private fun listOfCalendars(fragment: CalendarDayFragment) {
        val contentResolver = fragment.requireActivity().contentResolver

        val uriBuilder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
        val uri = uriBuilder.build()

        val fields = arrayOf(
            CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME
        )

        contentResolver.query(uri, fields, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val cid: String = cursor.getString(0)
                val displayName: String = cursor.getString(1)
                val accountName: String = cursor.getString(2)
                Timber.i("Calendar id: $cid, display name: $displayName, account name: $accountName")
            }
        }
    }
}
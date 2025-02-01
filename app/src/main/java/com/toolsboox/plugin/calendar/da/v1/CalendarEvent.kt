package com.toolsboox.plugin.calendar.da.v1

import com.squareup.moshi.JsonClass

/**
 * Calendar event data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */

@JsonClass(generateAdapter = true)
data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String,
    val allDay: Boolean,
    val startDate: Long,
    val endDate: Long,
    val calendarColor: Long,
    val eventColor: Long,
)

package com.toolsboox.plugin.calendar.da

import java.time.LocalDateTime

/**
 * Google Calendar event data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */

data class GoogleCalendarEvent(
    val id: String,
    val title: String,
    val description: String,
    val allDay: Boolean,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

package com.toolsboox.plugin.calendar.da.v1

import java.util.*

/**
 * Calendar item in the cloud storage.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class CalendarItem(
    val userId: UUID,
    val path: String,
    val baseName: String,
    val version: String,
    val created: Date?,
    val updated: Date?
)

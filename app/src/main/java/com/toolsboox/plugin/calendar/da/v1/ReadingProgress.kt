package com.toolsboox.plugin.calendar.da.v1

import com.squareup.moshi.JsonClass
import java.util.*

/**
 * Reading progress data class.
 */
@JsonClass(generateAdapter = true)
data class ReadingProgress(
    val authors: String?,
    val title: String,
    val progress: String?,
    val lastAccess: Date
)

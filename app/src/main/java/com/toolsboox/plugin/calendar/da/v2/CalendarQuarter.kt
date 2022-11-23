package com.toolsboox.plugin.calendar.da.v2

import com.squareup.moshi.JsonClass
import com.toolsboox.da.Stroke
import java.util.*

/**
 * Calendar quarter data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class CalendarQuarter(
    val year: Int,
    val quarter: Int,
    val locale: Locale = Locale.getDefault(),

    override var calendarStrokes: Map<String, List<Stroke>> = mapOf(),
    override var noteStrokes: Map<String, List<Stroke>> = mapOf()
) : Calendar {
    /**
     * Deep copy of the calendar quarter data class
     */
    fun deepCopy(): CalendarQuarter {
        return CalendarQuarter(
            this.year, this.quarter, this.locale,
            Calendar.mapDeepCopy(calendarStrokes), Calendar.mapDeepCopy(noteStrokes)
        )
    }
}

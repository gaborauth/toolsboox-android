package com.toolsboox.plugin.calendar.da.v2

import com.squareup.moshi.JsonClass
import com.toolsboox.da.Stroke
import java.util.*

/**
 * Calendar week data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class CalendarWeek(
    var year: Int,
    var weekOfYear: Int,
    val locale: Locale = Locale.getDefault(),

    override var calendarStrokes: Map<String, List<Stroke>> = mapOf(),
    override var noteStrokes: Map<String, List<Stroke>> = mapOf()
) : Calendar {
    /**
     * Deep copy of the calendar week data class
     */
    fun deepCopy(): CalendarWeek {
        return CalendarWeek(
            this.year, this.weekOfYear, this.locale,
            Calendar.mapDeepCopy(calendarStrokes), Calendar.mapDeepCopy(noteStrokes)
        )
    }
}

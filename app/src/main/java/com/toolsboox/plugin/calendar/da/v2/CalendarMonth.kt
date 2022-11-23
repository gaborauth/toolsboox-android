package com.toolsboox.plugin.calendar.da.v2

import com.squareup.moshi.JsonClass
import com.toolsboox.da.Stroke
import java.util.*

/**
 * Calendar month data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class CalendarMonth(
    val year: Int,
    val month: Int,
    val locale: Locale = Locale.getDefault(),

    override var calendarStrokes: Map<String, List<Stroke>> = mapOf(),
    override var noteStrokes: Map<String, List<Stroke>> = mapOf()
) : Calendar {
    /**
     * Deep copy of the calendar month data class
     */
    fun deepCopy(): CalendarMonth {
        return CalendarMonth(
            this.year, this.month, this.locale,
            Calendar.mapDeepCopy(calendarStrokes), Calendar.mapDeepCopy(noteStrokes)
        )
    }
}

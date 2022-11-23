package com.toolsboox.plugin.calendar.da.v2

import com.squareup.moshi.JsonClass
import com.toolsboox.da.Stroke
import java.util.*

/**
 * Calendar year data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class CalendarYear(
    val year: Int,
    val locale: Locale = Locale.getDefault(),

    override var calendarStrokes: Map<String, List<Stroke>> = mapOf(),
    override var noteStrokes: Map<String, List<Stroke>> = mapOf()
) : Calendar {
    /**
     * Deep copy of the calendar year data class
     */
    fun deepCopy(): CalendarYear {
        return CalendarYear(
            this.year, this.locale,
            Calendar.mapDeepCopy(calendarStrokes), Calendar.mapDeepCopy(noteStrokes)
        )
    }
}

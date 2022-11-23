package com.toolsboox.plugin.calendar.da.v2

import com.squareup.moshi.JsonClass
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
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

    override val strokes: List<Stroke> = listOf(),
    override val notesStrokes: List<Stroke> = listOf()
) : Calendar {
    /**
     * Deep copy of the calendar week data class
     */
    fun deepCopy(): CalendarWeek {
        val strokes = Calendar.listDeepCopy(this.strokes)
        val notesStrokes = Calendar.listDeepCopy(this.notesStrokes)

        return CalendarWeek(this.year, this.weekOfYear, this.locale, strokes, notesStrokes)
    }
}

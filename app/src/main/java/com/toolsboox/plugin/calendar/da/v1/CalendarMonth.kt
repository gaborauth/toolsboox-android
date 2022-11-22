package com.toolsboox.plugin.calendar.da.v1

import com.squareup.moshi.JsonClass
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
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

    override val strokes: List<Stroke> = listOf(),
    override val notesStrokes: List<Stroke> = listOf()
) : Calendar {
    /**
     * Deep copy of the calendar month data class
     */
    fun deepCopy(): CalendarMonth {
        val strokes = Calendar.listDeepCopy(this.strokes)
        val notesStrokes = Calendar.listDeepCopy(this.notesStrokes)

        return CalendarMonth(this.year, this.month, this.locale, strokes, notesStrokes)
    }
}
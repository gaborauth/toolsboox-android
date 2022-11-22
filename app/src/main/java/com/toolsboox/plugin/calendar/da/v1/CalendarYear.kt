package com.toolsboox.plugin.calendar.da.v1

import com.squareup.moshi.JsonClass
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
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

    override val strokes: List<Stroke> = listOf(),
    override val notesStrokes: List<Stroke> = listOf()
) : Calendar {
    /**
     * Deep copy of the calendar year data class
     */
    fun deepCopy(): CalendarYear {
        val strokes = Calendar.listDeepCopy(this.strokes)
        val notesStrokes = Calendar.listDeepCopy(this.notesStrokes)

        return CalendarYear(this.year, this.locale, strokes, notesStrokes)
    }
}

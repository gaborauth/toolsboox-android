package com.toolsboox.plugin.calendar.da

import com.squareup.moshi.JsonClass
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
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

    override val strokes: List<Stroke> = listOf(),
    override val notesStrokes: List<Stroke> = listOf()
) : Calendar {
    /**
     * Deep copy of the calendar quarter data class
     */
    fun deepCopy(): CalendarQuarter {
        val strokes = Calendar.listDeepCopy(this.strokes)
        val notesStrokes = Calendar.listDeepCopy(this.notesStrokes)

        return CalendarQuarter(this.year, this.quarter, this.locale, strokes, notesStrokes)
    }
}

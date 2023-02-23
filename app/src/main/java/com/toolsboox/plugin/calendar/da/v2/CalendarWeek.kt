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

    override var calendarStrokes: MutableMap<String, List<Stroke>> = mutableMapOf(),
    override var calendarValues: MutableMap<String, Map<String, Float?>> = mutableMapOf(),
    override var noteStrokes: MutableMap<String, List<Stroke>> = mutableMapOf(),
    override var created: Date? = null,
    override var updated: Date? = null
) : Calendar {

    companion object {
        /**
         * Name of the default calendar page style.
         */
        const val DEFAULT_STYLE = "Default"

        /**
         * Covert calendar week data class from v1 format to v2 format.
         *
         * @param v1 the v1 data class
         * @return the v2 data class
         */
        fun convert(v1: com.toolsboox.plugin.calendar.da.v1.CalendarWeek): CalendarWeek {
            val strokes = com.toolsboox.plugin.teamdrawer.nw.domain.Stroke.convertTo(v1.strokes)
            val notesStrokes = com.toolsboox.plugin.teamdrawer.nw.domain.Stroke.convertTo(v1.notesStrokes)

            val calendarStrokes = mutableMapOf(DEFAULT_STYLE to strokes)
            val calendarValues = mutableMapOf(CalendarDay.DEFAULT_STYLE to mapOf<String, Float?>())
            val noteStrokes = mutableMapOf("0" to notesStrokes)

            return CalendarWeek(v1.year, v1.weekOfYear, v1.locale, calendarStrokes, calendarValues, noteStrokes)
        }
    }

    /**
     * Deep copy of the calendar week data class
     */
    fun deepCopy(): CalendarWeek {
        return CalendarWeek(
            this.year, this.weekOfYear, this.locale,
            Calendar.strokesDeepCopy(calendarStrokes), Calendar.valuesDeepCopy(calendarValues), Calendar.strokesDeepCopy(noteStrokes)
        )
    }
}

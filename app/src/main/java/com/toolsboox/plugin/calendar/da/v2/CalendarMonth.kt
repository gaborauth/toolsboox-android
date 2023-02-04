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

    override var calendarStrokes: MutableMap<String, List<Stroke>> = mutableMapOf(),
    override var calendarValues: MutableMap<String, Map<String, Float?>> = mutableMapOf(),
    override var noteStrokes: MutableMap<String, List<Stroke>> = mutableMapOf(),
    override val cloudCreated: Date? = null,
    override val cloudUpdated: Date? = null
) : Calendar {

    companion object {
        /**
         * Name of the default calendar page style.
         */
        const val DEFAULT_STYLE = "Default"

        /**
         * Covert calendar month data class from v1 format to v2 format.
         *
         * @param v1 the v1 data class
         * @return the v2 data class
         */
        fun convert(v1: com.toolsboox.plugin.calendar.da.v1.CalendarMonth): CalendarMonth {
            val strokes = com.toolsboox.plugin.teamdrawer.nw.domain.Stroke.convertTo(v1.strokes)
            val notesStrokes = com.toolsboox.plugin.teamdrawer.nw.domain.Stroke.convertTo(v1.notesStrokes)

            val calendarStrokes = mutableMapOf(DEFAULT_STYLE to strokes)
            val calendarValues = mutableMapOf(CalendarDay.DEFAULT_STYLE to mapOf<String, Float?>())
            val noteStrokes = mutableMapOf("0" to notesStrokes)

            return CalendarMonth(v1.year, v1.month, v1.locale, calendarStrokes, calendarValues, noteStrokes)
        }
    }

    /**
     * Deep copy of the calendar month data class
     */
    fun deepCopy(): CalendarMonth {
        return CalendarMonth(
            this.year, this.month, this.locale,
            Calendar.strokesDeepCopy(calendarStrokes), Calendar.valuesDeepCopy(calendarValues), Calendar.strokesDeepCopy(noteStrokes)
        )
    }
}

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
         * Covert calendar year data class from v1 format to v2 format.
         *
         * @param v1 the v1 data class
         * @return the v2 data class
         */
        fun convert(v1: com.toolsboox.plugin.calendar.da.v1.CalendarYear): CalendarYear {
            val strokes = com.toolsboox.plugin.teamdrawer.nw.domain.Stroke.convertTo(v1.strokes)
            val notesStrokes = com.toolsboox.plugin.teamdrawer.nw.domain.Stroke.convertTo(v1.notesStrokes)

            val calendarStrokes = mutableMapOf(DEFAULT_STYLE to strokes)
            val calendarValues = mutableMapOf(CalendarDay.DEFAULT_STYLE to mapOf<String, Float?>())
            val noteStrokes = mutableMapOf("0" to notesStrokes)

            return CalendarYear(v1.year, v1.locale, calendarStrokes, calendarValues, noteStrokes)
        }
    }

    /**
     * Deep copy of the calendar year data class
     */
    fun deepCopy(): CalendarYear {
        return CalendarYear(
            this.year, this.locale,
            Calendar.strokesDeepCopy(calendarStrokes), Calendar.valuesDeepCopy(calendarValues), Calendar.strokesDeepCopy(noteStrokes)
        )
    }
}

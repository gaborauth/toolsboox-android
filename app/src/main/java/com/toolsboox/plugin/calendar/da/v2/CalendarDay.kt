package com.toolsboox.plugin.calendar.da.v2

import com.squareup.moshi.JsonClass
import com.toolsboox.da.Stroke
import com.toolsboox.plugin.calendar.da.v1.CalendarEvent
import java.util.*

/**
 * Calendar day data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class CalendarDay(
    val year: Int,
    val month: Int,
    val day: Int,
    val locale: Locale = Locale.getDefault(),
    val events: MutableList<CalendarEvent> = mutableListOf(),

    var hasLanes : Boolean = false,
    var startHour: Int?,

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
         * Name of the Health v1 page style.
         */
        const val HEALTH_V1_STYLE = "Health.v1"

        /**
         * Name of the TimeBox v1 page style.
         */
        const val TIME_BOX_V1_STYLE = "TimeBox.v1"

        /**
         * Covert calendar day data class from v1 format to v2 format.
         *
         * @param v1 the v1 data class
         * @return the v2 data class
         */
        fun convert(v1: com.toolsboox.plugin.calendar.da.v1.CalendarDay): CalendarDay {
            val strokes = com.toolsboox.plugin.teamdrawer.nw.domain.Stroke.convertTo(v1.strokes)
            val notesStrokes = com.toolsboox.plugin.teamdrawer.nw.domain.Stroke.convertTo(v1.notesStrokes)

            val calendarStrokes = mutableMapOf(DEFAULT_STYLE to strokes)
            val calendarValues = mutableMapOf(DEFAULT_STYLE to mapOf<String, Float?>())
            val noteStrokes = mutableMapOf("0" to notesStrokes)

            return CalendarDay(v1.year, v1.month, v1.day, v1.locale, mutableListOf(), false, null, calendarStrokes, calendarValues, noteStrokes)
        }
    }

    /**
     * Deep copy of the calendar day data class
     */
    fun deepCopy(): CalendarDay {
        return CalendarDay(
            this.year, this.month, this.day, this.locale, this.events.toMutableList(), this.hasLanes, this.startHour,
            Calendar.strokesDeepCopy(calendarStrokes), Calendar.valuesDeepCopy(calendarValues), Calendar.strokesDeepCopy(noteStrokes)
        )
    }
}

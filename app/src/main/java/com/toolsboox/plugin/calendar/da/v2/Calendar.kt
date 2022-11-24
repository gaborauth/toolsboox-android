package com.toolsboox.plugin.calendar.da.v2

import com.toolsboox.da.Stroke

/**
 * Calendar interface, common data and methods.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface Calendar {
    val calendarStrokes: Map<String, List<Stroke>>
    val noteStrokes: Map<String, List<Stroke>>

    companion object {
        /**
         * Deep copy of map of strokes.
         *
         * @param mapOfStrokes the map of strokes
         * @return the map of strokes
         */
        fun mapDeepCopy(mapOfStrokes: Map<String, List<Stroke>>): MutableMap<String, List<Stroke>> {
            val strokes = mutableMapOf<String, List<Stroke>>()
            mapOfStrokes.forEach { strokes[it.key] = Stroke.listDeepCopy(it.value) }
            return strokes
        }
    }
}
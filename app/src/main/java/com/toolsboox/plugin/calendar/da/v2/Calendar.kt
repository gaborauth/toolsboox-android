package com.toolsboox.plugin.calendar.da.v2

import com.toolsboox.da.Stroke
import java.util.*

/**
 * Calendar interface, common data and methods.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface Calendar {
    val calendarStrokes: Map<String, List<Stroke>>
    val calendarValues: Map<String, Map<String, Float?>>
    val noteStrokes: Map<String, List<Stroke>>
    val cloudUpdated: Date?

    companion object {
        /**
         * Deep copy of map of strokes.
         *
         * @param mapOfStrokes the map of strokes
         * @return the map of strokes
         */
        fun strokesDeepCopy(mapOfStrokes: Map<String, List<Stroke>>): MutableMap<String, List<Stroke>> {
            val strokes = mutableMapOf<String, List<Stroke>>()
            mapOfStrokes.forEach { strokes[it.key] = Stroke.listDeepCopy(it.value) }
            return strokes
        }

        /**
         * Deep copy of map of values.
         *
         * @param mapOfValues the map of values
         * @return the map of values
         */
        fun valuesDeepCopy(mapOfValues: Map<String, Map<String, Float?>>): MutableMap<String, Map<String, Float?>> {
            val values = mutableMapOf<String, Map<String, Float?>>()
            mapOfValues.forEach { values[it.key] = it.value.toMap() }
            return values
        }
    }
}
package com.toolsboox.plugin.calendar.da

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import java.lang.reflect.Type

/**
 * Calendar interface, common data and methods.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface Calendar {
    val strokes: List<Stroke>
    val notesStrokes: List<Stroke>

    companion object {
        /**
         * Deep copy of strokes.
         *
         * @param strokes the source list
         * @return the destination list
         */
        fun listDeepCopy(strokes: List<Stroke>): List<Stroke> {
            val listType: Type = object : TypeToken<List<Stroke>>() {}.type
            val gson = Gson()
            val json: String = gson.toJson(strokes, listType)
            return gson.fromJson(json, listType)
        }

        /**
         * Normalize the strokes (stretch it to the template's size).
         *
         * @param strokes the list of strokes
         * @param fromWidth the source width
         * @param fromHeight the source height
         * @param toWidth the destination width
         * @param toHeight the destination height
         */
        private fun normalizeStrokes(
            strokes: List<Stroke>, fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int
        ) {
            val widthRatio = 1.0f * toWidth / fromWidth
            val heightRatio = 1.0f * toHeight / fromHeight
            for (stroke in strokes) {
                for (point in stroke.strokePoints) {
                    point.x *= widthRatio
                    point.y *= heightRatio
                }
            }
        }
    }

    /**
     * Normalize the strokes (stretch it to the template's size).
     *
     * @param fromWidth the source width
     * @param fromHeight the source height
     * @param toWidth the destination width
     * @param toHeight the destination height
     */
    fun normalizeStrokes(fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int) {
        normalizeStrokes(strokes, fromWidth, fromHeight, toWidth, toHeight)
        normalizeStrokes(notesStrokes, fromWidth, fromHeight, toWidth, toHeight)
    }
}
package com.toolsboox.plugin.teamdrawer.nw.domain

import com.squareup.moshi.JsonClass
import com.toolsboox.da.StrokePoint
import java.util.*

/**
 * Stroke data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class Stroke(
    var pageId: UUID,
    var strokeId: UUID,
    var strokePoints: List<StrokePoint>
) {
    companion object {
        /**
         * Convert strokes from common format to team drawer format.
         *
         * @param commonStrokes the list of strokes in common format
         * @param pageId the page id to add
         * @return list of strokes in team drawer format
         */
        fun convertFrom(commonStrokes: List<com.toolsboox.da.Stroke>, pageId: UUID): List<Stroke> {
            val strokes = mutableListOf<Stroke>()
            commonStrokes.forEach { s -> strokes.add(Stroke(pageId, s.strokeId, s.strokePoints)) }

            return strokes.toList()
        }

        /**
         * Convert strokes from team drawer format to common format.
         *
         * @param teamStrokes the list of strokes in team drawer format
         * @return list of strokes in common format
         */
        fun convertTo(teamStrokes: List<Stroke>): List<com.toolsboox.da.Stroke> {
            val strokes = mutableListOf<com.toolsboox.da.Stroke>()
            teamStrokes.forEach { s -> strokes.add(com.toolsboox.da.Stroke(s.strokeId, s.strokePoints)) }

            return strokes.toList()
        }
    }
}

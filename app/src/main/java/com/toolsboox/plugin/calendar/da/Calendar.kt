package com.toolsboox.plugin.calendar.da

import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke

/**
 * Calendar interface, common data and methods.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface Calendar {
    val strokes: List<Stroke>

    /**
     * Normalize the strokes (stretch it to the template's size).
     *
     * @param fromWidth the source width
     * @param fromHeight the source height
     * @param toWidth the destination width
     * @param toHeight the destination height
     */
    fun normalizeStrokes(fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int) {
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
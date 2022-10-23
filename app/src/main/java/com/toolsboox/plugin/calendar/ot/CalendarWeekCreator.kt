package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import com.toolsboox.ot.Creator
import com.toolsboox.plugin.calendar.da.CalendarWeek
import java.util.*

/**
 * Create weekly template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarWeekCreator : Creator {

    companion object {
        /**
         * Draw weekly template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarWeek data class
         */
        fun drawPage(context: Context, canvas: Canvas, calendarWeek: CalendarWeek) {
            val year = calendarWeek.year
            val locale = calendarWeek.locale ?: Locale.getDefault()

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            canvas.drawText("Soon! Stay tuned! :)", 100.0f, 100.0f, Creator.textDefaultBlack)
        }
    }
}
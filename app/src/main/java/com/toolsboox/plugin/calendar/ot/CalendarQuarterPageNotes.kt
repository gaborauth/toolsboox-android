package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.toolsboox.ot.Creator
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v2.CalendarQuarter
import com.toolsboox.plugin.calendar.ui.CalendarQuarterFragment
import java.time.LocalDate

/**
 * Create quarterly template of calendar plugin notes.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarQuarterPageNotes : Creator {

    companion object {

        // Cell width
        private const val cew = 1300.0f

        // Cell height
        private const val ceh = 50.0f

        // Left offset
        private const val lo = (1404.0f - 1 * cew) / 2.0f

        // Top offset
        private const val to = (1872.0f - 35 * ceh) / 2.0f

        /**
         * Process touch event on the calendar page and navigate to the view of calendar.
         *
         * @param view the surface view
         * @param motionEvent the motion event
         * @param gestureResult the gesture result
         * @param fragment the parent fragment
         * @param calendarQuarter the calendar data class
         * @param notePage current notePage
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, gestureResult: Int,
            fragment: CalendarQuarterFragment, calendarQuarter: CalendarQuarter, notePage: String
        ): Boolean {
            if (motionEvent.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) return true

            val year = calendarQuarter.year
            val quarter = calendarQuarter.quarter
            val startMonth = (quarter - 1) * 3 + 1

            val localDate = LocalDate.of(year, startMonth, 1)

            val page = notePage.toIntOrNull() ?: 0
            when (gestureResult) {
                OnGestureListener.UTD -> {
                    if (page == 0) {
                        CalendarNavigator.toQuarterPage(fragment, localDate)
                    } else {
                        CalendarNavigator.toQuarterNote(fragment, localDate, "${page - 1}")
                    }
                    return true
                }

                OnGestureListener.DTU -> {
                    CalendarNavigator.toQuarterNote(fragment, localDate, "${page + 1}")
                    return true
                }
            }

            return true
        }

        /**
         * Draw the quarterly template of calendar plugin notes.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarQuarter data class
         * @param template the template code
         * @param notePage current notePage
         */
        fun drawPage(context: Context, canvas: Canvas, calendarQuarter: CalendarQuarter, template: Int, notePage: String) {
            val page = notePage.toIntOrNull() ?: 0

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            canvas.drawText("${page + 1}", lo + cew - 10.0f, to + 3 * ceh - 10.0f, Creator.textBigGray20Right)

            if (template == 0) {
                canvas.drawLine(lo, to + 0 * ceh, lo + cew, to + 0 * ceh, Creator.lineDefaultBlack)
                for (i in 1..34) {
                    canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
                }
                canvas.drawLine(lo, to + 35 * ceh, lo + cew, to + 35 * ceh, Creator.lineDefaultBlack)
            } else if (template == 1) {
                canvas.drawLine(lo, to + 0 * ceh, lo + cew, to + 0 * ceh, Creator.lineDefaultBlack)
                for (i in 1..34) {
                    canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
                }
                canvas.drawLine(lo, to + 35 * ceh, lo + cew, to + 35 * ceh, Creator.lineDefaultBlack)

                canvas.drawLine(lo, to + 0 * ceh, lo, to + 35 * ceh, Creator.lineDefaultBlack)
                for (i in 1..25) {
                    canvas.drawLine(lo + i * 50.0f, to + 0 * ceh, lo + i * 50.0f, to + 35 * ceh, Creator.lineDefaultGrey50)
                }
                canvas.drawLine(lo + 26 * 50.0f, to + 0 * ceh, lo + 26 * 50.0f, to + 35 * ceh, Creator.lineDefaultBlack)
            }
        }
    }
}

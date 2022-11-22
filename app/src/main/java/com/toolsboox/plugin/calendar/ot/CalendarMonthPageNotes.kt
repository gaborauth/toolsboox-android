package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.toolsboox.ot.Creator
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v1.CalendarMonth
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.ui.CalendarMonthFragment
import java.time.LocalDate
import java.time.temporal.WeekFields

/**
 * Create monthly template of calendar plugin notes.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarMonthPageNotes : Creator {

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
         * @param calendarMonth the calendar data class
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, gestureResult: Int,
            fragment: CalendarMonthFragment, calendarMonth: CalendarMonth
        ): Boolean {
            if (motionEvent.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) return true

            val year = calendarMonth.year
            val month = calendarMonth.month
            val locale = calendarMonth.locale
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek.value

            val localDate = LocalDate.of(year, month, 1)

            when (gestureResult) {
                OnGestureListener.UTD -> {
                    CalendarNavigator.toMonth(fragment, localDate, false)
                    return true
                }

                OnGestureListener.DTU -> {
                    return true
                }
            }

            return true
        }

        /**
         * Draw the monthly template of calendar plugin notes.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarMonth data class
         * @param calendarPattern pattern data class
         */
        fun drawPage(context: Context, canvas: Canvas, calendarMonth: CalendarMonth, calendarPattern: CalendarPattern) {
            val text1 = "What do you want to write here?"
            val text2 = "Sketch a template and send it to me... :)"
            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            canvas.drawRect(lo, to, lo + cew, to + ceh, Creator.fillGrey80)
            canvas.drawText(text1, lo + 10.0f, to + ceh - 10.0f, Creator.textDefaultWhite)

            canvas.drawRect(lo, to + 34 * ceh, lo + cew, to + 35 * ceh, Creator.fillGrey80)
            canvas.drawText(text2, lo + 10.0f, to + 35 * ceh - 10.0f, Creator.textDefaultWhite)

            canvas.drawLine(lo, to + 0 * ceh, lo + cew, to + 0 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(lo, to + 1 * ceh, lo + cew, to + 1 * ceh, Creator.lineDefaultBlack)
            for (i in 2..34) {
                canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
            }
            canvas.drawLine(lo, to + 35 * ceh, lo + cew, to + 35 * ceh, Creator.lineDefaultBlack)
        }
    }
}

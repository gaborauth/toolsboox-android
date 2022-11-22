package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.toolsboox.ot.Creator
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v1.CalendarMonth
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.ui.CalendarMonthFragment
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

/**
 * Create navigator of monthly template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarMonthNavigator {

    companion object {
        // Cell width
        private const val cew = 65.0f

        // Cell height
        private const val ceh = 120.0f

        // Left offset
        private const val lo = (1404.0f - 20 * cew) / 2.0f

        // Top offset
        private const val to = (140.4f - 1 * ceh) / 2.0f

        /**
         * Process touch event on the calendar navigator and navigate to the view of calendar.
         *
         * @param view the surface view
         * @param motionEvent the motion event
         * @param fragment the parent fragment
         * @param calendarMonth the calendar data class
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, fragment: CalendarMonthFragment, calendarMonth: CalendarMonth
        ): Boolean {
            val year = calendarMonth.year
            val month = calendarMonth.month

            val localDate = LocalDate.of(year, month, 1)

            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    val px = motionEvent.x * 1404.0f / view.width
                    val py = motionEvent.y * 140.4f / view.height

                    if (px >= lo + 0 * cew && px <= lo + 1 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toMonth(fragment, localDate.minusMonths(1L), false)
                        return true
                    }
                    if (px >= lo + 1 * cew && px <= lo + 3 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toDay(fragment, LocalDate.now(), false)
                        return true
                    }
                    if (px >= lo + 9 * cew && px <= lo + 13 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toMonth(fragment, localDate, false)
                        return true
                    }
                    if (px >= lo + 13 * cew && px <= lo + 15 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toQuarter(fragment, localDate, false)
                        return true
                    }
                    if (px >= lo + 15 * cew && px <= lo + 19 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toYear(fragment, localDate, false)
                        return true
                    }
                    if (px >= lo + 19 * cew && px <= lo + 20 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toMonth(fragment, localDate.plusMonths(1L), false)
                        return true
                    }
                }
            }

            return true
        }

        /**
         * Draw the navigator of monthly template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarMonth data class
         * @param calendarPattern the calendar pattern
         */
        fun draw(context: Context, canvas: Canvas, calendarMonth: CalendarMonth, calendarPattern: CalendarPattern) {
            canvas.drawRect(0.0f, 0.0f, 1404.0f, 140.4f, Creator.fillWhite)

            canvas.drawLine(0.0f, 138.4f, 1404.0f, 138.4f, Creator.lineDefaultBlack)
            canvas.drawLine(0.0f, 136.4f, 1404.0f, 136.4f, Creator.lineDefaultBlack)

            val year = calendarMonth.year
            val monthOfYear = calendarMonth.month

            val localDate = LocalDate.of(year, monthOfYear, 1)

            val monthName = localDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val quarterOfYear = (localDate.monthValue - 1) / 3 + 1

            canvas.drawRect(lo + 0 * cew, to + 0 * ceh, lo + 1 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 0 * cew, to + 0 * ceh, lo + 1 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            canvas.drawText("<", lo + 0 * cew + cew / 2, to + 1 * ceh - 30.0f, Creator.textBigBlackCenter)

            canvas.drawRect(lo + 1 * cew, to + 0 * ceh, lo + 3 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 1 * cew, to + 0 * ceh, lo + 3 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            Creator.drawEllipsizedText(
                canvas, "⏎", Creator.textBigBlackCenter, lo + 1 * cew, to + 1 * ceh - 30.0f, 2 * cew
            )

            canvas.drawRect(lo + 3 * cew, to + 0 * ceh, lo + 6 * cew, to + 1 * ceh, Creator.lineDefaultBlack)

            canvas.drawRect(lo + 6 * cew, to + 0 * ceh, lo + 9 * cew, to + 1 * ceh, Creator.lineDefaultBlack)

            canvas.drawRect(lo + 9 * cew, to + 0 * ceh, lo + 13 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 9 * cew, to + 0 * ceh, lo + 13 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            Creator.drawEllipsizedText(
                canvas, monthName, Creator.textBigBlackCenter, lo + 9 * cew, to + 1 * ceh - 30.0f, 4 * cew
            )

            if (calendarPattern.getMonthPages(monthOfYear) > 0) {
                Creator.drawTriangle(canvas, lo + 9 * cew, to + 0 * ceh, 20.0f)
            }
            if (calendarPattern.getMonthNotes(monthOfYear) > 0) {
                Creator.drawCircle(canvas, lo + 9 * cew + 10.0f, to + 1 * ceh - 10.0f, 5.0f)
            }

            canvas.drawRect(lo + 13 * cew, to + 0 * ceh, lo + 15 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 13 * cew, to + 0 * ceh, lo + 15 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            Creator.drawEllipsizedText(
                canvas, "Q$quarterOfYear", Creator.textBigBlackCenter, lo + 13 * cew, to + 1 * ceh - 30.0f, 2 * cew
            )

            if (calendarPattern.getQuarterPages(quarterOfYear) > 0) {
                Creator.drawTriangle(canvas, lo + 13 * cew, to + 0 * ceh, 20.0f)
            }
            if (calendarPattern.getQuarterNotes(quarterOfYear) > 0) {
                Creator.drawCircle(canvas, lo + 13 * cew + 10.0f, to + 1 * ceh - 10.0f, 5.0f)
            }

            canvas.drawRect(lo + 15 * cew, to + 0 * ceh, lo + 19 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 15 * cew, to + 0 * ceh, lo + 19 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            Creator.drawEllipsizedText(
                canvas, "$year", Creator.textBigBlackCenter, lo + 15 * cew, to + 1 * ceh - 30.0f, 4 * cew
            )

            if (calendarPattern.getYearPages() > 0) {
                Creator.drawTriangle(canvas, lo + 15 * cew, to + 0 * ceh, 20.0f)
            }
            if (calendarPattern.getYearNotes() > 0) {
                Creator.drawCircle(canvas, lo + 15 * cew + 10.0f, to + 1 * ceh - 10.0f, 5.0f)
            }

            canvas.drawRect(lo + 19 * cew, to + 0 * ceh, lo + 20 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 19 * cew, to + 0 * ceh, lo + 20 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            canvas.drawText(">", lo + 19 * cew + cew / 2, to + 1 * ceh - 30.0f, Creator.textBigBlackCenter)
        }
    }
}

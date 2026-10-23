package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.toolsboox.R
import com.toolsboox.ot.Creator
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v2.CalendarMonth
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
                        CalendarNavigator.toMonthPage(fragment, localDate.minusMonths(1L))
                        return true
                    }
                    if (px >= lo + 1 * cew && px <= lo + 3 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toDayPage(fragment, LocalDate.now())
                        return true
                    }
                    if (px >= lo + 9 * cew && px <= lo + 13 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toMonthPage(fragment, localDate)
                        return true
                    }
                    if (px >= lo + 13 * cew && px <= lo + 15 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toQuarterPage(fragment, localDate)
                        return true
                    }
                    if (px >= lo + 15 * cew && px <= lo + 19 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toYearPage(fragment, localDate)
                        return true
                    }
                    if (px >= lo + 19 * cew && px <= lo + 20 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toMonthPage(fragment, localDate.plusMonths(1L))
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
            val year = calendarMonth.year
            val monthOfYear = calendarMonth.month

            val localDate = LocalDate.of(year, monthOfYear, 1)

            val monthName = localDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val quarterOfYear = (localDate.monthValue - 1) / 3 + 1

            NavigatorRenderer.render(context, canvas, listOf(
                NavigatorRenderer.Slot(monthName, NavigatorRenderer.Emphasis.FOCAL,
                    calendarPattern.getMonthPages(monthOfYear) > 0, calendarPattern.getMonthNotes(monthOfYear)),
                NavigatorRenderer.Slot(context.getString(R.string.quarter_abbreviation, quarterOfYear),
                    NavigatorRenderer.Emphasis.NORMAL,
                    calendarPattern.getQuarterPages(quarterOfYear) > 0, calendarPattern.getQuarterNotes(quarterOfYear)),
                NavigatorRenderer.Slot("$year", NavigatorRenderer.Emphasis.MUTED,
                    calendarPattern.getYearPages() > 0, calendarPattern.getYearNotes()),
            ))
        }
    }
}

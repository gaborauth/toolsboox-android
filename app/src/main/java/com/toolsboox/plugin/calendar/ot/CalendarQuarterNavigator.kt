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
import com.toolsboox.plugin.calendar.da.v2.CalendarQuarter
import com.toolsboox.plugin.calendar.ui.CalendarQuarterFragment
import java.time.LocalDate

/**
 * Create navigator of quarterly template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarQuarterNavigator {

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
         * @param calendarQuarter the calendar data class
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, fragment: CalendarQuarterFragment, calendarQuarter: CalendarQuarter
        ): Boolean {
            val year = calendarQuarter.year
            val quarter = calendarQuarter.quarter
            val startMonth = (quarter - 1) * 3 + 1

            val localDate = LocalDate.of(year, startMonth, 1)

            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    val px = motionEvent.x * 1404.0f / view.width
                    val py = motionEvent.y * 140.4f / view.height

                    if (px >= lo + 0 * cew && px <= lo + 1 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toQuarterPage(fragment, localDate.minusMonths(3L))
                        return true
                    }
                    if (px >= lo + 1 * cew && px <= lo + 3 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toDayPage(fragment, LocalDate.now())
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
                        CalendarNavigator.toQuarterPage(fragment, localDate.plusMonths(3L))
                        return true
                    }
                }
            }

            return true
        }

        /**
         * Draw the navigator of quarterly template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarQuarter data class
         * @param calendarPattern the calendar pattern
         */
        fun draw(context: Context, canvas: Canvas, calendarQuarter: CalendarQuarter, calendarPattern: CalendarPattern) {
            val year = calendarQuarter.year
            val quarterOfYear = calendarQuarter.quarter

            NavigatorRenderer.render(context, canvas, listOf(
                NavigatorRenderer.Slot(context.getString(R.string.quarter_abbreviation, quarterOfYear),
                    NavigatorRenderer.Emphasis.FOCAL,
                    calendarPattern.getQuarterPages(quarterOfYear) > 0, calendarPattern.getQuarterNotes(quarterOfYear)),
                NavigatorRenderer.Slot("$year", NavigatorRenderer.Emphasis.MUTED,
                    calendarPattern.getYearPages() > 0, calendarPattern.getYearNotes()),
            ))
        }
    }
}

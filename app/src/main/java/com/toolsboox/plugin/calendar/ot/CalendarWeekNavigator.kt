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
import com.toolsboox.plugin.calendar.da.v2.CalendarWeek
import com.toolsboox.plugin.calendar.ui.CalendarWeekFragment
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

/**
 * Create navigator of weekly template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarWeekNavigator {

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
         * @param calendarWeek the calendar data class
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, fragment: CalendarWeekFragment, calendarWeek: CalendarWeek
        ): Boolean {
            val year = calendarWeek.year
            val weekOfYear = calendarWeek.weekOfYear
            val locale = calendarWeek.locale

            val weekFields = WeekFields.of(locale)
            val localDate = LocalDate.ofYearDay(year, 1)
                .with(weekFields.weekOfYear(), weekOfYear.toLong())
                .with(weekFields.dayOfWeek(), 1)

            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    val px = motionEvent.x * 1404.0f / view.width
                    val py = motionEvent.y * 140.4f / view.height

                    if (px >= lo + 0 * cew && px <= lo + 1 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toWeekPage(fragment, localDate.minusWeeks(1L), locale)
                        return true
                    }
                    if (px >= lo + 1 * cew && px <= lo + 3 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toDayPage(fragment, LocalDate.now())
                        return true
                    }
                    if (px >= lo + 6 * cew && px <= lo + 9 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toWeekPage(fragment, localDate, locale)
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
                        CalendarNavigator.toWeekPage(fragment, localDate.plusWeeks(1L), locale)
                        return true
                    }
                }
            }

            return true
        }

        /**
         * Draw the navigator of weekly template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarWeek data class
         * @param calendarPattern the calendar pattern
         */
        fun draw(context: Context, canvas: Canvas, calendarWeek: CalendarWeek, calendarPattern: CalendarPattern) {
            val year = calendarWeek.year
            val weekOfYear = calendarWeek.weekOfYear
            val locale = calendarWeek.locale

            val weekFields = WeekFields.of(locale)
            val localDate = LocalDate.ofYearDay(year, 1)
                .with(weekFields.weekOfYear(), weekOfYear.toLong())
                .with(weekFields.dayOfWeek(), 1)

            val monthOfYear = localDate.monthValue
            val monthName = localDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val quarterOfYear = (localDate.monthValue - 1) / 3 + 1
            val day = localDate.dayOfMonth
            val dayOfYear = localDate.dayOfYear
            val dayOfWeek = localDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

            NavigatorRenderer.render(context, canvas, listOf(
                NavigatorRenderer.Slot("$day", NavigatorRenderer.Emphasis.NORMAL,
                    calendarPattern.getDayPages(dayOfYear) > 0, calendarPattern.getDayNotes(dayOfYear)),
                NavigatorRenderer.Slot(dayOfWeek, NavigatorRenderer.Emphasis.MUTED, false, 0),
                NavigatorRenderer.Slot(context.getString(R.string.week_abbreviation, weekOfYear),
                    NavigatorRenderer.Emphasis.FOCAL,
                    calendarPattern.getWeekPages(weekOfYear) > 0, calendarPattern.getWeekNotes(weekOfYear)),
                NavigatorRenderer.Slot(monthName, NavigatorRenderer.Emphasis.NORMAL,
                    calendarPattern.getMonthPages(monthOfYear) > 0, calendarPattern.getMonthNotes(monthOfYear)),
                NavigatorRenderer.Slot(context.getString(R.string.quarter_abbreviation, quarterOfYear),
                    NavigatorRenderer.Emphasis.MUTED,
                    calendarPattern.getQuarterPages(quarterOfYear) > 0, calendarPattern.getQuarterNotes(quarterOfYear)),
                NavigatorRenderer.Slot("$year", NavigatorRenderer.Emphasis.MUTED,
                    calendarPattern.getYearPages() > 0, calendarPattern.getYearNotes()),
            ))
        }
    }
}

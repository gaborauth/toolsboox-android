package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.toolsboox.ot.Creator
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v1.CalendarQuarter
import com.toolsboox.plugin.calendar.ui.CalendarQuarterFragment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

/**
 * Create quarterly template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarQuarterPage : Creator {

    companion object {

        // Cell width
        private const val cew = 420.0f

        // Cell height
        private const val ceh = 55.0f

        // Left offset
        private const val lo = (1404.0f - 3 * cew - 100.0f) / 2.0f

        // Top offset
        private const val to = (1872.0f - 32 * ceh) / 2.0f

        /**
         * Process touch event on the calendar page and navigate to the view of calendar.
         *
         * @param view the surface view
         * @param motionEvent the motion event
         * @param gestureResult the gesture result
         * @param fragment the parent fragment
         * @param calendarQuarter the calendar data class
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, gestureResult: Int,
            fragment: CalendarQuarterFragment, calendarQuarter: CalendarQuarter
        ): Boolean {
            if (motionEvent.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) return true

            val year = calendarQuarter.year
            val quarter = calendarQuarter.quarter
            val startMonth = (quarter - 1) * 3 + 1

            val localDate = LocalDate.of(year, startMonth, 1)

            when (gestureResult) {
                OnGestureListener.LTR -> {
                    CalendarNavigator.toQuarter(fragment, localDate.minusMonths(3L), false)
                    return true
                }

                OnGestureListener.RTL -> {
                    CalendarNavigator.toQuarter(fragment, localDate.plusMonths(3L), false)
                    return true
                }

                OnGestureListener.UTD -> {
                    CalendarNavigator.toYear(fragment, localDate, false)
                    return true
                }

                OnGestureListener.DTU -> {
                    CalendarNavigator.toQuarter(fragment, localDate, true)
                    return true
                }
            }

            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    val px = motionEvent.x * 1404.0f / view.width
                    val py = motionEvent.y * 1872.0f / view.height

                    for (i in 0..2) {
                        val xo = lo + i * cew + i * 50.0f
                        val yo = to

                        if (px >= xo && px <= xo + cew && py >= yo && py <= yo + ceh) {
                            CalendarNavigator.toMonthPage(fragment, localDate.plusMonths(i.toLong()))
                            return true
                        }

                        val yearMonth = YearMonth.of(year, startMonth)
                        val days = yearMonth.month.length(yearMonth.isLeapYear)
                        for (day in 1..days) {
                            val dyo = yo + day * ceh
                            if (px >= xo && px <= xo + cew && py >= dyo && py <= dyo + ceh) {
                                CalendarNavigator.toDayPage(fragment, LocalDate.of(year, startMonth + i, day))
                                return true
                            }
                        }
                    }
                }
            }

            return true
        }

        /**
         * Draw the quarterly template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarQuarter data class
         * @param calendarPattern pattern data class
         */
        fun drawPage(
            context: Context, canvas: Canvas, calendarQuarter: CalendarQuarter, calendarPattern: CalendarPattern
        ) {
            val year = calendarQuarter.year
            val quarter = calendarQuarter.quarter
            val locale = calendarQuarter.locale

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            val startMonth = (quarter - 1) * 3 + 1
            for (i in 0..2) {
                drawMonthGrid(canvas, lo + i * cew + i * 50.0f, to + 1 * ceh, year, startMonth + i, calendarPattern)
                drawMonthName(canvas, lo + i * cew + i * 50.0f, to + 1 * ceh, startMonth + i, calendarPattern)
            }
        }

        /**
         * Draw the grid of the month.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param year the current year
         * @param month the current month
         * @param calendarPattern pattern data class
         */
        private fun drawMonthGrid(
            canvas: Canvas, lo: Float, to: Float, year: Int, month: Int, calendarPattern: CalendarPattern
        ) {
            val yearMonth = YearMonth.of(year, month)
            val days = yearMonth.month.length(yearMonth.isLeapYear)
            val dayOfYearBase = LocalDate.of(year, month, 1).dayOfYear

            canvas.drawLine(lo, to, lo + cew, to, Creator.lineDefaultBlack)
            canvas.drawLine(lo + 75.0f, to, lo + 75.0f, to + days * ceh, Creator.lineDefaultBlack)

            for (i in 1..days) {
                canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
                val day = LocalDate.of(year, month, i)
                val dayOfYear = dayOfYearBase + i - 1

                val locale = Locale.getDefault()
                val dayOfWeek = (day.dayOfWeek.value - 1) % 7 + 1
                if (dayOfWeek == DayOfWeek.SATURDAY.value || dayOfWeek == DayOfWeek.SUNDAY.value) {
                    canvas.drawRect(lo, to + i * ceh - ceh, lo + cew, to + i * ceh, Creator.fillGrey20)
                }

                when (dayOfWeek) {
                    DayOfWeek.MONDAY.value -> DayOfWeek.MONDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.TUESDAY.value -> DayOfWeek.TUESDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.WEDNESDAY.value -> DayOfWeek.WEDNESDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.THURSDAY.value -> DayOfWeek.THURSDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.FRIDAY.value -> DayOfWeek.FRIDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.SATURDAY.value -> DayOfWeek.SATURDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.SUNDAY.value -> DayOfWeek.SUNDAY.getDisplayName(TextStyle.NARROW, locale)
                    else -> "?"
                }.let { dayOfWeekText ->
                    canvas.drawText("$i", lo + 10.0f, to + i * ceh - 15.0f, Creator.textSmallBlack)
                    canvas.drawText(dayOfWeekText, lo + 65.0f, to + i * ceh - 15.0f, Creator.textSmallBlackRight)
                }

                if (calendarPattern.getDayPages(dayOfYear) > 0) {
                    Creator.drawTriangle(canvas, lo + 77.0f, to + (i - 1) * ceh, 10.0f)
                }
                if (calendarPattern.getDayNotes(dayOfYear) > 0) {
                    Creator.drawCircle(canvas, lo + 80.0f, to + (i - 1) * ceh - 5.0f, 2.5f)
                }
            }
        }

        /**
         * Draw the grid of the month.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param month the current month
         * @param calendarPattern pattern data class
         */
        private fun drawMonthName(canvas: Canvas, lo: Float, to: Float, month: Int, calendarPattern: CalendarPattern) {
            canvas.drawRect(lo, to - ceh, lo + cew, to, Creator.fillGrey80)

            val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
            canvas.drawText(monthName, lo + cew / 2.0f, to - 10.0f, Creator.textDefaultWhiteCenter)

            if (calendarPattern.getMonthPages(month) > 0) {
                Creator.drawTriangle(canvas, lo + 2.0f, to - ceh + 2.0f, 20.0f, Creator.fillWhite)
            }
            if (calendarPattern.getMonthNotes(month) > 0) {
                Creator.drawCircle(canvas, lo + 10.0f, to - 10.0f, 5.0f, Creator.fillWhite)
            }
        }
    }
}
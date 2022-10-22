package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import com.toolsboox.ot.Creator
import com.toolsboox.plugin.calendar.da.CalendarQuarter
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
class CalendarQuarterCreator : Creator {

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
         * Draw the quarterly template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarQuarter data class
         */
        fun drawPage(context: Context, canvas: Canvas, calendarQuarter: CalendarQuarter) {
            val year = calendarQuarter.year
            val quarter = calendarQuarter.quarter
            val locale = calendarQuarter.locale ?: Locale.getDefault()

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            val startMonth = (quarter - 1) * 3 + 1
            for (i in 0..2) {
                drawMonthGrid(canvas, lo + i * cew + i * 50.0f, to + 1 * ceh, year, startMonth + i)
                drawMonthName(canvas, lo + i * cew + i * 50.0f, to + 1 * ceh, startMonth + i)
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
         */
        private fun drawMonthGrid(canvas: Canvas, lo: Float, to: Float, year: Int, month: Int) {
            val yearMonth = YearMonth.of(year, month)
            val days = yearMonth.month.length(yearMonth.isLeapYear)

            canvas.drawLine(lo, to, lo + cew, to, Creator.lineDefaultBlack)
            canvas.drawLine(lo + 75.0f, to, lo + 75.0f, to + days * ceh, Creator.lineDefaultBlack)

            for (i in 1..days) {
                canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
                val day = LocalDate.of(year, month, i)

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
                    canvas.drawText("${i}", lo + 10.0f, to + i * ceh - 15.0f, Creator.textSmallBlack)
                    canvas.drawText(dayOfWeekText, lo + 65.0f, to + i * ceh - 15.0f, Creator.textSmallBlackRight)
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
         */
        private fun drawMonthName(canvas: Canvas, lo: Float, to: Float, month: Int) {
            canvas.drawRect(lo, to - ceh, lo + cew, to, Creator.fillGrey80)

            val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
            canvas.drawText(monthName, lo + cew / 2.0f, to - 10.0f, Creator.textDefaultWhiteCenter)
        }
    }
}
package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import com.toolsboox.ot.Creator
import com.toolsboox.plugin.calendar.da.CalendarMonth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

/**
 * Create monthly template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarMonthCreator : Creator {

    companion object {
        // Cell width
        private const val cew = 180.0f

        // Cell height
        private const val ceh = 280.0f

        // Left offset
        private const val lo = (1404.0f - 7 * cew - 50.0f) / 2.0f

        // Top offset
        private const val to = (1872.0f - 6 * ceh - 50.0f) / 2.0f

        /**
         * Draw the monthly template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarMonth data class
         */
        fun drawPage(context: Context, canvas: Canvas, calendarMonth: CalendarMonth) {
            val year = calendarMonth.year
            val month = calendarMonth.month
            val locale = calendarMonth.locale ?: Locale.getDefault()
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek.value

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            drawGrid(canvas)

            for (i in 0..6) {
                val dayOfWeekNumber = (firstDayOfWeek + i - 1) % 7 + 1
                drawDayOfWeekNames(canvas, lo + 50.0f + i * cew, to, dayOfWeekNumber)
            }

            val currentDate = LocalDate.of(calendarMonth.year, calendarMonth.month, 1)
            val weekOfYear = WeekFields.of(locale).weekOfWeekBasedYear()

            for (i in 0..5) {
                val weekOfYearText = "W${currentDate.plusWeeks(i.toLong()).get(weekOfYear)}"
                drawWeekNames(canvas, lo, to + 50.0f + i * ceh, weekOfYearText)
            }

            drawDayNumbers(canvas, locale, lo + 50.0f, to + 50.0f, year, month)
        }

        /**
         * Draw day number of the month.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param year the current year
         * @param month the current month
         */
        private fun drawDayNumbers(canvas: Canvas, locale: Locale, lo: Float, to: Float, year: Int, month: Int) {
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek.value

            val yearMonth = YearMonth.of(year, month)
            val dayValue: Int = LocalDate.of(year, month, 1).dayOfWeek.value
            var xOffset = (dayValue - firstDayOfWeek + 7) % 7
            var yOffset = 0

            for (day in 1..yearMonth.month.length(yearMonth.isLeapYear)) {
                val x = lo + xOffset * cew + cew - 10.0f
                val y = to + yOffset * ceh + 40.0f
                canvas.drawText("$day", x, y, Creator.textDefaultBlackRight)

                xOffset++
                if (xOffset > 6) {
                    xOffset = 0
                    yOffset++
                }
            }
        }

        /**
         * Draw the day of week names of the month.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param dayOfWeekNumber the number of the day of week
         */
        private fun drawDayOfWeekNames(canvas: Canvas, lo: Float, to: Float, dayOfWeekNumber: Int) {
            canvas.drawRect(lo - 50.0f, to, lo + cew, to + 50.0f, Creator.fillGrey80)
            val locale = Locale.getDefault()
            val displayName = when (dayOfWeekNumber) {
                DayOfWeek.MONDAY.value -> DayOfWeek.MONDAY.getDisplayName(TextStyle.SHORT, locale)
                DayOfWeek.TUESDAY.value -> DayOfWeek.TUESDAY.getDisplayName(TextStyle.SHORT, locale)
                DayOfWeek.WEDNESDAY.value -> DayOfWeek.WEDNESDAY.getDisplayName(TextStyle.SHORT, locale)
                DayOfWeek.THURSDAY.value -> DayOfWeek.THURSDAY.getDisplayName(TextStyle.SHORT, locale)
                DayOfWeek.FRIDAY.value -> DayOfWeek.FRIDAY.getDisplayName(TextStyle.SHORT, locale)
                DayOfWeek.SATURDAY.value -> DayOfWeek.SATURDAY.getDisplayName(TextStyle.SHORT, locale)
                DayOfWeek.SUNDAY.value -> DayOfWeek.SUNDAY.getDisplayName(TextStyle.SHORT, locale)
                else -> "?"
            }

            canvas.drawText(displayName, lo + cew / 2.0f, to + 40.0f, Creator.textDefaultWhiteCenter)
            if (dayOfWeekNumber == DayOfWeek.SATURDAY.value || dayOfWeekNumber == DayOfWeek.SUNDAY.value) {
                canvas.drawRect(lo, to + 50.0f, lo + cew, to + 50.0f + 6 * ceh, Creator.fillGrey20)
            }
        }

        /**
         * Draw the grid of the month.
         *
         * @param canvas the canvas
         */
        private fun drawGrid(canvas: Canvas) {
            for (i in 0..6) {
                canvas.drawLine(
                    lo, to + 50.0f + i * ceh, lo + 50.0f + 7 * cew, to + 50.0f + i * ceh,
                    Creator.lineDefaultBlack
                )
            }

            for (i in 0..7) {
                canvas.drawLine(
                    lo + 50.0f + i * cew, to, lo + 50.0f + i * cew, to + 50.0f + 6 * ceh,
                    Creator.lineDefaultBlack
                )
            }
        }

        /**
         * Draw the week names.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param weekOfYear the week of year
         */
        private fun drawWeekNames(canvas: Canvas, lo: Float, to: Float, weekOfYear: String) {
            canvas.drawRect(lo, to, lo + 50.0f, to + ceh, Creator.fillGrey80)
            canvas.save()
            canvas.rotate(-90.0f, lo + 45.0f, to + ceh / 2.0f)
            canvas.drawText(weekOfYear, lo + 45.0f, to + ceh / 2.0f - 5.0f, Creator.textDefaultWhiteCenter)
            canvas.restore()
        }
    }
}
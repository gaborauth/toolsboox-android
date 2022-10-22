package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import com.toolsboox.ot.Creator
import com.toolsboox.plugin.calendar.da.CalendarYear
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

/**
 * Create yearly template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarYearCreator : Creator {

    companion object {

        // Cell width
        private const val cew = 50.0f

        // Cell height
        private const val ceh = 50.0f

        // Left offset
        private const val lo = (1404.0f - 27 * cew) / 2.0f

        // Top offset
        private const val to = (1872.0f - 35 * ceh) / 2.0f

        /**
         * Draw the yearly template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarYear data class
         */
        fun drawPage(context: Context, canvas: Canvas, calendarYear: CalendarYear) {
            val year = calendarYear.year
            val locale = calendarYear.locale ?: Locale.getDefault()
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek.value

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            for (x in 0..2) {
                for (y in 0..3) {
                    val monthNumber = y * 3 + x + 1
                    val quarterNumber = y + 1

                    val xo = lo + cew + x * 9 * cew
                    val yo = to + y * 9 * ceh

                    drawQuarter(canvas, xo - 50.0f, yo, quarterNumber)

                    drawMonthGrid(canvas, xo, yo, firstDayOfWeek)
                    drawMonthName(canvas, xo, yo, monthNumber)

                    drawDayNumbers(canvas, locale, xo, yo, year, monthNumber)
                    drawDayNames(canvas, xo, yo, firstDayOfWeek)
                }
            }
        }

        /**
         * Draw day names.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param firstDayOfWeek first day of the week
         */
        private fun drawDayNames(canvas: Canvas, lo: Float, to: Float, firstDayOfWeek: Int) {
            val x = lo + cew / 2.0f
            val y = to + 2 * ceh - 10.0f
            canvas.drawText("W", x, y, Creator.textDefaultBlackCenter)

            var offset = cew
            for (d in firstDayOfWeek..firstDayOfWeek + 6) {
                val locale = Locale.getDefault()
                val dayOfWeekText = when ((d - 1) % 7 + 1) {
                    DayOfWeek.MONDAY.value -> DayOfWeek.MONDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.TUESDAY.value -> DayOfWeek.TUESDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.WEDNESDAY.value -> DayOfWeek.WEDNESDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.THURSDAY.value -> DayOfWeek.THURSDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.FRIDAY.value -> DayOfWeek.FRIDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.SATURDAY.value -> DayOfWeek.SATURDAY.getDisplayName(TextStyle.NARROW, locale)
                    DayOfWeek.SUNDAY.value -> DayOfWeek.SUNDAY.getDisplayName(TextStyle.NARROW, locale)
                    else -> "?"
                }
                canvas.drawText(dayOfWeekText, x + offset, y, Creator.textDefaultBlackCenter)
                offset += cew
            }
        }

        /**
         * Draw day and week numbers.
         *
         * @param canvas the canvas
         * @param locale the stored locale
         * @param lo the left offset
         * @param to the top offset
         * @param year the current year
         * @param month the current month
         * @param firstDayOfWeek first day of the week
         */
        private fun drawDayNumbers(canvas: Canvas, locale: Locale, le: Float, to: Float, year: Int, month: Int) {
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek.value
            val weekOfYear = WeekFields.of(locale).weekOfWeekBasedYear()

            val yearMonth = YearMonth.of(year, month)
            val dayValue: Int = LocalDate.of(year, month, 1).dayOfWeek.value
            var xOffset = (dayValue - firstDayOfWeek + 7) % 7
            var yOffset = 3
            var firstInRow = true

            for (day in 1..yearMonth.month.length(yearMonth.isLeapYear)) {
                val x = le + xOffset * cew + cew + cew / 2.0f
                val y = to + yOffset * ceh - 15.0f
                canvas.drawText("$day", x, y, Creator.textSmallBlackCenter)

                if (firstInRow) {
                    val weekNumber = LocalDate.of(year, month, day).get(weekOfYear)
                    canvas.drawText("$weekNumber", le + cew / 2.0f, y, Creator.textSmallBlackCenter)
                    firstInRow = false
                }

                xOffset++
                if (xOffset > 6) {
                    xOffset = 0
                    yOffset++
                    firstInRow = true
                }
            }
        }

        /**
         * Draw month grid and backgrounds.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
        +         */
        private fun drawMonthGrid(canvas: Canvas, lo: Float, to: Float, firstDayOfWeek: Int) {
            canvas.drawRect(lo, to + 0 * ceh, lo + 8 * cew, to + 1 * ceh, Creator.fillGrey80)
            canvas.drawRect(lo, to + 1 * ceh, lo + 8 * cew, to + 2 * ceh, Creator.fillGrey20)
            canvas.drawLine(lo, to + 0 * ceh, lo + 8 * cew, to + 0 * ceh, Creator.lineDefaultBlack)
            for (i in 1..2) {
                canvas.drawLine(lo, to + i * ceh, lo + 8 * cew, to + i * ceh, Creator.lineDefaultBlack)
            }
            for (i in 3..7) {
                canvas.drawLine(lo, to + i * ceh, lo + 8 * cew, to + i * ceh, Creator.lineDefaultGrey50)
            }
            canvas.drawLine(lo, to + 8 * ceh, lo + 8 * cew, to + 8 * ceh, Creator.lineDefaultBlack)

            canvas.drawRect(lo + 0 * cew, to + 1 * ceh, lo + 1 * cew, to + 8 * ceh, Creator.fillGrey20)
            canvas.drawLine(lo + 0 * cew, to + 0 * ceh, lo + 0 * cew, to + 8 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(lo + 1 * cew, to + 1 * ceh, lo + 1 * cew, to + 8 * ceh, Creator.lineDefaultBlack)
            for (i in 2..7) {
                canvas.drawLine(lo + i * cew, to + 1 * ceh, lo + i * cew, to + 8 * ceh, Creator.lineDefaultGrey50)
            }
            canvas.drawLine(lo + 8 * cew, to + 0 * ceh, lo + 8 * cew, to + 8 * ceh, Creator.lineDefaultBlack)

            var offset = cew
            for (d in firstDayOfWeek..firstDayOfWeek + 6) {
                when ((d - 1) % 7 + 1) {
                    DayOfWeek.SATURDAY.value, DayOfWeek.SUNDAY.value -> {
                        canvas.drawRect(lo + offset, to + 1 * ceh, lo + offset + cew, to + 8 * ceh, Creator.fillGrey20)
                    }
                }
                offset += cew
            }
        }

        /**
         * Draw month names.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param monthNumber the month as number
         */
        private fun drawMonthName(canvas: Canvas, lo: Float, to: Float, monthNumber: Int) {
            val monthName = Month.of(monthNumber).getDisplayName(TextStyle.FULL, Locale.getDefault())
            canvas.drawText(monthName, lo + 4 * cew, to + ceh - 10.0f, Creator.textDefaultWhiteCenter)
        }

        /**
         * Draw the quarter background and text.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param quarterNumber the quarter as number
         */
        private fun drawQuarter(canvas: Canvas, lo: Float, to: Float, quarterNumber: Int) {
            canvas.drawRect(lo + 0 * cew, to + 0 * ceh, lo + 1 * cew, to + 8 * ceh, Creator.fillGrey80)
            canvas.save()
            canvas.rotate(-90.0f, lo + cew - 5.0f, to + 4 * ceh)
            canvas.drawText("Q$quarterNumber", lo + cew - 5.0f, to + 4 * ceh - 5.0f, Creator.textDefaultWhiteCenter)
            canvas.restore()
        }
    }
}

package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import com.toolsboox.ot.Creator
import com.toolsboox.plugin.calendar.da.CalendarYear
import java.time.LocalDate
import java.time.YearMonth
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
        private val cew = 50.0f

        // Cell height
        private val ceh = 50.0f

        // Left offset
        private val lo = (1404.0f - 27 * cew) / 2.0f

        // Top offset
        private val to = (1872.0f - 35 * ceh) / 2.0f

        /**
         * Draw the page of the boxed days of month calendar.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarYear data class
         */
        fun drawPage(context: Context, canvas: Canvas, calendarYear: CalendarYear) {
            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            for (x in 0..2) {
                for (y in 0..3) {
                    val xo = lo + cew + x * 9 * cew
                    val yo = to + y * 9 * ceh
                    val monthNumber = y * 3 + x + 1
                    val quarterNumber = y + 1

                    if (x == 0) drawQuarter(canvas, lo, yo, quarterNumber)

                    drawMonthGrid(canvas, xo, yo, calendarYear.startWithDay)
                    drawMonthName(canvas, xo, yo, monthNumber)

                    drawDayNumbers(canvas, xo, yo, calendarYear.year, monthNumber, calendarYear.startWithDay)
                    drawDayNames(canvas, xo, yo, calendarYear.startWithDay)
                }
            }
        }

        /**
         * Draw day names.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param startWithDay the first day in the week
         *
         */
        private fun drawDayNames(canvas: Canvas, lo: Float, to: Float, startWithDay: Int) {
            val x = lo + cew / 2.0f
            val y = to + 2 * ceh - 10.0f
            canvas.drawText("W", x, y, Creator.textDefaultBlackCenter)

            var offset = cew
            for (d in startWithDay - 1..startWithDay + 5) {
                when (d % 7 + 1) {
                    Calendar.SUNDAY -> canvas.drawText("S", x + offset, y, Creator.textDefaultBlackCenter)
                    Calendar.MONDAY -> canvas.drawText("M", x + offset, y, Creator.textDefaultBlackCenter)
                    Calendar.TUESDAY -> canvas.drawText("T", x + offset, y, Creator.textDefaultBlackCenter)
                    Calendar.WEDNESDAY -> canvas.drawText("W", x + offset, y, Creator.textDefaultBlackCenter)
                    Calendar.THURSDAY -> canvas.drawText("T", x + offset, y, Creator.textDefaultBlackCenter)
                    Calendar.FRIDAY -> canvas.drawText("F", x + offset, y, Creator.textDefaultBlackCenter)
                    Calendar.SATURDAY -> canvas.drawText("S", x + offset, y, Creator.textDefaultBlackCenter)
                }
                offset += cew
            }
        }

        /**
         * Draw day and week numbers.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param year the current year
         * @param month the current month
         * @param startWithDay the first day in the week
         */
        private fun drawDayNumbers(canvas: Canvas, le: Float, to: Float, year: Int, month: Int, startWithDay: Int) {
            val yearMonth = YearMonth.of(year, month)
            val dayValue: Int = LocalDate.of(year, month, 1).dayOfWeek.value
            var xOffset = dayValue - startWithDay + 1
            var yOffset = 3
            var firstInRow = true

            val weekOfYear = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
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
         */
        private fun drawMonthGrid(canvas: Canvas, lo: Float, to: Float, startWithDay: Int) {
            canvas.drawRect(lo, to + 0 * ceh, lo + 8 * cew, to + 1 * ceh, Creator.fillGrey80)
            canvas.drawRect(lo, to + 1 * ceh, lo + 8 * cew, to + 2 * ceh, Creator.fillGrey20)
            canvas.drawLine(lo, to + 1 * ceh, lo + 8 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(lo, to + 2 * ceh, lo + 8 * cew, to + 2 * ceh, Creator.lineDefaultBlack)
            for (i in 3..7) {
                canvas.drawLine(lo, to + i * ceh, lo + 8 * cew, to + i * ceh, Creator.lineDefaultGrey50)
            }

            canvas.drawRect(lo + 0 * cew, to + 1 * ceh, lo + 1 * cew, to + 8 * ceh, Creator.fillGrey20)
            canvas.drawLine(lo + 0 * cew, to + 0 * ceh, lo + 0 * cew, to + 8 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(lo + 1 * cew, to + 1 * ceh, lo + 1 * cew, to + 8 * ceh, Creator.lineDefaultBlack)
            for (i in 2..7) {
                canvas.drawLine(lo + i * cew, to + 1 * ceh, lo + i * cew, to + 8 * ceh, Creator.lineDefaultGrey50)
            }
            canvas.drawLine(lo + 8 * cew, to + 0 * ceh, lo + 8 * cew, to + 8 * ceh, Creator.lineDefaultBlack)

            var offset = cew
            for (d in startWithDay..startWithDay + 6) {
                when ((d - 1) % 7 + 1) {
                    Calendar.SATURDAY, Calendar.SUNDAY -> {
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
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.MONTH, monthNumber - 1)

            val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: return
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

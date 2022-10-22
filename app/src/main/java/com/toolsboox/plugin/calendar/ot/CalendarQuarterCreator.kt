package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import com.toolsboox.ot.Creator
import com.toolsboox.plugin.calendar.da.CalendarQuarter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

/**
 * Create quarterly template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarQuarterCreator : Creator {

    companion object {

        // Cell width
        private val cew = 420.0f

        // Cell height
        private val ceh = 55.0f

        // Left offset
        private val lo = (1404.0f - 3 * cew - 100.0f) / 2.0f

        // Top offset
        private val to = (1872.0f - 32 * ceh) / 2.0f

        /**
         * Draw the page of the boxed days of month calendar.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarQuarter data class
         */
        fun drawPage(context: Context, canvas: Canvas, calendarQuarter: CalendarQuarter) {
            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            val startMonth = (calendarQuarter.quarter - 1) * 3 + 1
            for (i in 0..2) {
                drawMonthGrid(canvas, lo + i * cew + i * 50.0f, to + 1 * ceh, calendarQuarter.year, startMonth + i)
                drawMonthName(canvas, lo + i * cew + i * 50.0f, to + 1 * ceh, startMonth + i)
            }
        }

        /**
         * Draw the grid of the month.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param year the year
         * @param monthNumber the month as number
         */
        private fun drawMonthGrid(canvas: Canvas, lo: Float, to: Float, year: Int, monthNumber: Int) {
            val yearMonth = YearMonth.of(year, monthNumber)
            val days = yearMonth.month.length(yearMonth.isLeapYear)

            canvas.drawLine(lo, to, lo + cew, to, Creator.lineDefaultBlack)
            canvas.drawLine(lo + 75.0f, to, lo + 75.0f, to + days * ceh, Creator.lineDefaultBlack)
            for (i in 1..days) {
                canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
                val day = LocalDate.of(year, monthNumber, i)
                when (day.dayOfWeek) {
                    DayOfWeek.MONDAY -> {
                        canvas.drawText("${i} M", lo + 65.0f, to + i * ceh - 15.0f, Creator.textSmallBlackRight)
                    }

                    DayOfWeek.TUESDAY -> {
                        canvas.drawText("${i} T", lo + 65.0f, to + i * ceh - 15.0f, Creator.textSmallBlackRight)
                    }

                    DayOfWeek.WEDNESDAY -> {
                        canvas.drawText("${i} W", lo + 65.0f, to + i * ceh - 15.0f, Creator.textSmallBlackRight)
                    }

                    DayOfWeek.THURSDAY -> {
                        canvas.drawText("${i} T", lo + 65.0f, to + i * ceh - 15.0f, Creator.textSmallBlackRight)
                    }

                    DayOfWeek.FRIDAY -> {
                        canvas.drawText("${i} F", lo + 65.0f, to + i * ceh - 15.0f, Creator.textSmallBlackRight)
                    }

                    DayOfWeek.SATURDAY -> {
                        canvas.drawText("${i} S", lo + 65.0f, to + i * ceh - 15.0f, Creator.textSmallBlackRight)
                        canvas.drawRect(lo, to + i * ceh - ceh, lo + cew, to + i * ceh, Creator.fillGrey20)
                    }

                    DayOfWeek.SUNDAY -> {
                        canvas.drawText("${i} S", lo + 65.0f, to + i * ceh - 15.0f, Creator.textSmallBlackRight)
                        canvas.drawRect(lo, to + i * ceh - ceh, lo + cew, to + i * ceh, Creator.fillGrey20)
                    }

                    else -> {}
                }
            }
        }

        /**
         * Draw the grid of the month.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         * @param year the year
         * @param monthNumber the month as number
         */
        private fun drawMonthName(canvas: Canvas, lo: Float, to: Float, monthNumber: Int) {
            canvas.drawRect(lo, to -ceh, lo + cew, to, Creator.fillGrey80)

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.MONTH, monthNumber - 1)

            val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: return
            canvas.drawText(monthName, lo + cew / 2.0f, to - 10.0f, Creator.textDefaultWhiteCenter)
        }
    }
}
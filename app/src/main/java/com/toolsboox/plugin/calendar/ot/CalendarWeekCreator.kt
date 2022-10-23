package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import com.toolsboox.R
import com.toolsboox.ot.Creator
import com.toolsboox.plugin.calendar.da.CalendarWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

/**
 * Create weekly template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarWeekCreator : Creator {

    companion object {
        // Cell width
        private const val cew = 600.0f

        // Cell height
        private const val ceh = 400.0f

        // Left offset
        private const val lo = (1404.0f - 2 * cew - 50.0f) / 2.0f

        // Top offset
        private const val to = (1872.0f - 4 * ceh - 150.0f) / 2.0f

        /**
         * Draw weekly template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarWeek data class
         */
        fun drawPage(context: Context, canvas: Canvas, calendarWeek: CalendarWeek) {
            val year = calendarWeek.year
            val weekOfYear = calendarWeek.weekOfYear
            val locale = calendarWeek.locale ?: Locale.getDefault()

            val weekFields = WeekFields.of(locale)
            val startWeekDate = LocalDate.ofYearDay(year, 1)
                .with(weekFields.weekOfYear(), weekOfYear.toLong())
                .with(weekFields.dayOfWeek(), 1)

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            for (i in 0..7) {
                val xo = lo + (i % 2) * cew + (i % 2) * 50.0f
                val yo = to + (i / 2) * ceh + (i / 2) * 50.0f
                drawDayGrid(canvas, xo, yo)
                val currentDate = startWeekDate.plusDays(i.toLong())
                val day = currentDate.dayOfMonth
                val dayName = when (currentDate.dayOfWeek.value) {
                    DayOfWeek.MONDAY.value -> DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, locale)
                    DayOfWeek.TUESDAY.value -> DayOfWeek.TUESDAY.getDisplayName(TextStyle.FULL, locale)
                    DayOfWeek.WEDNESDAY.value -> DayOfWeek.WEDNESDAY.getDisplayName(TextStyle.FULL, locale)
                    DayOfWeek.THURSDAY.value -> DayOfWeek.THURSDAY.getDisplayName(TextStyle.FULL, locale)
                    DayOfWeek.FRIDAY.value -> DayOfWeek.FRIDAY.getDisplayName(TextStyle.FULL, locale)
                    DayOfWeek.SATURDAY.value -> DayOfWeek.SATURDAY.getDisplayName(TextStyle.FULL, locale)
                    DayOfWeek.SUNDAY.value -> DayOfWeek.SUNDAY.getDisplayName(TextStyle.FULL, locale)
                    else -> "?"
                }
                if (i == 7) {
                    val notes = context.getString(R.string.calendar_week_notes)
                    canvas.drawText("W$weekOfYear", xo + 10.0f, yo + 40.0f, Creator.textDefaultWhite)
                    canvas.drawText(notes, xo + cew - 10.0f, yo + 40.0f, Creator.textDefaultWhiteRight)
                } else {
                    canvas.drawText("$day", xo + 10.0f, yo + 40.0f, Creator.textDefaultWhite)
                    canvas.drawText(dayName, xo + cew - 10.0f, yo + 40.0f, Creator.textDefaultWhiteRight)
                }
            }
        }

        /**
         * Draw the grid of day.
         *
         * @param canvas the canvas
         * @param lo the left offset
         * @param to the top offset
         */
        private fun drawDayGrid(canvas: Canvas, lo: Float, to: Float) {
            canvas.drawRect(lo, to, lo + cew, to + 50.0f, Creator.fillGrey80)

            canvas.drawLine(lo, to + 1 * 50.0f, lo + cew, to + 1 * 50.0f, Creator.lineDefaultBlack)
            for (i in 2..7) {
                canvas.drawLine(lo, to + i * 50.0f, lo + cew, to + i * 50.0f, Creator.lineDefaultGrey50)
            }
            canvas.drawLine(lo, to + 8 * 50.0f, lo + cew, to + 8 * 50.0f, Creator.lineDefaultBlack)
        }
    }
}
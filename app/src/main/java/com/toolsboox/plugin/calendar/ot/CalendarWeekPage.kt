package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import com.toolsboox.R
import com.toolsboox.ot.Creator
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v2.CalendarWeek
import com.toolsboox.plugin.calendar.ui.CalendarWeekFragment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * Create weekly template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarWeekPage : Creator {

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
         * Process touch event on the calendar page and navigate to the view of calendar.
         *
         * @param view the surface view
         * @param motionEvent the motion event
         * @param gestureResult the gesture result
         * @param fragment the parent fragment
         * @param calendarWeek the calendar data class
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, gestureResult: Int,
            fragment: CalendarWeekFragment, calendarWeek: CalendarWeek
        ): Boolean {
            if (motionEvent.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) return true

            val year = calendarWeek.year
            val weekOfYear = calendarWeek.weekOfYear
            val locale = calendarWeek.locale

            val weekFields = WeekFields.of(locale)
            val startWeekDate = LocalDate.ofYearDay(year, 1)
                .with(weekFields.weekOfYear(), weekOfYear.toLong())
                .with(weekFields.dayOfWeek(), 1)

            when (gestureResult) {
                OnGestureListener.LTR -> {
                    CalendarNavigator.toWeekPage(fragment, startWeekDate.minusWeeks(1L), locale)
                    return true
                }

                OnGestureListener.RTL -> {
                    CalendarNavigator.toWeekPage(fragment, startWeekDate.plusWeeks(1L), locale)
                    return true
                }

                OnGestureListener.UTD -> {
                    CalendarNavigator.toMonthPage(fragment, startWeekDate)
                    return true
                }

                OnGestureListener.DTU -> {
                    CalendarNavigator.toWeekNote(fragment, startWeekDate, locale, "0")
                    return true
                }
            }

            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    val px = motionEvent.x * 1404.0f / view.width
                    val py = motionEvent.y * 1872.0f / view.height

                    for (i in 0..7) {
                        val xo = lo + (i % 2) * cew + (i % 2) * 50.0f
                        val yo = to + (i / 2) * ceh + (i / 2) * 50.0f

                        if (px >= xo && px <= xo + cew && py >= yo && py <= yo + ceh) {
                            if (i == 7) {
                                CalendarNavigator.toWeekPage(fragment, startWeekDate, locale)
                            } else {
                                CalendarNavigator.toDayPage(fragment, startWeekDate.plusDays(i.toLong()))
                            }

                            return true
                        }
                    }
                }
            }

            return true
        }

        /**
         * Draw weekly template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarWeek data class
         * @param calendarPattern the calendar pattern
         */
        fun drawPage(context: Context, canvas: Canvas, calendarWeek: CalendarWeek, calendarPattern: CalendarPattern) {
            val year = calendarWeek.year
            val weekOfYear = calendarWeek.weekOfYear
            val locale = calendarWeek.locale

            val weekFields = WeekFields.of(locale)
            val startWeekDate = LocalDate.ofYearDay(year, 1)
                .with(weekFields.weekOfYear(), weekOfYear.toLong())
                .with(weekFields.dayOfWeek(), 1)
            val dayOfYearBase = startWeekDate.dayOfYear

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            for (i in 0..7) {
                val dayOfYear = dayOfYearBase + i
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
                    val weekText = context.getString(R.string.week_abbreviation, weekOfYear)
                    canvas.drawText( weekText, xo + 10.0f, yo + 40.0f, Creator.textDefaultWhite)
                    canvas.drawText(notes, xo + cew - 10.0f, yo + 40.0f, Creator.textDefaultWhiteRight)
                } else {
                    canvas.drawText("$day", xo + 20.0f, yo + 40.0f, Creator.textDefaultWhite)
                    canvas.drawText(dayName, xo + cew - 10.0f, yo + 40.0f, Creator.textDefaultWhiteRight)

                    if (calendarPattern.getDayPages(dayOfYear) > 0) {
                        Creator.drawTriangle(canvas, xo + 2.0f, yo + 2.0f, 10.0f, Creator.fillWhite)
                    }
                    Creator.notesDots(canvas, xo + 5.0f, yo + 45.0f, 2.5f, calendarPattern.getDayNotes(dayOfYear), Color.WHITE)
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
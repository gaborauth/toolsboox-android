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
            canvas.drawRect(0.0f, 0.0f, 1404.0f, 140.4f, Creator.fillWhite)

            canvas.drawLine(0.0f, 138.4f, 1404.0f, 138.4f, Creator.lineDefaultBlack)
            canvas.drawLine(0.0f, 136.4f, 1404.0f, 136.4f, Creator.lineDefaultBlack)

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

            canvas.drawRect(lo + 0 * cew, to + 0 * ceh, lo + 1 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 0 * cew, to + 0 * ceh, lo + 1 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            canvas.drawText("<", lo + 0 * cew + cew / 2, to + 1 * ceh - 30.0f, Creator.textBigBlackCenter)

            canvas.drawRect(lo + 1 * cew, to + 0 * ceh, lo + 3 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 1 * cew, to + 0 * ceh, lo + 3 * cew, to + 1 * ceh, Creator.lineDefaultBlack)

            val today = ContextCompat.getDrawable(context, R.drawable.ic_calendar_today)
            canvas.save()
            today?.setBounds(10, 10, (2 * cew).toInt() - 10, ceh.toInt() - 10)
            canvas.translate(lo + 1 * cew, to)
            today?.draw(canvas)
            canvas.restore()

            canvas.drawRect(lo + 3 * cew, to + 0 * ceh, lo + 6 * cew, to + 1 * ceh, Creator.lineDefaultBlack)

            canvas.drawRect(lo + 6 * cew, to + 0 * ceh, lo + 9 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 6 * cew, to + 0 * ceh, lo + 9 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            val weekText = context.getString(R.string.week_abbreviation, weekOfYear)
            Creator.drawEllipsizedText(
                canvas, weekText, Creator.textBigBlackCenter, lo + 6 * cew, to + 1 * ceh - 30.0f, 3 * cew
            )

            if (calendarPattern.getWeekPages(weekOfYear) > 0) {
                Creator.drawTriangle(canvas, lo + 6 * cew, to + 0 * ceh, 20.0f)
            }
            Creator.notesDots(canvas, lo + 6 * cew + 10.0f, to + 1 * ceh - 10.0f, 5.0f, calendarPattern.getWeekNotes(weekOfYear))

            canvas.drawRect(lo + 9 * cew, to + 0 * ceh, lo + 13 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 9 * cew, to + 0 * ceh, lo + 13 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            Creator.drawEllipsizedText(
                canvas, monthName, Creator.textBigBlackCenter, lo + 9 * cew, to + 1 * ceh - 30.0f, 4 * cew
            )

            if (calendarPattern.getMonthPages(monthOfYear) > 0) {
                Creator.drawTriangle(canvas, lo + 9 * cew, to + 0 * ceh, 20.0f)
            }
            Creator.notesDots(canvas, lo + 9 * cew + 10.0f, to + 1 * ceh - 10.0f, 5.0f, calendarPattern.getMonthNotes(monthOfYear))

            canvas.drawRect(lo + 13 * cew, to + 0 * ceh, lo + 15 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 13 * cew, to + 0 * ceh, lo + 15 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            val quarterText = context.getString(R.string.quarter_abbreviation, quarterOfYear)
            Creator.drawEllipsizedText(
                canvas, quarterText, Creator.textBigBlackCenter, lo + 13 * cew, to + 1 * ceh - 30.0f, 2 * cew
            )

            if (calendarPattern.getQuarterPages(quarterOfYear) > 0) {
                Creator.drawTriangle(canvas, lo + 13 * cew, to + 0 * ceh, 20.0f)
            }
            Creator.notesDots(canvas, lo + 13 * cew + 10.0f, to + 1 * ceh - 10.0f, 5.0f, calendarPattern.getQuarterNotes(quarterOfYear))

            canvas.drawRect(lo + 15 * cew, to + 0 * ceh, lo + 19 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 15 * cew, to + 0 * ceh, lo + 19 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            Creator.drawEllipsizedText(
                canvas, "$year", Creator.textBigBlackCenter, lo + 15 * cew, to + 1 * ceh - 30.0f, 4 * cew
            )

            if (calendarPattern.getYearPages() > 0) {
                Creator.drawTriangle(canvas, lo + 15 * cew, to + 0 * ceh, 20.0f)
            }
            Creator.notesDots(canvas, lo + 15 * cew + 10.0f, to + 1 * ceh - 10.0f, 5.0f, calendarPattern.getYearNotes())

            canvas.drawRect(lo + 19 * cew, to + 0 * ceh, lo + 20 * cew, to + 1 * ceh, Creator.fillGrey20)
            canvas.drawRect(lo + 19 * cew, to + 0 * ceh, lo + 20 * cew, to + 1 * ceh, Creator.lineDefaultBlack)
            canvas.drawText(">", lo + 19 * cew + cew / 2, to + 1 * ceh - 30.0f, Creator.textBigBlackCenter)
        }
    }
}

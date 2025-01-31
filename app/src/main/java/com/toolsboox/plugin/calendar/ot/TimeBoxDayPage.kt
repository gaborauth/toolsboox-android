package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.text.format.DateFormat
import android.view.MotionEvent
import android.view.View
import com.toolsboox.R
import com.toolsboox.ot.Creator
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import com.toolsboox.plugin.calendar.ui.CalendarDayFragment
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Create daily time-box template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class TimeBoxDayPage {

    companion object {
        // Cell width
        private const val cew = 600.0f

        // Cell height
        private const val ceh = 50.0f

        // Left offset
        private const val lo = (1404.0f - 2 * cew - 50.0f) / 2.0f

        // Top offset
        private const val to = (1872.0f - 35 * ceh) / 2.0f

        /**
         * Process touch event on the calendar page and navigate to the view of calendar.
         *
         * @param view the surface view
         * @param motionEvent the motion event
         * @param gestureResult the gesture result
         * @param fragment the parent fragment
         * @param calendarDay the calendar data class
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, gestureResult: Int,
            fragment: CalendarDayFragment, calendarDay: CalendarDay
        ): Boolean {
            if (motionEvent.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) return true

            val year = calendarDay.year
            val month = calendarDay.month
            val day = calendarDay.day
            val locale = calendarDay.locale

            val localDate = LocalDate.of(year, month, day)

            when (gestureResult) {
                OnGestureListener.LTR -> {
                    CalendarNavigator.toDayPage(fragment, localDate.minusDays(1L), CalendarDay.TIME_BOX_V1_STYLE)
                    return true
                }

                OnGestureListener.RTL -> {
                    CalendarNavigator.toDayPage(fragment, localDate.plusDays(1L), CalendarDay.TIME_BOX_V1_STYLE)
                    return true
                }

                OnGestureListener.UTD -> {
                    CalendarNavigator.toWeekPage(fragment, localDate, locale)
                    return true
                }

                OnGestureListener.DTU -> {
                    CalendarNavigator.toDayNote(fragment, localDate, "0")
                    return true
                }
            }

            return true
        }

        /**
         * Draw the daily template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarDay data class
         * @param startHour start hour
         */
        fun drawPage(
            context: Context, canvas: Canvas, calendarDay: CalendarDay
        ) {
            val timeBoxText = context.getString(R.string.calendar_time_box_day_time_box)
            val tasksText = context.getString(R.string.calendar_time_box_day_tasks)
            val brainDumpText = context.getString(R.string.calendar_time_box_day_brain_dump)
            val notesText = context.getString(R.string.calendar_time_box_day_notes)
            val locale = calendarDay.locale
            val startHour = calendarDay.startHour!!

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            // Time box title
            canvas.drawRect(lo, to, lo + cew, to + ceh, Creator.fillGrey80)
            canvas.drawText(timeBoxText, lo + 10.0f, to + ceh - 10.0f, Creator.textDefaultWhite)

            // Time box grid
            canvas.drawLine(lo, to + ceh, lo + cew, to + ceh, Creator.lineDefaultBlack)
            for (i in 1..34) {
                canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey20)
                if (i % 2 == 1) {
                    if (startHour > -1) {
                        val localTime = LocalTime.of(i / 2 + startHour, 0, 0)
                        if (DateFormat.is24HourFormat(context)) {
                            val hourText = localTime.format(DateTimeFormatter.ofPattern("HH"))
                            canvas.drawText(hourText, lo + 60.0f, to - 10.0f + (i + 1) * ceh, Creator.textDefaultGray20Right)
                        } else {
                            val hourText = localTime.format(DateTimeFormatter.ofPattern("h"))
                            val ampmText = localTime.format(DateTimeFormatter.ofPattern("a"))
                            canvas.drawText(hourText, lo + 60.0f, to - 10.0f + (i + 1) * ceh, Creator.textDefaultGray20Right)
                            canvas.drawText(ampmText, lo + 60.0f, to + 40.0f + (i + 1) * ceh, Creator.textDefaultGray20Right)
                        }
                    }
                } else {
                    canvas.drawLine(lo + 30.0f, to + i * ceh, lo + 30.0f, to + (i + 1) * ceh, Creator.lineDefaultGrey20)
                    canvas.drawLine(lo, to + (i + 1) * ceh, lo + cew, to + (i + 1) * ceh, Creator.lineDefaultGrey50)
                }
            }
            canvas.drawLine(lo, to + 35 * ceh, lo + cew, to + 35 * ceh, Creator.lineDefaultGrey20)

            canvas.drawLine(lo, to + 1 * ceh, lo, to + 35 * ceh, Creator.lineDefaultGrey20)
            for (i in 2..20) {
                canvas.drawLine(lo + i * 30.0f, to + 1 * ceh, lo + i * 30.0f, to + 35 * ceh, Creator.lineDefaultGrey20)
            }

            // Tasks title
            canvas.drawRect(lo + cew + 50.0f, to, lo + 2 * cew + 50.0f, to + ceh, Creator.fillGrey80)
            canvas.drawText(tasksText, lo + cew + 60.0f, to + ceh - 10.0f, Creator.textDefaultWhite)

            // Tasks grid
            canvas.drawLine(lo + cew + 50.0f, to + ceh, lo + 2 * cew + 50.0f, to + ceh, Creator.lineDefaultBlack)
            for (i in 1..4) {
                canvas.drawLine(lo + cew + 50.0f, to + i * ceh, lo + 2 * cew + 50.0f, to + i * ceh, Creator.lineDefaultGrey50)
                if (i % 2 == 0) {
                    canvas.drawRect(lo + cew + 50.0f, to + i * ceh, lo + 2 * cew + 50.0f, to + i * ceh + ceh, Creator.fillGrey20)
                }
                canvas.drawRect(lo + cew + 60.0f, to + i * ceh + 10.0f, lo + cew + 90.0f, to + i * ceh + 40.0f, Creator.lineDefaultGrey50)
            }
            canvas.drawLine(lo + cew + 50.0f, to + 5 * ceh, lo + 2 * cew + 50.0f, to + 5 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(lo + cew + 100.0f, to + ceh, lo + cew + 100.0f, to + 5 * ceh, Creator.lineDefaultBlack)

            // Brain dump title
            canvas.drawRect(lo + cew + 50.0f, to + 6 * ceh, lo + 2 * cew + 50.0f, to + 7 * ceh, Creator.fillGrey80)
            canvas.drawText(brainDumpText, lo + cew + 60.0f, to + 7 * ceh - 10.0f, Creator.textDefaultWhite)

            // Brain dump grid
            for (x in 20..40) {
                for (y in 8..25) {
                    canvas.drawRect(lo + 48.0f + x * 30, to + y * ceh - 2.0f, lo + 52.0f + x * 30, to + y * ceh + 2.0f, Creator.fillGrey80)
                }
            }

            // Notes title
            canvas.drawRect(lo + cew + 50.0f, to + 26 * ceh, lo + 2 * cew + 50.0f, to + 27 * ceh, Creator.fillGrey80)
            canvas.drawText(notesText, lo + cew + 60.0f, to + 27 * ceh - 10.0f, Creator.textDefaultWhite)

            // Notes grid
            canvas.drawLine(
                lo + cew + 50.0f,
                to + 27 * ceh,
                lo + 2 * cew + 50.0f,
                to + 27 * ceh,
                Creator.lineDefaultBlack
            )
            for (i in 27..35) {
                canvas.drawLine(
                    lo + cew + 50.0f, to + i * ceh, lo + 2 * cew + 50.0f, to + i * ceh,
                    Creator.lineDefaultGrey50
                )
                if (i % 2 == 0) {
                    canvas.drawRect(
                        lo + cew + 50.0f, to + i * ceh, lo + 2 * cew + 50.0f, to + i * ceh + ceh,
                        Creator.fillGrey20
                    )
                }
            }
            canvas.drawLine(
                lo + cew + 50.0f, to + 35 * ceh, lo + 2 * cew + 50.0f, to + 35 * ceh,
                Creator.lineDefaultBlack
            )
        }
    }
}

package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.toolsboox.R
import com.toolsboox.ot.Creator
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v1.CalendarDay
import com.toolsboox.plugin.calendar.da.v1.GoogleCalendarEvent
import com.toolsboox.plugin.calendar.ui.CalendarDayFragment
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Create daily template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarDayPage {

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
                    CalendarNavigator.toDay(fragment, localDate.minusDays(1L), false)
                    return true
                }

                OnGestureListener.RTL -> {
                    CalendarNavigator.toDay(fragment, localDate.plusDays(1L), false)
                    return true
                }

                OnGestureListener.UTD -> {
                    CalendarNavigator.toWeek(fragment, localDate, locale, false)
                    return true
                }

                OnGestureListener.DTU -> {
                    CalendarNavigator.toDay(fragment, localDate, true)
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
         * @param googleCalendarEvents the list of Google calendar events
         */
        fun drawPage(
            context: Context, canvas: Canvas, calendarDay: CalendarDay, googleCalendarEvents: List<GoogleCalendarEvent>
        ) {
            val schedulesText = context.getString(R.string.calendar_day_schedules)
            val tasksText = context.getString(R.string.calendar_day_tasks)
            val notesText = context.getString(R.string.calendar_day_notes)
            val notesCalsText = context.getString(R.string.calendar_day_notes_cals)
            val allDayText = context.getString(R.string.calendar_day_all_day)
            val locale = calendarDay.locale

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            // Schedules title
            canvas.drawRect(lo, to, lo + cew, to + ceh, Creator.fillGrey80)
            canvas.drawText(schedulesText, lo + 10.0f, to + ceh - 10.0f, Creator.textDefaultWhite)

            // Schedules grid
            canvas.drawLine(lo, to + ceh, lo + cew, to + ceh, Creator.lineDefaultBlack)
            for (i in 1..34) {
                if (i % 2 == 1) {
                    canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
                    canvas.drawText(":00", lo + 115.0f, to + 35.0f + i * ceh, Creator.textSmallBlackRight)
                } else {
                    canvas.drawLine(lo + 80.0f, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
                    canvas.drawRect(lo + 80.0f, to + i * ceh, lo + cew, to + i * ceh + ceh, Creator.fillGrey20)
                    canvas.drawText(":30", lo + 115.0f, to + 35.0f + i * ceh, Creator.textSmallBlackRight)
                }
            }
            canvas.drawLine(lo, to + 35 * ceh, lo + cew, to + 35 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(lo + 120.0f, to + ceh, lo + 120.0f, to + 35 * ceh, Creator.lineDefaultBlack)

            // Tasks title
            canvas.drawRect(lo + cew + 50.0f, to, lo + 2 * cew + 50.0f, to + ceh, Creator.fillGrey80)
            canvas.drawText(tasksText, lo + cew + 60.0f, to + ceh - 10.0f, Creator.textDefaultWhite)

            // Tasks grid
            canvas.drawLine(lo + cew + 50.0f, to + ceh, lo + 2 * cew + 50.0f, to + ceh, Creator.lineDefaultBlack)
            for (i in 1..16) {
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
                canvas.drawRect(
                    lo + cew + 60.0f, to + i * ceh + 10.0f, lo + cew + 90.0f, to + i * ceh + 40.0f,
                    Creator.lineDefaultGrey50
                )
            }
            canvas.drawLine(
                lo + cew + 50.0f, to + 17 * ceh, lo + 2 * cew + 50.0f, to + 17 * ceh,
                Creator.lineDefaultBlack
            )
            canvas.drawLine(
                lo + cew + 100.0f, to + ceh, lo + cew + 100.0f, to + 17 * ceh,
                Creator.lineDefaultBlack
            )

            // Notes title
            canvas.drawRect(lo + cew + 50.0f, to + 18 * ceh, lo + 2 * cew + 50.0f, to + 19 * ceh, Creator.fillGrey80)
            if (googleCalendarEvents.isEmpty()) {
                canvas.drawText(notesText, lo + cew + 60.0f, to + 19 * ceh - 10.0f, Creator.textDefaultWhite)
            } else {
                canvas.drawText(notesCalsText, lo + cew + 60.0f, to + 19 * ceh - 10.0f, Creator.textDefaultWhite)
            }

            // Notes grid
            canvas.drawLine(
                lo + cew + 50.0f,
                to + 19 * ceh,
                lo + 2 * cew + 50.0f,
                to + 19 * ceh,
                Creator.lineDefaultBlack
            )
            for (i in 20..35) {
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

            // Google Calendar events
            for (i in 0..7) {
                if (i < googleCalendarEvents.size) {
                    googleCalendarEvents[i].let { event ->
                        Creator.drawEllipsizedText(
                            canvas, event.title, Creator.textDefaultBlack,
                            lo + cew + 60.0f, to + (20 + i * 2) * ceh - 10.0f, cew
                        )
                        if (event.allDay) {
                            canvas.drawText(
                                allDayText, lo + cew + 60.0f, to + (21 + i * 2) * ceh - 10.0f,
                                Creator.textSmallBlack
                            )
                        } else {
                            val startDate = event.startDate.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG))
                            val endDate = event.endDate.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG))
                            canvas.drawText(
                                startDate, lo + cew + 60.0f, to + (21 + i * 2) * ceh - 10.0f,
                                Creator.textSmallBlack
                            )
                            canvas.drawText(
                                endDate, lo + cew + 40.0f + cew, to + (21 + i * 2) * ceh - 10.0f,
                                Creator.textSmallBlackRight
                            )
                        }
                    }
                }
            }
        }
    }
}

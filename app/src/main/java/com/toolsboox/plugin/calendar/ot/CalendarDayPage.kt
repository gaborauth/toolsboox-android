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
import com.toolsboox.plugin.calendar.da.v1.CalendarEvent
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import com.toolsboox.plugin.calendar.ui.CalendarDayFragment
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
                    CalendarNavigator.toDayPage(fragment, localDate.minusDays(1L))
                    return true
                }

                OnGestureListener.RTL -> {
                    CalendarNavigator.toDayPage(fragment, localDate.plusDays(1L))
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
         * @param calendarEvents the list of calendar events
         */
        fun drawPage(
            context: Context, canvas: Canvas, calendarDay: CalendarDay, calendarEvents: List<CalendarEvent>
        ) {
            val schedulesText = context.getString(R.string.calendar_day_schedules)
            val tasksText = context.getString(R.string.calendar_day_tasks)
            val notesText = context.getString(R.string.calendar_day_notes)
            val allDayText = context.getString(R.string.calendar_day_all_day)
            val locale = calendarDay.locale

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            // Schedules title
            canvas.drawRect(lo, to, lo + cew, to + ceh, Creator.fillGrey80)
            canvas.drawText(schedulesText, lo + 10.0f, to + ceh - 10.0f, Creator.textDefaultWhite)

            calendarEvents.sortedWith(compareBy({ it.startDate }, { it.endDate }))
            val laneOne = mutableListOf<CalendarEvent>()
            val laneTwo = mutableListOf<CalendarEvent>()
            val laneFull = mutableListOf<CalendarEvent>()
            val outside = mutableListOf<CalendarEvent>()

            val startHour = calendarDay.startHour!!
            if (startHour < 0) {
                outside.addAll(calendarEvents)
            } else {
                for (event in calendarEvents) {
                    if (event.allDay) {
                        outside.add(event)
                        continue
                    }

                    val startLocalDate = Instant.ofEpochMilli(event.startDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    if (startLocalDate.hour * 60 + startLocalDate.minute < startHour * 60) {
                        outside.add(event)
                        continue
                    }
                    val endLocalDate = Instant.ofEpochMilli(event.endDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    if (endLocalDate.hour * 60 + endLocalDate.minute > (startHour + 17) * 60) {
                        outside.add(event)
                        continue
                    }

                    if (checkOverlap(event, laneOne)) {
                        if (checkOverlap(event, laneTwo)) {
                            outside.add(event)
                        } else {
                            laneTwo.add(event)
                        }
                    } else {
                        laneOne.add(event)
                    }
                }

                for (event in calendarEvents) {
                    if (event.allDay) continue
                    val startLocalDate = Instant.ofEpochMilli(event.startDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    if (startLocalDate.hour * 60 + startLocalDate.minute < startHour * 60) continue
                    val endLocalDate = Instant.ofEpochMilli(event.endDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    if (endLocalDate.hour * 60 + endLocalDate.minute > (startHour + 17) * 60) continue

                    if (!checkFullWidth(event, laneOne, laneTwo)) {
                        laneFull.add(event)
                    }
                }
            }

            val notesCalsText = if (outside.size > 8) {
                context.getString(R.string.calendar_day_notes_events_ex).format(outside.size)
            } else {
                context.getString(R.string.calendar_day_notes_events).format(outside.size)
            }

            // Schedules grid
            canvas.drawLine(lo, to + ceh, lo + cew, to + ceh, Creator.lineDefaultBlack)
            for (i in 1..34) {
                if (i % 2 == 1) {
                    canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
                    canvas.drawText(":00", lo + 115.0f, to + 35.0f + i * ceh, Creator.textSmallBlackRight)
                    if (startHour > -1) {
                        val localTime = LocalTime.of(i / 2 + startHour, 0, 0)
                        if (DateFormat.is24HourFormat(context)) {
                            val hourText = localTime.format(DateTimeFormatter.ofPattern("HH"))
                            canvas.drawText(hourText, lo + 40.0f, to + 20.0f + (i + 1) * ceh, Creator.text60BlackCenter)
                        } else {
                            val hourText = localTime.format(DateTimeFormatter.ofPattern("h"))
                            val ampmText = localTime.format(DateTimeFormatter.ofPattern("a"))
                            canvas.drawText(hourText, lo + 70.0f, to + 40.0f + (i) * ceh, Creator.textDefaultBlackRight)
                            canvas.drawText(ampmText, lo + 70.0f, to + 30.0f + (i + 1) * ceh, Creator.textDefaultBlackRight)
                        }
                    }
                } else {
                    canvas.drawLine(lo + 80.0f, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
                    canvas.drawRect(lo + 80.0f, to + i * ceh, lo + cew, to + i * ceh + ceh, Creator.fillGrey20)
                    canvas.drawText(":30", lo + 115.0f, to + 35.0f + i * ceh, Creator.textSmallBlackRight)
                }
            }
            canvas.drawLine(lo, to + 35 * ceh, lo + cew, to + 35 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(lo + 120.0f, to + ceh, lo + 120.0f, to + 35 * ceh, Creator.lineDefaultBlack)

            if (laneOne.isNotEmpty()) {
                drawEventLane(canvas, startHour, laneOne, 120.0f, (cew - 120.0f) / 2)
            }
            if (laneTwo.isNotEmpty()) {
                drawEventLane(canvas, startHour, laneTwo, 120.0f + (cew - 120.0f) / 2, (cew - 120.0f) / 2)
            }
            if (laneFull.isNotEmpty()) {
                drawEventLane(canvas, startHour, laneFull, 120.0f, cew - 120.0f)
            }

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
            if (outside.isEmpty()) {
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

            // Calendar events
            for (i in 0..7) {
                if (i < outside.size) {
                    outside[i].let { event ->
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
                            val startLocalDate = Instant.ofEpochMilli(event.startDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                            val endLocalDate = Instant.ofEpochMilli(event.endDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                            val startDate = startLocalDate.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))
                            val endDate = endLocalDate.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))
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

        private fun drawEventLane(canvas: Canvas, startHour: Int, lane: MutableList<CalendarEvent>, llo: Float, lw: Float) {
            for (event in lane) {
                val startLocalDate = Instant.ofEpochMilli(event.startDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                val endLocalDate = Instant.ofEpochMilli(event.endDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                val cehs = (startLocalDate.hour * 60 + startLocalDate.minute - startHour * 60) / 30.0f * ceh + 1.0f * ceh
                val cehe = (endLocalDate.hour * 60 + endLocalDate.minute - startHour * 60) / 30.0f * ceh + 1.0f * ceh
                canvas.drawRect(lo + llo + 5.0f, to + cehs, lo + llo + lw - 5.0f, to + cehe, Creator.fillGrey10)
                canvas.drawRect(lo + llo + 5.0f, to + cehs, lo + llo + lw - 5.0f, to + cehe, Creator.lineDefaultBlack)

                Creator.drawEllipsizedText(
                    canvas, event.title, Creator.textSmallBlack,
                    lo + llo + 15.0f, to + cehs + ceh * 0.66f - 10.0f, lw - 20.0f
                )
            }
        }

        private fun checkOverlap(event: CalendarEvent, lane: MutableList<CalendarEvent>): Boolean {
            for (laneEvent in lane) {
                if (event.startDate in laneEvent.startDate..<laneEvent.endDate) return true
                if (laneEvent.startDate in event.startDate..<event.endDate) return true
            }

            return false
        }

        private fun checkFullWidth(event: CalendarEvent, laneOne: MutableList<CalendarEvent>, laneTwo: MutableList<CalendarEvent>): Boolean {
            for (laneEvent in laneOne) {
                if (laneEvent.id == event.id) continue
                if (event.startDate in laneEvent.startDate..<laneEvent.endDate) return true
                if (laneEvent.startDate in event.startDate..<event.endDate) return true
            }

            for (laneEvent in laneTwo) {
                if (laneEvent.id == event.id) continue
                if (event.startDate in laneEvent.startDate..<laneEvent.endDate) return true
                if (laneEvent.startDate in event.startDate..<event.endDate) return true
            }

            laneOne.remove(event)
            laneTwo.remove(event)

            return false
        }
    }
}

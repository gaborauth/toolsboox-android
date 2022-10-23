package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import com.toolsboox.R
import com.toolsboox.ot.Creator
import com.toolsboox.plugin.calendar.da.CalendarDay
import java.util.*

/**
 * Create daily template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarDayCreator {

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
         * Draw the daily template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarDay data class
         */
        fun drawPage(context: Context, canvas: Canvas, calendarDay: CalendarDay) {
            val schedulesText = context.getString(R.string.calendar_day_schedules)
            val tasksText = context.getString(R.string.calendar_day_tasks)
            val notesText = context.getString(R.string.calendar_day_notes)
            val locale = calendarDay.locale ?: Locale.getDefault()

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
            canvas.drawText(notesText, lo + cew + 60.0f, to + 19 * ceh - 10.0f, Creator.textDefaultWhite)

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
        }
    }
}

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
import com.toolsboox.plugin.calendar.da.v2.CalendarYear
import com.toolsboox.plugin.calendar.ui.CalendarYearFragment
import java.time.LocalDate

/**
 * Create navigator of yearly template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarYearNavigator {

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
         * @param calendarYear the calendar data class
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, fragment: CalendarYearFragment, calendarYear: CalendarYear
        ): Boolean {
            val year = calendarYear.year

            val localDate = LocalDate.of(year, 1, 1)

            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    val px = motionEvent.x * 1404.0f / view.width
                    val py = motionEvent.y * 140.4f / view.height

                    if (px >= lo + 0 * cew && px <= lo + 1 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toYearPage(fragment, localDate.minusYears(1L))
                        return true
                    }
                    if (px >= lo + 1 * cew && px <= lo + 3 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toDayPage(fragment, LocalDate.now())
                        return true
                    }
                    if (px >= lo + 15 * cew && px <= lo + 19 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toYearPage(fragment, localDate)
                        return true
                    }
                    if (px >= lo + 19 * cew && px <= lo + 20 * cew && py >= to && py <= to + ceh) {
                        CalendarNavigator.toYearPage(fragment, localDate.plusYears(1L))
                        return true
                    }
                }
            }

            return true
        }

        /**
         * Draw the navigator of quarterly template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarYear data class
         * @param calendarPattern the calendar pattern
         */
        fun draw(context: Context, canvas: Canvas, calendarYear: CalendarYear, calendarPattern: CalendarPattern) {
            canvas.drawRect(0.0f, 0.0f, 1404.0f, 140.4f, Creator.fillWhite)

            canvas.drawLine(0.0f, 138.4f, 1404.0f, 138.4f, Creator.lineDefaultBlack)
            canvas.drawLine(0.0f, 136.4f, 1404.0f, 136.4f, Creator.lineDefaultBlack)

            val year = calendarYear.year

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

            canvas.drawRect(lo + 6 * cew, to + 0 * ceh, lo + 9 * cew, to + 1 * ceh, Creator.lineDefaultBlack)

            canvas.drawRect(lo + 9 * cew, to + 0 * ceh, lo + 13 * cew, to + 1 * ceh, Creator.lineDefaultBlack)

            canvas.drawRect(lo + 13 * cew, to + 0 * ceh, lo + 15 * cew, to + 1 * ceh, Creator.lineDefaultBlack)

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

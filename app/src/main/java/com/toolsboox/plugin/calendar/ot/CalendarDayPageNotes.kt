package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import com.toolsboox.ot.Creator
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import com.toolsboox.plugin.calendar.ui.CalendarDayFragment
import java.time.LocalDate

/**
 * Create daily template of calendar plugin notes.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarDayPageNotes : Creator {

    companion object {

        // Cell width
        private const val cew = 1300.0f

        // Cell height
        private const val ceh = 50.0f

        // Left offset
        private const val lo = (1404.0f - 1 * cew) / 2.0f

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
         * @param notePage current notePage
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, gestureResult: Int,
            fragment: CalendarDayFragment, calendarDay: CalendarDay, notePage: String
        ): Boolean {
            if (motionEvent.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) return true

            val year = calendarDay.year
            val month = calendarDay.month
            val day = calendarDay.day
            val locale = calendarDay.locale

            val localDate = LocalDate.of(year, month, day)

            when (gestureResult) {
                OnGestureListener.UTD -> {
                    if (notePage == "gratitude") {
                        CalendarNavigator.toDayPage(fragment, localDate)
                    } else {
                        val page = notePage.toIntOrNull() ?: 0
                        if (page == 0) {
                            CalendarNavigator.toDayNote(fragment, localDate, "gratitude")
                        } else {
                            CalendarNavigator.toDayNote(fragment, localDate, "${page - 1}")
                        }
                    }
                    return true
                }

                OnGestureListener.DTU -> {
                    if (notePage == "gratitude") {
                        CalendarNavigator.toDayNote(fragment, localDate, "0")
                    } else {
                        val page = notePage.toIntOrNull() ?: 0
                        CalendarNavigator.toDayNote(fragment, localDate, "${page + 1}")
                    }
                    return true
                }
            }

            return true
        }

        /**
         * Draw the daily template of calendar plugin notes.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarDay data class
         * @param template the template code
         * @param notePage current notePage
         */
        fun drawPage(context: Context, canvas: Canvas, calendarDay: CalendarDay, template: Int, notePage: String) {
            if (notePage == "gratitude") {
                drawGratitudePage(canvas)
                return
            }

            val page = notePage.toIntOrNull() ?: 0

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)

            canvas.drawText("${page + 1}", lo + cew - 10.0f, to + 3 * ceh - 10.0f, Creator.textBigGray20Right)

            if (template == 0) {
                canvas.drawLine(lo, to + 0 * ceh, lo + cew, to + 0 * ceh, Creator.lineDefaultBlack)
                for (i in 1..34) {
                    canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
                }
                canvas.drawLine(lo, to + 35 * ceh, lo + cew, to + 35 * ceh, Creator.lineDefaultBlack)
            } else if (template == 1) {
                canvas.drawLine(lo, to + 0 * ceh, lo + cew, to + 0 * ceh, Creator.lineDefaultBlack)
                for (i in 1..34) {
                    canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, Creator.lineDefaultGrey50)
                }
                canvas.drawLine(lo, to + 35 * ceh, lo + cew, to + 35 * ceh, Creator.lineDefaultBlack)

                canvas.drawLine(lo, to + 0 * ceh, lo, to + 35 * ceh, Creator.lineDefaultBlack)
                for (i in 1..25) {
                    canvas.drawLine(lo + i * 50.0f, to + 0 * ceh, lo + i * 50.0f, to + 35 * ceh, Creator.lineDefaultGrey50)
                }
                canvas.drawLine(lo + 26 * 50.0f, to + 0 * ceh, lo + 26 * 50.0f, to + 35 * ceh, Creator.lineDefaultBlack)
            }
        }

        /**
         * Draw the gratitude / journal page: two columns at the top
         * (3 Things I'm Grateful For + The Best Thing That Happened Today),
         * then a wide Doodle area below.
         */
        private fun drawGratitudePage(canvas: Canvas) {
            canvas.drawRect(0f, 0f, 1404f, 1872f, Creator.fillWhite)

            val robotBold = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            val robotPlain = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

            val headerPaint = TextPaint().apply {
                color = Color.BLACK
                textAlign = Paint.Align.LEFT
                textSize = 42f
                typeface = robotBold
                isAntiAlias = true
            }
            val headerCenterPaint = TextPaint(headerPaint).apply {
                textAlign = Paint.Align.CENTER
            }
            val numberPaint = TextPaint().apply {
                color = Color.BLACK
                textAlign = Paint.Align.LEFT
                textSize = 36f
                typeface = robotPlain
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.argb(140, 0, 0, 0)
                strokeWidth = 1.5f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            val dashedBorder = Paint().apply {
                color = Color.argb(100, 0, 0, 0)
                strokeWidth = 1.5f
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                isAntiAlias = true
            }

            val outerLeft = 60f
            val outerRight = 1344f
            val pageMidGap = 30f
            val colLeft = outerLeft
            val colMidRight = (outerLeft + outerRight) / 2f - pageMidGap / 2f  // 672
            val colMidLeft = (outerLeft + outerRight) / 2f + pageMidGap / 2f   // 732
            val colRight = outerRight

            val headerY = 130f
            val firstLineY = 200f
            val lineSpacing = 70f
            val numLines = 11
            val bottomOfColumns = firstLineY + (numLines - 1) * lineSpacing  // 200 + 10*70 = 900

            // --- Left column: 3 Things I'm Grateful For ---
            canvas.drawText("3 THINGS I'M GRATEFUL FOR", colLeft, headerY, headerPaint)
            // Three numbered slots, each with multiple lines below
            val slotsPerItem = numLines / 3  // 3
            val numberCol = colLeft
            val textIndent = colLeft + 60f
            for (i in 0 until numLines) {
                val y = firstLineY + i * lineSpacing
                if (i % slotsPerItem == 0) {
                    val n = (i / slotsPerItem) + 1
                    canvas.drawText("$n.", numberCol, y - 10f, numberPaint)
                }
                canvas.drawLine(textIndent, y, colMidRight, y, linePaint)
            }

            // --- Right column: The Best Thing That Happened Today ---
            val rightColCenter = (colMidLeft + colRight) / 2f
            canvas.drawText("THE BEST THING TODAY", rightColCenter, headerY, headerCenterPaint)
            for (i in 0 until numLines) {
                val y = firstLineY + i * lineSpacing
                canvas.drawLine(colMidLeft, y, colRight, y, linePaint)
            }

            // --- Doodle area (full width) ---
            val doodleHeaderY = bottomOfColumns + 80f
            canvas.drawText("DOODLE", colLeft, doodleHeaderY, headerPaint)
            val doodleTop = doodleHeaderY + 25f
            val doodleBottom = 1820f
            canvas.drawRect(colLeft, doodleTop, colRight, doodleBottom, dashedBorder)
        }
    }
}

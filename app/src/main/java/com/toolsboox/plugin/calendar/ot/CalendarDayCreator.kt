package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.toolsboox.R

/**
 * Create daily template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarDayCreator {

    companion object {

        /**
         * Draw the page of the boxed days of month calendar.
         *
         * @param context the context
         * @param canvas the canvas
         * @param withNotes generate with notes
         * @param withTasks generate with tasks
         * @param withHours generate with hours
         * @param startHours start hours
         */
        fun drawPage(
            context: Context, canvas: Canvas,
            withNotes: Boolean, withTasks: Boolean, withHours: Boolean, startHours: Int = 6
        ) {
            val le = 50.0f  // Left offset
            val to = 50.0f // Top offset
            val wi = if (withNotes || withTasks) 697.0f else 1304.0f // Width
            val rwi = 607.0f // Right width

            val fillPaint = Paint()
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = Color.WHITE
            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, fillPaint)

            val linePaint = Paint()
            linePaint.style = Paint.Style.STROKE
            linePaint.color = Color.BLACK

            val textPaint = Paint()
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.color = Color.BLACK
            textPaint.textSize = 40.0f

            // Paint schedules header
            fillPaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
            canvas.drawRect(le, to, le + wi, to + 100.0f, fillPaint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 75.0f
            canvas.drawText(
                context.getString(R.string.calendar_day_schedules),
                le + 10.0f,
                to + 75.0f,
                textPaint
            )

            // Paint schedules grey-white lines
            linePaint.strokeWidth = 2.0f
            fillPaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)
            for (i in 1..16) {
                linePaint.color = Color.argb(0.4f, 0.5f, 0.5f, 0.5f)
                canvas.drawRect(le + 150.0f, to + i * 100.0f, le + wi, to + i * 100.0f + 50.0f, fillPaint)

                canvas.drawLine(le + 150.0f, to + i * 100.0f, le + wi, to + i * 100.0f, linePaint)
                canvas.drawLine(le + 150.0f, to + i * 100.0f + 50.0f, le + wi, to + i * 100.0f + 50.0f, linePaint)

                linePaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
                canvas.drawLine(le, to + i * 100.0f, le + wi, to + i * 100.0f, linePaint)
            }
            linePaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
            canvas.drawLine(le, to + 17 * 100.0f, le + wi, to + 17 * 100.0f, linePaint)

            // Paint schedules vertical separator
            linePaint.strokeWidth = 2.0f
            linePaint.color = Color.argb(1.0f, 0.0f, 0.0f, 0.0f)
            canvas.drawLine(le + 150.0f, to + 100.0f, le + 150.0f, to + 1700.0f, linePaint)
            canvas.drawLine(le + 200.0f, to + 100.0f, le + 200.0f, to + 1700.0f, linePaint)

            // Paint schedules hours
            textPaint.textAlign = Paint.Align.RIGHT
            for (i in 1..16) {
                if (withHours) {
                    textPaint.textSize = 50.0f
                    textPaint.color = Color.argb(0.8f, 0.0f, 0.0f, 0.0f)
                    canvas.drawText("${i + startHours - 1}", le + 90.0f, to + 70.0f + i * 100.0f, textPaint)
                }
                textPaint.textSize = 25.0f
                textPaint.color = Color.argb(0.5f, 0.0f, 0.0f, 0.0f)
                canvas.drawText(":00", le + 190.0f, to + 40.0f + i * 100.0f, textPaint)
                canvas.drawText(":30", le + 190.0f, to + 90.0f + i * 100.0f, textPaint)
            }

            // Paint tasks
            if (withTasks) {
                val h = if (withNotes) 8 else 16
                val o = 0

                linePaint.strokeWidth = 2.0f
                fillPaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)
                for (i in 1..h) {
                    linePaint.color = Color.argb(0.4f, 0.5f, 0.5f, 0.5f)
                    canvas.drawRect(
                        le + wi + 50.0f,
                        to + o + i * 100.0f,
                        le + wi + rwi,
                        to + o + i * 100.0f + 50.0f,
                        fillPaint
                    )

                    canvas.drawLine(
                        le + wi + 50.0f,
                        to + o + i * 100.0f,
                        le + wi + rwi,
                        to + o + i * 100.0f,
                        linePaint
                    )
                    canvas.drawLine(
                        le + wi + 50.0f,
                        to + o + i * 100.0f + 50.0f,
                        le + wi + rwi,
                        to + o + i * 100.0f + 50.0f,
                        linePaint
                    )

                    linePaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
                    canvas.drawRect(
                        le + wi + 60.0f,
                        to + o + i * 100.0f + 10.0f,
                        le + wi + 90.0f,
                        to + o + i * 100.0f + 40.0f,
                        linePaint
                    )
                    canvas.drawRect(
                        le + wi + 60.0f,
                        to + o + i * 100.0f + 60.0f,
                        le + wi + 90.0f,
                        to + o + i * 100.0f + 90.0f,
                        linePaint
                    )
                }

                linePaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
                canvas.drawLine(
                    le + wi + 100.0f,
                    to + o + 100.0f,
                    le + wi + 100.0f,
                    to + o + h * 100.0f + 100.0f,
                    linePaint
                )
                canvas.drawLine(
                    le + wi + 50.0f,
                    to + o + h * 100.0f + 100.0f,
                    le + wi + rwi,
                    to + o + h * 100.0f + 100.0f,
                    linePaint
                )

                fillPaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
                canvas.drawRect(le + wi + 50.0f, to + o, le + wi + rwi, to + o + 100.0f, fillPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 75.0f
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    context.getString(R.string.calendar_day_tasks),
                    le + wi + 60.0f,
                    to + o + 75.0f,
                    textPaint
                )
            }

            if (withNotes) {
                val h = if (withTasks) 7 else 16
                val o = if (withTasks) 900.0f else 0.0f

                linePaint.strokeWidth = 2.0f
                fillPaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)
                for (i in 1..h) {
                    linePaint.color = Color.argb(0.4f, 0.5f, 0.5f, 0.5f)
                    canvas.drawRect(
                        le + wi + 50.0f,
                        to + o + i * 100.0f,
                        le + wi + rwi,
                        to + o + i * 100.0f + 50.0f,
                        fillPaint
                    )

                    canvas.drawLine(
                        le + wi + 50.0f,
                        to + o + i * 100.0f,
                        le + wi + rwi,
                        to + o + i * 100.0f,
                        linePaint
                    )
                    canvas.drawLine(
                        le + wi + 50.0f,
                        to + o + i * 100.0f + 50.0f,
                        le + wi + rwi,
                        to + o + i * 100.0f + 50.0f,
                        linePaint
                    )
                }
                linePaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
                canvas.drawLine(
                    le + wi + 50.0f,
                    to + o + (h + 1) * 100.0f,
                    le + wi + rwi,
                    to + o + (h + 1) * 100.0f,
                    linePaint
                )

                fillPaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
                canvas.drawRect(le + wi + 50.0f, to + o, le + wi + rwi, to + o + 100.0f, fillPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 75.0f
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    context.getString(R.string.calendar_day_notes),
                    le + wi + 60.0f,
                    to + o + 75.0f,
                    textPaint
                )
            }
        }
    }
}

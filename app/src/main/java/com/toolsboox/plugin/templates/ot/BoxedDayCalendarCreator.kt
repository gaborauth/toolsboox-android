package com.toolsboox.plugin.templates.ot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.format.DateFormat
import com.toolsboox.R
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*

/**
 * Create boxed day of month calendar template.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class BoxedDayCalendarCreator {

    companion object {

        /**
         * Draw the page of the boxed days of month calendar.
         *
         * @param context the context
         * @param canvas the canvas
         * @param dm the day of month
         * @param withNotes generate with notes
         * @param withTasks generate with tasks
         * @param withHours generate with hours
         */
        fun drawPage(
            context: Context,
            canvas: Canvas,
            dm: Long,
            withNotes: Boolean,
            withTasks: Boolean,
            withHours: Boolean
        ) {
            val fillPaint = Paint()
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = Color.WHITE

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, fillPaint)

            val le = 50.0f  // Left offset
            val to = 120.0f // Top offset
            val wi = if (withNotes || withTasks) 904.0f else 1304.0f // Width
            val he = 250.0f // Height

            val linePaint = Paint()
            linePaint.style = Paint.Style.STROKE
            linePaint.color = Color.BLACK

            val textPaint = Paint()
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.color = Color.BLACK
            textPaint.textSize = 40.0f

            val localDate = LocalDate.of(LocalDate.now().year, LocalDate.now().monthValue, 1)
                .with(TemporalAdjusters.firstDayOfMonth())
                .plusDays(dm - 1)
            val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

            val dateFormat = DateFormat.getDateFormat(context)
            val formattedDate = dateFormat.format(date)
            val formattedDay = SimpleDateFormat("EEE").format(date)
            val formattedMonth = SimpleDateFormat("MMMM").format(date)

            textPaint.textSize = 80.0f
            canvas.drawText(
                context.getString(R.string.templates_boxed_day_calendar_creator_title, formattedMonth),
                le, to - 40.0f, textPaint
            )

            linePaint.strokeWidth = 2.0f
            fillPaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)
            for (i in 1..20) {
                linePaint.color = Color.argb(0.4f, 0.5f, 0.5f, 0.5f)
                canvas.drawRect(le, to + i * 80.0f, le + wi, to + i * 80.0f + 40.0f, fillPaint)

                canvas.drawLine(le, to + i * 80.0f, le + wi, to + i * 80.0f, linePaint)
                canvas.drawLine(le, to + i * 80.0f + 40.0f, le + wi, to + i * 80.0f + 40.0f, linePaint)
                if (i % 2 == 1) {
                    linePaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
                    canvas.drawLine(le, to + i * 80.0f, le + wi, to + i * 80.0f, linePaint)
                }
            }

            fillPaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
            canvas.drawRect(le, to, le + wi, to + 80.0f, fillPaint)

            linePaint.strokeWidth = 2.0f
            linePaint.color = Color.argb(1.0f, 0.0f, 0.0f, 0.0f)
            canvas.drawRect(le, to, le + wi, to + 1680.0f, linePaint)
            canvas.drawLine(le + 200.0f, to, le + 200.0f, to + 1680.0f, linePaint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 60.0f
            canvas.drawText(formattedDay, le + 20.0f, to + 60.0f, textPaint)
            canvas.drawText(formattedDate, le + 220.0f, to + 60.0f, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            if (withHours) {
                for (i in 1..10) {
                    textPaint.textSize = 30.0f
                    textPaint.color = Color.argb(0.8f, 0.0f, 0.0f, 0.0f)
                    canvas.drawText("${i + 7}", le + 90.0f, to - 50.0f + i * 160.0f, textPaint)
                    textPaint.textSize = 25.0f
                    textPaint.color = Color.argb(0.5f, 0.0f, 0.0f, 0.0f)
                    canvas.drawText(":00", le + 190.0f, to - 50.0f + i * 160.0f, textPaint)
                    canvas.drawText(":15", le + 190.0f, to - 10.0f + i * 160.0f, textPaint)
                    canvas.drawText(":30", le + 190.0f, to + 30.0f + i * 160.0f, textPaint)
                    canvas.drawText(":45", le + 190.0f, to + 70.0f + i * 160.0f, textPaint)
                }
            }

            if (withTasks) {
                val h = if (withNotes) 9 else 20
                val o = 0

                linePaint.strokeWidth = 2.0f
                fillPaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)
                for (i in 1..h) {
                    linePaint.color = Color.argb(0.4f, 0.5f, 0.5f, 0.5f)
                    canvas.drawRect(
                        le + wi + 50.0f,
                        to + o + i * 80.0f,
                        le + wi + 400.0f,
                        to + o + i * 80.0f + 40.0f,
                        fillPaint
                    )

                    canvas.drawLine(
                        le + wi + 50.0f,
                        to + o + i * 80.0f,
                        le + wi + 400.0f,
                        to + o + i * 80.0f,
                        linePaint
                    )
                    canvas.drawLine(
                        le + wi + 50.0f,
                        to + o + i * 80.0f + 40.0f,
                        le + wi + 400.0f,
                        to + o + i * 80.0f + 40.0f,
                        linePaint
                    )
                }

                if (withNotes) {
                    canvas.drawRect(
                        le + wi + 50.0f,
                        to + o + 800.0f,
                        le + wi + 400.0f,
                        to + o + 800.0f + 40.0f,
                        fillPaint
                    )
                    canvas.drawLine(
                        le + wi + 50.0f,
                        to + o + 800.0f,
                        le + wi + 400.0f,
                        to + o + 800.0f,
                        linePaint
                    )
                }

                fillPaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
                canvas.drawRect(le + wi + 50.0f, to + o, le + wi + 400.0f, to + o + 80.0f, fillPaint)

                linePaint.strokeWidth = 2.0f
                linePaint.color = Color.argb(1.0f, 0.0f, 0.0f, 0.0f)
                if (withNotes) {
                    canvas.drawRect(le + wi + 50.0f, to + o, le + wi + 400.0f, to + o + 120.0f + h * 80.0f, linePaint)
                } else {
                    canvas.drawRect(le + wi + 50.0f, to + o, le + wi + 400.0f, to + o + 80.0f + h * 80.0f, linePaint)
                }

                textPaint.color = Color.WHITE
                textPaint.textSize = 60.0f
                canvas.drawText(
                    context.getString(R.string.templates_boxed_day_calendar_creator_tasks),
                    le + wi + 230.0f,
                    to + o + 60.0f,
                    textPaint
                )
            }

            if (withNotes) {
                val h = if (withTasks) 9 else 20
                val o = if (withTasks) 880.0f else 0.0f

                linePaint.strokeWidth = 2.0f
                fillPaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)
                for (i in 1..h) {
                    linePaint.color = Color.argb(0.4f, 0.5f, 0.5f, 0.5f)
                    canvas.drawRect(
                        le + wi + 50.0f,
                        to + o + i * 80.0f,
                        le + wi + 400.0f,
                        to + o + i * 80.0f + 40.0f,
                        fillPaint
                    )

                    canvas.drawLine(
                        le + wi + 50.0f,
                        to + o + i * 80.0f,
                        le + wi + 400.0f,
                        to + o + i * 80.0f,
                        linePaint
                    )
                    canvas.drawLine(
                        le + wi + 50.0f,
                        to + o + i * 80.0f + 40.0f,
                        le + wi + 400.0f,
                        to + o + i * 80.0f + 40.0f,
                        linePaint
                    )
                }

                fillPaint.color = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
                canvas.drawRect(le + wi + 50.0f, to + o, le + wi + 400.0f, to + o + 80.0f, fillPaint)

                linePaint.strokeWidth = 2.0f
                linePaint.color = Color.argb(1.0f, 0.0f, 0.0f, 0.0f)
                canvas.drawRect(le + wi + 50.0f, to + o, le + wi + 400.0f, to + o + 80.0f + h * 80.0f, linePaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 60.0f
                canvas.drawText(
                    context.getString(R.string.templates_boxed_day_calendar_creator_notes),
                    le + wi + 230.0f,
                    to + o + 60.0f,
                    textPaint
                )
            }
        }
    }
}

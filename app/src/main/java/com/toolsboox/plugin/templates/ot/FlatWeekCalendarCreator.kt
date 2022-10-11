package com.toolsboox.plugin.templates.ot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.toolsboox.R
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.*

/**
 * Create flat week of year calendar template.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class FlatWeekCalendarCreator {

    companion object {

        /**
         * Draw the page of the flat weeks of year calendar.
         *
         * @param context the context
         * @param canvas the canvas
         * @param we the week of year
         * @param wr the width ratio of the vertical line (default 0.5f)
         * @param wd generate with days
         */
        fun drawPage(context: Context, canvas: Canvas, we: Long, wr: Float, wd: Boolean) {
            val fillPaint = Paint()
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = Color.WHITE

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, fillPaint)

            val le = 0.0f  // Left offset
            val to = 100.0f // Top offset
            val wi = 1404.0f * wr // Width
            val he = 250.0f // Height

            val linePaint = Paint()
            linePaint.style = Paint.Style.STROKE
            linePaint.color = Color.BLACK

            val textPaint = Paint()
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.color = Color.BLACK
            textPaint.textSize = 40.0f

            val localDate = LocalDate.of(LocalDate.now().year, 1, 1).plusWeeks(we)
                .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)

            val year = localDate.year
            val weekOfYear = localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear())

            textPaint.textSize = 80.0f
            if (wd) {
                canvas.drawText(
                    context.getString(R.string.templates_flat_week_calendar_creator_title, year, weekOfYear),
                    100.0f, 80.0f,
                    textPaint
                )
            }

            linePaint.strokeWidth = 2.0f
            linePaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)
            for (i in 1..(7 * he / 50.0f).toInt()) {
                canvas.drawLine(le, to + i * 50.0f, le + 1404.0f, to + i * 50.0f, linePaint)
            }

            linePaint.strokeWidth = 2.0f
            linePaint.color = Color.argb(1.0f, 0.0f, 0.0f, 0.0f)
            fillPaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)
            for (i in 1..7) {
                canvas.drawRect(le, to + (i - 1) * he, le + wi, to + (i - 1) * he + 50.0f, fillPaint)
                canvas.drawLine(le, to + i * he, le + wi, to + i * he, linePaint)

                val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).plusDays(i - 1L).toInstant())
                val dayFormat = SimpleDateFormat("EEEE").format(date)
                val dateFormat = SimpleDateFormat("MM-dd").format(date)

                textPaint.textSize = 40.0f
                canvas.drawText(dayFormat, le + 10.0f, to + (i - 1) * he + 40.0f, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                if (wd) {
                    canvas.drawText(dateFormat, le + wi - 10.0f, to + (i - 1) * he + 40.0f, textPaint)
                }
                textPaint.textAlign = Paint.Align.LEFT
            }

            linePaint.strokeWidth = 5.0f
            linePaint.color = Color.argb(1.0f, 0.0f, 0.0f, 0.0f)
            canvas.drawLine(le, to, le + 1404.0f, to, linePaint)
            canvas.drawLine(le, to + 7 * he, le + 1404.0f, to + 7 * he, linePaint)
            canvas.drawLine(le + wi, to, le + wi, to + 7 * he, linePaint)
        }
    }
}

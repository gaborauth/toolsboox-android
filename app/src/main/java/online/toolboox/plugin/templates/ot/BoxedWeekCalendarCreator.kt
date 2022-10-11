package online.toolboox.plugin.templates.ot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import online.toolboox.R
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.*

/**
 * Create boxed week of year calendar template.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class BoxedWeekCalendarCreator {

    companion object {

        /**
         * Draw the page of the boxed weeks of year calendar.
         *
         * @param context the context
         * @param canvas the canvas
         * @param we the week of year
         * @param vertical the vertical day layout flag
         */
        fun drawPage(context: Context, canvas: Canvas, we: Long, vertical: Boolean) {
            val fillPaint = Paint()
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = Color.WHITE

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, fillPaint)

            if (vertical) {
                drawCalendarRect(context, canvas, 0, 0, we, 0)
                drawCalendarRect(context, canvas, 0, 1, we, 1)
                drawCalendarRect(context, canvas, 0, 2, we, 2)
                drawCalendarRect(context, canvas, 0, 3, we, 3)
                drawCalendarRect(context, canvas, 1, 0, we, 4)
                drawCalendarRect(context, canvas, 1, 1, we, 5)
                drawCalendarRect(context, canvas, 1, 2, we, 6)
                drawCalendarRect(context, canvas, 1, 3, we, 7)
            } else {
                drawCalendarRect(context, canvas, 0, 0, we, 0)
                drawCalendarRect(context, canvas, 1, 0, we, 1)
                drawCalendarRect(context, canvas, 0, 1, we, 2)
                drawCalendarRect(context, canvas, 1, 1, we, 3)
                drawCalendarRect(context, canvas, 0, 2, we, 4)
                drawCalendarRect(context, canvas, 1, 2, we, 5)
                drawCalendarRect(context, canvas, 0, 3, we, 6)
                drawCalendarRect(context, canvas, 1, 3, we, 7)
            }
        }

        /**
         * Draw one day of week box.
         *
         * @param context the context
         * @param canvas the canvas
         * @param xo the X offset
         * @param yo the Y offset
         * @param we the week of year
         * @param dw the day offset in the week
         */
        private fun drawCalendarRect(context: Context, canvas: Canvas, xo: Int, yo: Int, we: Long, dw: Long) {
            val le = 50.0f  // Left offset
            val to = 100.0f // Top offset
            val wi = 630.0f // Width
            val he = 400.0f // Height
            val hg = 44.0f // Horizontal gap
            val vg = 43.0f // Vertical gap

            val fillPaint = Paint()
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)

            val linePaint = Paint()
            linePaint.style = Paint.Style.STROKE
            linePaint.color = Color.BLACK

            val textPaint = Paint()
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.color = Color.BLACK
            textPaint.textSize = 40.0f

            canvas.drawRect(
                le + xo * (wi + hg), to + yo * (he + vg),
                le + xo * (wi + hg) + wi, to + yo * (he + vg) + 50.0f,
                fillPaint
            )
            canvas.drawRect(
                le + xo * (wi + hg), to + yo * (he + vg),
                le + xo * (wi + hg) + wi, to + yo * (he + vg) + he,
                linePaint
            )

            linePaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)
            linePaint.strokeWidth = 2.0f
            for (i in 1..7) {
                canvas.drawLine(
                    le + xo * (wi + hg), to + yo * (he + vg) + i * 50.0f,
                    le + xo * (wi + hg) + wi, to + yo * (he + vg) + i * 50.0f,
                    linePaint
                )
            }

            val localDate = LocalDate.of(LocalDate.now().year, 1, 1).plusWeeks(we)
                .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), (dw % 7L) + 1)
            val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

            if (dw == 7L) {
                val year = localDate.year
                val weekOfYear = localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear())

                textPaint.textSize = 80.0f
                canvas.drawText(
                    context.getString(R.string.templates_boxed_week_calendar_creator_title, year, weekOfYear),
                    100.0f, 80.0f,
                    textPaint
                )

                textPaint.textSize = 40.0f
                canvas.drawText(
                    context.getString(R.string.templates_boxed_week_calendar_creator_weekly_notes),
                    le + xo * (wi + hg) + 10.0f, to + yo * (he + vg) + 40.0f,
                    textPaint
                )
            } else {
                val dayFormat = SimpleDateFormat("EEEE").format(date)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd").format(date)

                textPaint.textSize = 40.0f
                canvas.drawText(
                    dayFormat,
                    le + xo * (wi + hg) + 10.0f, to + yo * (he + vg) + 40.0f,
                    textPaint
                )

                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(
                    dateFormat,
                    le + xo * (wi + hg) + wi - 10.0f, to + yo * (he + vg) + 40.0f,
                    textPaint
                )
            }
        }
    }
}

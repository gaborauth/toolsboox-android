package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.toolsboox.R
import com.toolsboox.ot.Creator

/**
 * Shared renderer for the navigator strip that sits at the top of every calendar
 * page (Day, Week, Month, Quarter, Year). Clean white background, single bottom
 * underline, Atkinson Hyperlegible font, thin separators between slots.
 *
 * Each navigator builds a list of [Slot]s with one marked [Emphasis.FOCAL]
 * (the granularity currently being viewed). The focal slot renders larger and
 * bolder; the rest are normal or muted depending on relevance.
 */
object NavigatorRenderer {

    enum class Emphasis { FOCAL, NORMAL, MUTED }

    data class Slot(
        val text: String,
        val emphasis: Emphasis = Emphasis.NORMAL,
        val hasPages: Boolean = false,
        val noteCount: Int = 0,
    )

    fun render(context: Context, canvas: Canvas, slots: List<Slot>) {
        canvas.drawRect(0.0f, 0.0f, 1404.0f, 140.4f, Creator.fillWhite)

        val atkinsonBold = try {
            ResourcesCompat.getFont(context, R.font.atkinson_hyperlegible_bold)
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
        val atkinsonReg = try {
            ResourcesCompat.getFont(context, R.font.atkinson_hyperlegible_regular)
        } catch (_: Exception) {
            Typeface.DEFAULT
        }

        val focalPaint = TextPaint().apply {
            color = Color.BLACK; textAlign = Paint.Align.CENTER; textSize = 92f
            typeface = atkinsonBold; isAntiAlias = true
        }
        val normalPaint = TextPaint().apply {
            color = Color.BLACK; textAlign = Paint.Align.CENTER; textSize = 68f
            typeface = atkinsonReg; isAntiAlias = true
        }
        val mutedPaint = TextPaint().apply {
            color = Color.argb(180, 0, 0, 0); textAlign = Paint.Align.CENTER; textSize = 56f
            typeface = atkinsonReg; isAntiAlias = true
        }
        val arrowPaint = TextPaint().apply {
            color = Color.argb(180, 0, 0, 0); textAlign = Paint.Align.CENTER; textSize = 76f
            typeface = atkinsonBold; isAntiAlias = true
        }
        val separator = Paint().apply {
            color = Color.argb(80, 0, 0, 0); strokeWidth = 1.5f; style = Paint.Style.STROKE
        }
        val underline = Paint().apply {
            color = Color.BLACK; strokeWidth = 2.0f; style = Paint.Style.STROKE
        }

        val rowCenterY = 98f
        canvas.drawText("<", 60f, rowCenterY, arrowPaint)
        canvas.drawText(">", 1344f, rowCenterY, arrowPaint)

        val slotsLeft = 140f
        val slotsRight = 1264f
        val slotWidth = (slotsRight - slotsLeft) / slots.size

        for ((idx, slot) in slots.withIndex()) {
            val cx = slotsLeft + slotWidth * (idx + 0.5f)
            val paint = when (slot.emphasis) {
                Emphasis.FOCAL -> focalPaint
                Emphasis.NORMAL -> normalPaint
                Emphasis.MUTED -> mutedPaint
            }
            canvas.drawText(slot.text, cx, rowCenterY, paint)

            if (idx > 0) {
                val sx = slotsLeft + slotWidth * idx
                canvas.drawLine(sx, 30f, sx, 110f, separator)
            }
            if (slot.hasPages) {
                Creator.drawTriangle(canvas, cx - slotWidth * 0.4f, 20f, 14f)
            }
            if (slot.noteCount > 0) {
                Creator.notesDots(canvas, cx - slotWidth * 0.4f, 130f, 4.0f, slot.noteCount)
            }
        }

        canvas.drawLine(0.0f, 138.4f, 1404.0f, 138.4f, underline)
    }
}

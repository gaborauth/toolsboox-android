package com.toolsboox.ot

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import timber.log.Timber
import kotlin.math.min

/**
 * Creator interface, common drawing methods and constants.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface Creator {

    companion object {
        val colorBlack = Color.argb(1.0f, 0.0f, 0.0f, 0.0f)
        val colorGrey20 = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)
        val colorGrey50 = Color.argb(0.5f, 0.5f, 0.5f, 0.5f)
        val colorGrey80 = Color.argb(0.8f, 0.5f, 0.5f, 0.5f)
        val colorWhite = Color.argb(1.0f, 1.0f, 1.0f, 1.0f)

        val fillBlack = Paint()
        val fillGrey20 = Paint()
        val fillGrey80 = Paint()
        val fillWhite = Paint()

        val lineDefaultBlack: Paint = Paint()
        val lineDefaultGrey50: Paint = Paint()
        val lineDefaultWhite: Paint = Paint()

        val text60BlackCenter = TextPaint()
        val textBigBlackCenter = TextPaint()
        val textBigGray20Right = TextPaint()
        val textDefaultBlack = TextPaint()
        val textDefaultBlackCenter = TextPaint()
        val textDefaultBlackRight = TextPaint()
        val textDefaultGray20Center = TextPaint()
        val textDefaultWhite = TextPaint()
        val textDefaultWhiteCenter = TextPaint()
        val textDefaultWhiteRight = TextPaint()
        val textSmallBlack = TextPaint()
        val textSmallBlackCenter = TextPaint()
        val textSmallBlackRight = TextPaint()


        init {
            // Fill styles
            fillBlack.strokeWidth = 1.0f
            fillBlack.color = Color.BLACK
            fillBlack.style = Paint.Style.FILL_AND_STROKE

            fillGrey20.strokeWidth = 1.0f
            fillGrey20.color = colorGrey20
            fillGrey20.style = Paint.Style.FILL_AND_STROKE

            fillGrey80.strokeWidth = 1.0f
            fillGrey80.color = colorGrey80
            fillGrey80.style = Paint.Style.FILL_AND_STROKE

            fillWhite.strokeWidth = 1.0f
            fillWhite.color = colorWhite
            fillWhite.style = Paint.Style.FILL_AND_STROKE

            // Line styles
            lineDefaultBlack.color = colorBlack
            lineDefaultBlack.strokeWidth = 2.0f
            lineDefaultBlack.style = Paint.Style.STROKE

            lineDefaultGrey50.color = colorGrey50
            lineDefaultGrey50.strokeWidth = 2.0f
            lineDefaultGrey50.style = Paint.Style.STROKE

            lineDefaultWhite.color = colorWhite
            lineDefaultWhite.strokeWidth = 2.0f
            lineDefaultWhite.style = Paint.Style.STROKE

            // Text styles
            text60BlackCenter.color = colorBlack
            text60BlackCenter.textAlign = Paint.Align.CENTER
            text60BlackCenter.textSize = 60.0f
            text60BlackCenter.typeface = Typeface.DEFAULT

            textBigBlackCenter.color = colorBlack
            textBigBlackCenter.textAlign = Paint.Align.CENTER
            textBigBlackCenter.textSize = 80.0f
            textBigBlackCenter.typeface = Typeface.DEFAULT_BOLD

            textBigGray20Right.color = colorGrey20
            textBigGray20Right.textAlign = Paint.Align.RIGHT
            textBigGray20Right.textSize = 160.0f
            textBigGray20Right.typeface = Typeface.DEFAULT_BOLD

            textDefaultBlack.color = colorBlack
            textDefaultBlack.textSize = 40.0f
            textDefaultBlack.typeface = Typeface.DEFAULT_BOLD

            textDefaultBlackCenter.color = colorBlack
            textDefaultBlackCenter.textAlign = Paint.Align.CENTER
            textDefaultBlackCenter.textSize = 40.0f
            textDefaultBlackCenter.typeface = Typeface.DEFAULT_BOLD

            textDefaultBlackRight.color = colorBlack
            textDefaultBlackRight.textAlign = Paint.Align.RIGHT
            textDefaultBlackRight.textSize = 40.0f
            textDefaultBlackRight.typeface = Typeface.DEFAULT_BOLD

            textDefaultGray20Center.color = colorGrey20
            textDefaultGray20Center.textAlign = Paint.Align.CENTER
            textDefaultGray20Center.textSize = 40.0f
            textDefaultGray20Center.typeface = Typeface.DEFAULT_BOLD

            textDefaultWhite.color = colorWhite
            textDefaultWhite.textAlign = Paint.Align.LEFT
            textDefaultWhite.textSize = 40.0f
            textDefaultWhite.typeface = Typeface.DEFAULT_BOLD

            textDefaultWhiteCenter.color = colorWhite
            textDefaultWhiteCenter.textAlign = Paint.Align.CENTER
            textDefaultWhiteCenter.textSize = 40.0f
            textDefaultWhiteCenter.typeface = Typeface.DEFAULT_BOLD

            textDefaultWhiteRight.color = colorWhite
            textDefaultWhiteRight.textAlign = Paint.Align.RIGHT
            textDefaultWhiteRight.textSize = 40.0f
            textDefaultWhiteRight.typeface = Typeface.DEFAULT_BOLD

            textSmallBlack.color = colorBlack
            textSmallBlack.textAlign = Paint.Align.LEFT
            textSmallBlack.textSize = 25.0f
            textSmallBlack.typeface = Typeface.DEFAULT

            textSmallBlackCenter.color = colorBlack
            textSmallBlackCenter.textAlign = Paint.Align.CENTER
            textSmallBlackCenter.textSize = 25.0f
            textSmallBlackCenter.typeface = Typeface.DEFAULT

            textSmallBlackRight.color = colorBlack
            textSmallBlackRight.textAlign = Paint.Align.RIGHT
            textSmallBlackRight.textSize = 25.0f
            textSmallBlackRight.typeface = Typeface.DEFAULT
        }

        /**
         * Draw a drawable in a canvas.
         *
         * @param context the context
         * @param canvas the canvas
         * @param drawable the drawable
         * @param left the left point
         * @param top the top point
         * @param right the right point
         * @param bottom the bottom point
         */
        fun drawable(
            context: Context, canvas: Canvas, drawable: Int,
            left: Float, top: Float, right: Float, bottom: Float
        ) {
            val d = ResourcesCompat.getDrawable(context.resources, drawable, context.theme) ?: return
            d.setTint(Color.BLACK)
            d.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            d.draw(canvas)
        }

        /**
         * Draw ellipsized text.
         *
         * @param canvas the canvas
         * @param text the text
         * @param style the text style
         * @param x the X coordinate
         * @param y the Y coordinate
         * @param width the width of the space
         */
        fun drawEllipsizedText(canvas: Canvas, text: String, style: TextPaint, x: Float, y: Float, width: Float) {
            val ellipsizedText = TextUtils.ellipsize(text, style, width, TextUtils.TruncateAt.END).toString()
            if (style.textAlign == Paint.Align.CENTER) {
                canvas.drawText(ellipsizedText, x + width / 2, y, style)
            } else {
                canvas.drawText(ellipsizedText, x, y, style)
            }
        }

        /**
         * Draw circle.
         *
         * @param canvas the canvas
         * @param x the x coordinate
         * @param y the y coordinate
         * @param size the size of the circle
         * @param fillPaint optional fill paint
         */
        private fun drawCircle(canvas: Canvas, x: Float, y: Float, size: Float, fillPaint: Paint = fillBlack) {
            canvas.drawOval(x - size, y - size, x + size, y + size, fillPaint)
        }

        /**
         * Draw top-left triangle.
         *
         * @param canvas the canvas
         * @param x the x coordinate
         * @param y the y coordinate
         * @param size the size of the triangle
         * @param fillPaint optional fill paint
         */
        fun drawTriangle(canvas: Canvas, x: Float, y: Float, size: Float, fillPaint: Paint = fillBlack) {
            val path = Path()
            path.fillType = Path.FillType.EVEN_ODD
            path.moveTo(x + 0.0f, y + 0.0f)
            path.lineTo(x + size, y + 0.0f)
            path.lineTo(x + 0.0f, y + size)
            path.lineTo(x + 0.0f, y + 0.0f)
            path.close()

            canvas.drawPath(path, fillPaint)
        }

        /**
         * Draw note dots (max 5).
         *
         * @param canvas the canvas
         * @param x the x coordinate
         * @param y the y coordinate
         * @param size the size of the dots
         * @param notePages the number of pages
         * @param color optional fill color
         */
        fun notesDots(canvas: Canvas, x: Float, y: Float, size: Float, notePages: Int, color: Int = Color.BLACK) {
            Timber.i("$notePages")
            for (i in 0 until min(notePages, 5)) {
                if (color == Color.BLACK) {
                    drawCircle(canvas, x + 2 * i * size, y, size, fillBlack)
                } else {
                    drawCircle(canvas, x + 2 * i * size, y, size, fillWhite)
                }
            }
            if (notePages > 5) {
                if (color == Color.BLACK) {
                    canvas.drawLine(x + 10 * size - size, y, x + 10 * size + size, y, lineDefaultBlack)
                    canvas.drawLine(x + 10 * size, y - size, x + 10 * size, y + size, lineDefaultBlack)
                } else {
                    canvas.drawLine(x + 10 * size - size, y, x + 10 * size + size, y, lineDefaultWhite)
                    canvas.drawLine(x + 10 * size, y - size, x + 10 * size, y + size, lineDefaultWhite)
                }
            }
        }
    }
}
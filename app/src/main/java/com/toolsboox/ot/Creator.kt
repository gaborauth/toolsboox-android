package com.toolsboox.ot

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint

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

        val fillGrey20 = Paint()
        val fillGrey80 = Paint()
        val fillWhite = Paint()

        val lineDefaultBlack: Paint = Paint()
        val lineDefaultGrey50: Paint = Paint()

        val textDefaultBlack = TextPaint()
        val textDefaultBlackCenter = TextPaint()
        val textDefaultBlackRight = TextPaint()
        val textDefaultWhite = TextPaint()
        val textDefaultWhiteCenter = TextPaint()
        val textDefaultWhiteRight = TextPaint()
        val textSmallBlack = TextPaint()
        val textSmallBlackCenter = TextPaint()
        val textSmallBlackRight = TextPaint()


        init {
            // Fill styles
            fillGrey20.color = colorGrey20
            fillGrey20.style = Paint.Style.FILL

            fillGrey80.color = colorGrey80
            fillGrey80.style = Paint.Style.FILL

            fillWhite.color = colorWhite
            fillWhite.style = Paint.Style.FILL

            // Line styles
            lineDefaultBlack.color = colorBlack
            lineDefaultBlack.strokeWidth = 2.0f
            lineDefaultBlack.style = Paint.Style.STROKE

            lineDefaultGrey50.color = colorGrey50
            lineDefaultGrey50.strokeWidth = 2.0f
            lineDefaultGrey50.style = Paint.Style.STROKE

            // Text styles
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
    }
}
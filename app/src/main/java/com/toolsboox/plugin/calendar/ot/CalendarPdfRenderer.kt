package com.toolsboox.plugin.calendar.ot

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.toolsboox.da.Stroke
import java.io.File
import java.io.FileOutputStream

/**
 * Renders calendar page strokes to PDF documents using android.graphics.pdf.PdfDocument.
 *
 * Page dimensions default to 1404x1872 to match Boox e-ink portrait mode.
 * All rendering is CPU-bound and must run on Dispatchers.Default, not Main.
 */
object CalendarPdfRenderer {

    /**
     * Render a single calendar page (calendar strokes + note strokes) to a one-page PDF.
     *
     * @param strokes the calendar strokes map (style key -> stroke list)
     * @param noteStrokes the note strokes map (page key -> stroke list)
     * @param outputFile the destination PDF file
     * @param pageWidth the page width in pixels (default 1404)
     * @param pageHeight the page height in pixels (default 1872)
     */
    fun renderPageToPdf(
        strokes: Map<String, List<Stroke>>,
        noteStrokes: Map<String, List<Stroke>>,
        outputFile: File,
        pageWidth: Int = 1404,
        pageHeight: Int = 1872
    ) {
        val pdf = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            val paint = createStrokePaint()
            drawAllStrokes(canvas, paint, strokes)
            drawAllStrokes(canvas, paint, noteStrokes)

            pdf.finishPage(page)

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }

    /**
     * Render multiple calendar pages into a single multi-page PDF.
     * Each entry in [pages] produces one PDF page, in order.
     *
     * @param pages list of (label, (calendarStrokes, noteStrokes)) pairs
     * @param outputFile the destination PDF file
     * @param pageWidth the page width in pixels (default 1404)
     * @param pageHeight the page height in pixels (default 1872)
     */
    fun renderMonthToPdf(
        pages: List<Pair<String, Pair<Map<String, List<Stroke>>, Map<String, List<Stroke>>>>>,
        outputFile: File,
        pageWidth: Int = 1404,
        pageHeight: Int = 1872
    ) {
        if (pages.isEmpty()) return

        val pdf = PdfDocument()
        try {
            val paint = createStrokePaint()

            pages.forEachIndexed { index, (_, strokePair) ->
                val (calendarStrokes, noteStrokes) = strokePair
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)
                drawAllStrokes(canvas, paint, calendarStrokes)
                drawAllStrokes(canvas, paint, noteStrokes)

                pdf.finishPage(page)
            }

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }

    /**
     * Create a reusable Paint configured for stroke rendering.
     */
    private fun createStrokePaint(): Paint {
        return Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            strokeWidth = 3f
        }
    }

    /**
     * Draw all strokes from a stroke map onto a canvas.
     */
    private fun drawAllStrokes(
        canvas: Canvas,
        paint: Paint,
        strokeMap: Map<String, List<Stroke>>
    ) {
        for ((_, strokeList) in strokeMap) {
            for (stroke in strokeList) {
                drawStroke(canvas, paint, stroke)
            }
        }
    }

    /**
     * Draw a single stroke as a path on the canvas.
     * Pressure (StrokePoint.p) modulates the stroke width for a more natural look.
     */
    private fun drawStroke(canvas: Canvas, basePaint: Paint, stroke: Stroke) {
        val points = stroke.strokePoints
        if (points.size < 2) return

        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }

        val avgPressure = points.map { it.p }.average().toFloat().coerceIn(0.1f, 2.0f)
        val paint = Paint(basePaint).apply {
            color = stroke.color
            strokeWidth = stroke.strokeWidth * avgPressure
        }

        canvas.drawPath(path, paint)
    }
}

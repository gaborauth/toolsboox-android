package com.toolsboox.ui.plugin

import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import com.toolsboox.plugin.teamdrawer.nw.domain.StrokePoint
import timber.log.Timber
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * SurfaceView fragment of Boox pen supports.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
abstract class SurfaceFragment : ScreenFragment() {

    /**
     * The paint in the bitmap.
     */
    protected var paint = Paint()

    /**
     * TouchHelper of the Onyx's pen.
     */
    protected lateinit var touchHelper: TouchHelper

    /**
     * The bitmap of the canvas.
     */
    protected lateinit var bitmap: Bitmap

    /**
     * The canvas of the surface view.
     */
    protected lateinit var canvas: Canvas

    /**
     * The callback of the surface holder.
     */
    protected var surfaceCallback: SurfaceHolder.Callback? = null

    /**
     * The list of strokes.
     */
    protected var strokes: List<Stroke> = mutableListOf()

    /**
     * SurfaceView provide method.
     *
     * @return the actual surfaceView
     */
    abstract fun provideSurfaceView(): SurfaceView

    /**
     * Add stroke callback.
     *
     * @param stroke list of stroke points
     */
    abstract fun addStroke(stroke: List<StrokePoint>)

    /**
     * Delete stroke callback.
     *
     * @param strokeId the UUID of the stroke
     */
    abstract fun delStroke(strokeId: UUID)

    /**
     * Initialize the surface view of Onyx's drawing.
     */
    fun initializeSurface() {
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 3.0f

        if (surfaceCallback == null) {
            surfaceCallback = object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    Timber.i("surfaceCreated")
                    val limit = Rect()
                    provideSurfaceView().getLocalVisibleRect(limit)
                    bitmap = Bitmap.createBitmap(
                        provideSurfaceView().width,
                        provideSurfaceView().height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.TRANSPARENT)
                    canvas = Canvas(bitmap)

                    if (provideSurfaceView().holder == null) {
                        return
                    }

                    clearSurface()

                    touchHelper.setLimitRect(limit, ArrayList())
                        .setStrokeWidth(3.0f)
                        .openRawDrawing()
                    touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_BRUSH)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    Timber.i("surfaceChanged")
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    Timber.i("surfaceDestroyed")
                    holder.removeCallback(surfaceCallback)
                    surfaceCallback = null
                }
            }
        }

        provideSurfaceView().holder.addCallback(surfaceCallback)
        provideSurfaceView().viewTreeObserver.addOnGlobalLayoutListener {
            Timber.i("addOnGlobalLayoutListener")
            touchHelper.setRawDrawingEnabled(true)
        }
    }

    /**
     * Clear the surface and the shadow canvas.
     */
    fun clearSurface() {
        val lockerCanvas = provideSurfaceView().holder.lockCanvas() ?: return
        EpdController.enablePost(provideSurfaceView(), 1)
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = Color.TRANSPARENT
        val rect = Rect(0, 0, provideSurfaceView().width, provideSurfaceView().height)
        lockerCanvas.drawRect(rect, paint)
        provideSurfaceView().holder.unlockCanvasAndPost(lockerCanvas)

        canvas.drawRect(rect, paint)
    }

    /**
     * The raw input callback of Onyx's pen library.
     */
    val callback: RawInputCallback = object : RawInputCallback() {
        var lastPoint: TouchPoint? = null

        private fun epsilon(touchPoint: TouchPoint, lastPoint: TouchPoint, epsilon: Float): Boolean {
            return epsilon(touchPoint.x, touchPoint.y, lastPoint.x, lastPoint.y, epsilon)
        }

        private fun epsilon(x1: Float, y1: Float, x2: Float, y2: Float, epsilon: Float): Boolean {
            val dx = abs(x1 - x2).toDouble()
            val dy = abs(y1 - y2).toDouble()
            val d = sqrt(dx * dx + dy * dy)
            return d <= epsilon
        }

        override fun onBeginRawDrawing(b: Boolean, touchPoint: TouchPoint) {
            Timber.i("onBeginRawDrawing (${touchPoint.x}/${touchPoint.y})")
            lastPoint = touchPoint
        }

        override fun onEndRawDrawing(b: Boolean, touchPoint: TouchPoint) {
            Timber.i("onEndRawDrawing (${touchPoint.x}/${touchPoint.y})")
            lastPoint = null
        }

        override fun onRawDrawingTouchPointMoveReceived(touchPoint: TouchPoint) {
            Timber.i("onRawDrawingTouchPointMoveReceived (${touchPoint.x}/${touchPoint.y} - ${touchPoint.pressure})")
        }

        override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList) {
            Timber.i("onRawDrawingTouchPointListReceived (${touchPointList.size()})")

            val stroke: MutableList<StrokePoint> = mutableListOf()
            var prevPoint: TouchPoint = touchPointList[0]
            stroke.add(
                StrokePoint(
                    (10 * prevPoint.x).roundToInt() / 10.0f,
                    (10 * prevPoint.y).roundToInt() / 10.0f,
                    (10 * prevPoint.pressure).roundToInt() / 10.0f
                )
            )
            for (tp in touchPointList) {
                if (!epsilon(tp, prevPoint, 3.0f) and epsilon(tp, prevPoint, 30.0f)) {
                    prevPoint = tp
                    stroke.add(
                        StrokePoint(
                            (10 * tp.x).roundToInt() / 10.0f,
                            (10 * tp.y).roundToInt() / 10.0f,
                            (10 * tp.pressure).roundToInt() / 10.0f
                        )
                    )
                }
            }
            addStroke(stroke)
        }

        override fun onBeginRawErasing(b: Boolean, touchPoint: TouchPoint) {
            Timber.i("onBeginRawErasing (${touchPoint.x} - ${touchPoint.y})")
        }

        override fun onEndRawErasing(b: Boolean, touchPoint: TouchPoint) {
            Timber.d("onEndRawErasing (${touchPoint.x} - ${touchPoint.y})")
        }

        override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint) {
            Timber.d("onRawErasingTouchPointMoveReceived (${touchPoint.x} - ${touchPoint.y})")
        }

        override fun onRawErasingTouchPointListReceived(touchPointList: TouchPointList) {
            Timber.d("onRawErasingTouchPointListReceived (${touchPointList.size()})")

            val eraserPoints: MutableList<TouchPoint> = mutableListOf()
            var prevPoint: TouchPoint = touchPointList[0]
            eraserPoints.add(prevPoint)
            for (tp in touchPointList) {
                if (!epsilon(tp, prevPoint, 5.0f)) {
                    prevPoint = tp
                    eraserPoints.add(prevPoint)
                }
            }

            Timber.i("onRawErasingTouchPointListReceived ($eraserPoints)")
            for (ep in eraserPoints) {
                for (stroke in strokes) {
                    for (tp in stroke.strokePoints) {
                        if (epsilon(ep.x, ep.y, tp.x, tp.y, 10.0f)) {
                            delStroke(stroke.strokeId)
                            return
                        }
                    }
                }
            }
        }
    }
}
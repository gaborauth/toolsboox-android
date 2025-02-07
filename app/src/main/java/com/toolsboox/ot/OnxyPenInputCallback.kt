package com.toolsboox.ot

import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.data.TouchPointList
import com.toolsboox.da.StrokePoint
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import timber.log.Timber
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The raw input callback of Onyx's pen library.
 *
 * @param penCallback pen callback
 * @param eraseCallback erase callback
 */
class OnxyPenInputCallback(private val penCallback: PenCallback?, private val eraseCallback: EraseCallback?) : RawInputCallback() {

    private var lastPoint: TouchPoint? = null
    private var strokes: List<Stroke> = mutableListOf()

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
        penCallback?.addStrokes(stroke)
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
                        eraseCallback?.removeStroke(stroke.strokeId)
                        return
                    }
                }
            }
        }
    }

    interface PenCallback {
        fun addStrokes(strokes: List<StrokePoint>)
    }

    interface EraseCallback {
        fun removeStroke(strokeId: UUID)
    }
}

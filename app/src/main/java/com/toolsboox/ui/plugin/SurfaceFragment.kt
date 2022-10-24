package com.toolsboox.ui.plugin

import android.Manifest
import android.graphics.*
import android.provider.MediaStore
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import com.toolsboox.R
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import com.toolsboox.plugin.teamdrawer.nw.domain.StrokePoint
import timber.log.Timber
import java.time.Instant
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
    private var paint = Paint()

    /**
     * TouchHelper of the Onyx's pen.
     */
    private lateinit var touchHelper: TouchHelper

    /**
     * The bitmap of the canvas.
     */
    private lateinit var bitmap: Bitmap

    /**
     * The canvas of the surface view.
     */
    private lateinit var canvas: Canvas

    /**
     * The callback of the surface holder.
     */
    private var surfaceCallback: SurfaceHolder.Callback? = null

    /**
     * The list of strokes.
     */
    private var strokes: MutableList<Stroke> = mutableListOf()

    /**
     * The actual size of the surface.
     */
    private var surfaceSize: Rect = Rect(0, 0, 0, 0)

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
    open fun onStrokeAdded(stroke: List<StrokePoint>) {}

    /**
     * Delete stroke callback.
     *
     * @param strokeId the UUID of the stroke
     */
    open fun onStrokeDeleted(strokeId: UUID) {}

    /**
     * Stroke changed callback.
     *
     * @param strokes the actual strokes
     */
    open fun onStrokeChanged(strokes: MutableList<Stroke>) {}

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        initializeSurface()
        touchHelper.setRawDrawingEnabled(true)
        touchHelper.isRawDrawingRenderEnabled = true
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        touchHelper.setRawDrawingEnabled(false)
        touchHelper.isRawDrawingRenderEnabled = false

        touchHelper.closeRawDrawing()
        bitmap.recycle()
    }

    /**
     * Export bitmap to the external storage.
     */
    fun exportBitmap() {
        if (!checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showError(null, R.string.main_read_external_storage_permission_missing, provideSurfaceView())
            return
        }

        if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showError(null, R.string.main_write_external_storage_permission_missing, provideSurfaceView())
            return
        }

        val title = "export-${Instant.now().epochSecond}"
        MediaStore.Images.Media.insertImage(
            this@SurfaceFragment.requireActivity().contentResolver,
            bitmap,
            title,
            title
        )
        showMessage(getString(R.string.team_drawer_page_export_message).format(title), provideSurfaceView())
    }

    /**
     * Initialize the surface view of Onyx's drawing.
     *
     * @param first first initialization flag
     */
    fun initializeSurface(first: Boolean = false) {
        if (first) {
            touchHelper = TouchHelper.create(provideSurfaceView(), callback)
            provideSurfaceView().setZOrderOnTop(true)
            provideSurfaceView().holder.setFormat(PixelFormat.TRANSPARENT)
        }

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
                    Timber.i("surfaceChanged: ${width}x${height}")
                    surfaceSize = Rect(0, 0, width, height)
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
     * Apply strokes on the surface.
     *
     * @param strokes the list of strokes
     * @param clearPage the clear page flag
     */
    fun applyStrokes(strokes: List<Stroke>, clearPage: Boolean) {
        this.strokes = strokes.toMutableList()
        // TODO: render page when onSurfaceCreated
        val lockCanvas = provideSurfaceView().holder.lockCanvas() ?: return

        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.TRANSPARENT
        val rect = Rect(0, 0, provideSurfaceView().width, provideSurfaceView().height)
        lockCanvas.drawRect(rect, fillPaint)
        lockCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        if (clearPage) {
            canvas.drawRect(rect, fillPaint)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }

        for (stroke in strokes) {
            val points = stroke.strokePoints
            if (points.isNotEmpty()) {
                val path = Path()
                val prePoint = PointF(points[0].x, points[0].y)
                if (points.size == 1) {
                    path.moveTo(prePoint.x - 1f, prePoint.y - 1f)
                } else {
                    path.moveTo(prePoint.x, prePoint.y)
                }
                for (point in points) {
                    path.quadTo(prePoint.x, prePoint.y, point.x, point.y)
                    prePoint.x = point.x
                    prePoint.y = point.y
                }

                lockCanvas.drawPath(path, paint)
                canvas.drawPath(path, paint)
            }
        }

        touchHelper.setRawDrawingEnabled(false)
        touchHelper.isRawDrawingRenderEnabled = false
        provideSurfaceView().holder.unlockCanvasAndPost(lockCanvas)
        touchHelper.setRawDrawingEnabled(true)
        touchHelper.isRawDrawingRenderEnabled = true
    }

    /**
     * Get the size of the surface.
     *
     * @return the size of the surface
     */
    protected fun getSurfaceSize(): Rect = surfaceSize

    /**
     * The raw input callback of Onyx's pen library.
     */
    private val callback: RawInputCallback = object : RawInputCallback() {
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

        override fun onPenActive(touchPoint: TouchPoint) {
            super.onPenActive(touchPoint)
        }

        override fun onPenUpRefresh(refreshRect: RectF) {
            super.onPenUpRefresh(refreshRect)
            Timber.i("onPenUpRefresh (${refreshRect.top}x${refreshRect.left}/${refreshRect.bottom}x${refreshRect.right} )")
            applyStrokes(strokes, false)
            onStrokeChanged(strokes)
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
            Timber.d("onRawDrawingTouchPointMoveReceived (${touchPoint.x}/${touchPoint.y} - ${touchPoint.pressure})")
        }

        override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList) {
            Timber.i("onRawDrawingTouchPointListReceived (${touchPointList.size()})")

            val strokePoints: MutableList<StrokePoint> = mutableListOf()
            var prevPoint: TouchPoint = touchPointList[0]
            strokePoints.add(
                StrokePoint(
                    (10 * prevPoint.x).roundToInt() / 10.0f,
                    (10 * prevPoint.y).roundToInt() / 10.0f,
                    (10 * prevPoint.pressure).roundToInt() / 10.0f
                )
            )
            for (tp in touchPointList) {
                if (!epsilon(tp, prevPoint, 3.0f) and epsilon(tp, prevPoint, 30.0f)) {
                    prevPoint = tp
                    strokePoints.add(
                        StrokePoint(
                            (10 * tp.x).roundToInt() / 10.0f,
                            (10 * tp.y).roundToInt() / 10.0f,
                            (10 * tp.pressure).roundToInt() / 10.0f
                        )
                    )
                }
            }
            strokes.add(Stroke(UUID.randomUUID(), UUID.randomUUID(), strokePoints))
            onStrokeAdded(strokePoints)
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

            val strokesToRemove: MutableList<UUID> = mutableListOf()
            for (ep in eraserPoints) {
                for (stroke in strokes) {
                    for (tp in stroke.strokePoints) {
                        if (epsilon(ep.x, ep.y, tp.x, tp.y, 10.0f)) {
                            strokesToRemove.add(stroke.strokeId)
                        }
                    }
                }
            }
            strokesToRemove.forEach { strokes.removeIf { stroke -> stroke.strokeId == it } }

            // TODO: multiple delete of strokes
            if (strokesToRemove.firstOrNull() != null) {
                onStrokeDeleted(strokesToRemove.firstOrNull()!!)
            }

            onStrokeChanged(strokes)
            applyStrokes(strokes, true)
        }
    }
}
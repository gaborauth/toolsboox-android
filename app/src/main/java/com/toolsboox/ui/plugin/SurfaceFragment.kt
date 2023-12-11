package com.toolsboox.ui.plugin

import android.Manifest
import android.content.SharedPreferences
import android.graphics.*
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GestureDetectorCompat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.toolsboox.R
import com.toolsboox.da.Stroke
import com.toolsboox.da.StrokePoint
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.plugin.calendar.CalendarNavigator
import timber.log.Timber
import java.time.Instant
import java.util.*
import javax.inject.Inject
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
     * The injected presenter.
     */
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

    /**
     * The paint in the bitmap.
     */
    private var paint = Paint()

    /**
     * The gesture detector
     */
    protected lateinit var gestureDetector: GestureDetectorCompat

    /**
     * The gesture listener
     */
    protected lateinit var gestureListener: OnGestureListener

    /**
     * The bitmap of the canvas.
     */
    private var bitmap: Bitmap? = null

    /**
     * The canvas of the surface view.
     */
    private lateinit var canvas: Canvas

    /**
     * The callback of the surface holder.
     */
    private var surfaceCallback: SurfaceHolder.Callback? = null

    /**
     * The last point of the stroke.
     */
    private var lastPoint: StrokePoint? = null

    /**
     * The list of stylus points.
     */
    private val stylusPointList: MutableList<StrokePoint> = mutableListOf()

    /**
     * The list of strokes.
     */
    private var strokes: MutableList<Stroke> = mutableListOf()

    /**
     * The list of strokes to add.
     */
    private var strokesToAdd: MutableList<Stroke> = mutableListOf()

    /**
     * The actual size of the surface.
     */
    private var surfaceSize: Rect = Rect(0, 0, 0, 0)

    /**
     * Pen or eraser state.
     */
    private var penState: Boolean = true

    /**
     * The canvas of the navigator.
     */
    protected lateinit var navigatorCanvas: Canvas

    /**
     * The bitmap of the navigator.
     */
    protected lateinit var navigatorBitmap: Bitmap

    /**
     * The canvas of the template.
     */
    protected lateinit var templateCanvas: Canvas

    /**
     * The bitmap of the template.
     */
    protected lateinit var templateBitmap: Bitmap

    /**
     * SurfaceView provide method.
     *
     * @return the actual surfaceView
     */
    abstract fun provideSurfaceView(): SurfaceView

    /**
     * Provide toolbar of drawing's bindings.
     *
     * @return the actual bindings of toolbar of drawings
     */
    abstract fun provideToolbarDrawing(): ToolbarDrawingBinding

    /**
     * Add strokes callback.
     *
     * @param strokes list of strokes
     */
    open fun onStrokesAdded(strokes: List<Stroke>) {}

    /**
     * On side switched event.
     */
    open fun onSideSwitched() {}

    /**
     * Delete strokes callback.
     *
     * @param strokeIds the list UUID of the strokes
     */
    open fun onStrokesDeleted(strokeIds: List<UUID>) {}

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

        penState = true
        provideToolbarDrawing().toolbarPen.background.setTint(Color.GRAY)
        provideToolbarDrawing().toolbarEraser.background.setTint(Color.WHITE)

        provideToolbarDrawing().toolbarPen.setOnClickListener {
            penState = true
            provideToolbarDrawing().toolbarPen.background.setTint(Color.GRAY)
            provideToolbarDrawing().toolbarEraser.background.setTint(Color.WHITE)
        }

        provideToolbarDrawing().toolbarEraser.setOnClickListener {
            penState = false
            provideToolbarDrawing().toolbarEraser.background.setTint(Color.GRAY)
            provideToolbarDrawing().toolbarPen.background.setTint(Color.WHITE)
        }

        provideToolbarDrawing().toolbarTrash.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
            builder.setTitle(R.string.calendar_drawing_toolbar_trash_dialog_title)
                .setMessage(R.string.calendar_drawing_toolbar_trash_dialog_message)
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    val strokesToRemove: MutableSet<UUID> = mutableSetOf()
                    for (stroke in strokes) {
                        strokesToRemove.add(stroke.strokeId)
                    }
                    strokes.clear()
                    onStrokesDeleted(strokesToRemove.toList())

                    applyStrokes(strokes, true)
                    onStrokeChanged(strokes)
                    dialog.cancel()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
            builder.create().show()
        }

        provideToolbarDrawing().toolbarSwitchSide.setOnClickListener {
            onSideSwitched()
        }

        provideToolbarDrawing().toolbarCloudSync.setOnClickListener {
            CalendarNavigator.toCloudSync(this)
        }

        // Hide the cloud sync feature in case of regular users or enable it generally.
        val androidId = sharedPreferences.getString("androidId", "")
        val earlyAdopterDeviceIdsJson = sharedPreferences.getString("earlyAdopterDeviceIds", "[]")

        val earlyAdopterDeviceIdsType = Types.newParameterizedType(MutableList::class.java, String::class.java)
        val jsonAdapter = moshi.adapter<List<String>>(earlyAdopterDeviceIdsType)
        val earlyAdopterDeviceIds = jsonAdapter.fromJson(earlyAdopterDeviceIdsJson!!)

        val cloudPluginEnabled = sharedPreferences.getString("cloudPluginEnabled", "false").toBoolean()
        Timber.i("Cloud plugin enabled: $cloudPluginEnabled")

        val earlyAdopter = earlyAdopterDeviceIds?.contains(androidId) ?: false
        Timber.i("Early adopter: $earlyAdopter")

        // Show the cloud sync feature in case of early adopter or enable it generally.
        if (earlyAdopter or cloudPluginEnabled) {
            provideToolbarDrawing().toolbarCloudSync.visibility = View.VISIBLE
        } else {
            provideToolbarDrawing().toolbarCloudSync.visibility = View.GONE
        }

        provideToolbarDrawing().toolbarSettings.setOnClickListener {
            CalendarNavigator.toSettings(this)
        }

        templateBitmap = Bitmap.createBitmap(1404, 1872, Bitmap.Config.ARGB_8888)
        templateCanvas = Canvas(templateBitmap)

        navigatorBitmap = Bitmap.createBitmap(1404, 140, Bitmap.Config.ARGB_8888)
        navigatorCanvas = Canvas(navigatorBitmap)

        gestureListener = OnGestureListener()
        gestureDetector = GestureDetectorCompat(requireActivity(), gestureListener)
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        bitmap?.recycle()
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
                    bitmap!!.eraseColor(Color.TRANSPARENT)
                    canvas = Canvas(bitmap!!)

                    if (provideSurfaceView().holder == null) {
                        return
                    }

                    clearSurface()
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
    }

    /**
     * Clear the surface and the shadow canvas.
     */
    fun clearSurface() {
        val lockerCanvas = provideSurfaceView().holder.lockCanvas() ?: return
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

        // TODO: Without this the template image flickers... refresh race condition somewhere?!
        val trickPath = Path()
        trickPath.moveTo(0.0f, 0.0f)
        for (i in 1..1404) {
            trickPath.quadTo((i - 1) * 1.0f, 0.0f, i * 1.0f, 0.0f)
        }
        lockCanvas.drawPath(trickPath, paint)
        canvas.drawPath(trickPath, paint)

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

        provideSurfaceView().holder.unlockCanvasAndPost(lockCanvas)
    }

    /**
     * Normalize strokes from surface dimensions to unified.
     *
     * @param strokes the strokes
     * @return the normalized strokes
     */
    fun surfaceFrom(strokes: List<Stroke>): List<Stroke> {
        return normalizeStrokes(strokes, surfaceSize.width(), surfaceSize.height(), 1404, 1872)
    }

    /**
     * Normalize strokes to surface dimensions from unified.
     *
     * @param strokes the strokes
     * @return the normalized strokes
     */
    fun surfaceTo(strokes: List<Stroke>): List<Stroke> {
        return normalizeStrokes(strokes, 1404, 1872, surfaceSize.width(), surfaceSize.height())
    }

    /**
     * Normalize strokes.
     *
     * @param strokes the strokes
     * @param fromWidth from width
     * @param fromHeight from height
     * @param toWidth to width
     * @param toHeight to height
     * @return the normalized strokes
     */
    private fun normalizeStrokes(
        strokes: List<Stroke>, fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int
    ): List<Stroke> {
        val strokesCopy = Stroke.listDeepCopy(strokes)

        val widthRatio = 1.0f * toWidth / fromWidth
        val heightRatio = 1.0f * toHeight / fromHeight
        for (stroke in strokesCopy) {
            for (point in stroke.strokePoints) {
                point.x *= widthRatio
                point.y *= heightRatio
            }
        }

        return strokesCopy
    }

    /**
     * The input callback of stylus events.
     */
    fun callback(motionEvent: MotionEvent): Boolean {
        // TODO: check on other devices (stylus extra button)
        val ACTION_ERASE_DOWN = 211
        val ACTION_ERASE_UP = 212
        val ACTION_ERASE_MOVE = 213

        if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            val x = (10.0f * motionEvent.x).roundToInt() / 10.0f
            val y = (10.0f * motionEvent.y).roundToInt() / 10.0f
            val p = (10.0f * motionEvent.pressure).roundToInt() / 10.0f
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                onBeginDrawing(StrokePoint(x, y, p))
            } else if (motionEvent.action == MotionEvent.ACTION_MOVE) {
                onMoveDrawing(StrokePoint(x, y, p))
            } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                onEndDrawing(StrokePoint(x, y, p))
            } else if (motionEvent.action == ACTION_ERASE_DOWN) {
                onBeginDrawing(StrokePoint(x, y, p))
            } else if (motionEvent.action == ACTION_ERASE_MOVE) {
                onMoveDrawing(StrokePoint(x, y, p))
            } else if (motionEvent.action == ACTION_ERASE_UP) {
                onEndDrawing(StrokePoint(x, y, p), true)
            }

            return true
        }

        return false
    }

    private fun epsilon(touchPoint: StrokePoint, lastPoint: StrokePoint, epsilon: Float): Boolean {
        return epsilon(touchPoint.x, touchPoint.y, lastPoint.x, lastPoint.y, epsilon)
    }

    private fun epsilon(x1: Float, y1: Float, x2: Float, y2: Float, epsilon: Float): Boolean {
        val dx = abs(x1 - x2).toDouble()
        val dy = abs(y1 - y2).toDouble()
        val d = sqrt(dx * dx + dy * dy)
        return d <= epsilon
    }

    private fun onBeginDrawing(touchPoint: StrokePoint) {
        Timber.i("onBeginDrawing (${touchPoint.x}/${touchPoint.y})")
        lastPoint = touchPoint
        stylusPointList.add(touchPoint)
    }

    private fun onMoveDrawing(touchPoint: StrokePoint) {
        if (!epsilon(touchPoint, lastPoint!!, 3.0f)) {
            Timber.d("onMoveDrawing (${touchPoint.x}/${touchPoint.y} - ${touchPoint.p})")

            val sigma = paint.strokeWidth
            val rectLeft = (Math.min(lastPoint!!.x, touchPoint.x) - sigma).toInt()
            val rectRight = (Math.max(lastPoint!!.x, touchPoint.x) + sigma).toInt()
            val rectTop = (Math.min(lastPoint!!.y, touchPoint.y) - sigma).toInt()
            val rectBottom = (Math.max(lastPoint!!.y, touchPoint.y) + sigma).toInt()
            val rect = Rect(rectLeft, rectTop, rectRight, rectBottom)

            val lockCanvas = provideSurfaceView().holder.lockCanvas(rect)
            val path = Path()
            path.moveTo(stylusPointList[0].x, stylusPointList[0].y)
            stylusPointList.forEach {
                path.lineTo(it.x, it.y)
            }
            path.lineTo(touchPoint.x, touchPoint.y)
            lockCanvas?.drawPath(path, paint)
            provideSurfaceView().holder.unlockCanvasAndPost(lockCanvas)

            lastPoint = touchPoint
            stylusPointList.add(touchPoint)
        }
    }

    private fun onEndDrawing(touchPoint: StrokePoint, erasing: Boolean = false) {
        Timber.i("onEndDrawing (${touchPoint.x}/${touchPoint.y})")

        if (!penState || erasing) {
            val strokesToRemove: MutableSet<UUID> = mutableSetOf()
            for (ep in stylusPointList) {
                for (stroke in strokes) {
                    for (tp in stroke.strokePoints) {
                        if (epsilon(ep.x, ep.y, tp.x, tp.y, 10.0f)) {
                            strokesToRemove.add(stroke.strokeId)
                        }
                    }
                }
            }
            strokesToRemove.forEach { strokes.removeIf { stroke -> stroke.strokeId == it } }
            onStrokesDeleted(strokesToRemove.toList())

            applyStrokes(strokes, true)
            onStrokeChanged(strokes)
        } else {
            val stroke = Stroke(UUID.randomUUID(), stylusPointList.toList())
            strokes.add(stroke)
            strokesToAdd.add(stroke)
            applyStrokes(strokes, false)
            onStrokeChanged(strokes)

            onStrokesAdded(strokesToAdd.toList())
            strokesToAdd.clear()
        }

        lastPoint = null
        stylusPointList.clear()
    }
}
package online.toolboox.plugin.teamdrawer.ui

import android.Manifest
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.view.SurfaceHolder
import android.view.View
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPoint
import com.onyx.android.sdk.pen.data.TouchPointList
import kotlinx.coroutines.*
import online.toolboox.R
import online.toolboox.databinding.FragmentTeamdrawerPageBinding
import online.toolboox.plugin.teamdrawer.nw.domain.Stroke
import online.toolboox.plugin.teamdrawer.nw.domain.StrokePoint
import online.toolboox.ui.plugin.ScreenFragment
import timber.log.Timber
import java.time.Instant
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Team drawer main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class PageFragment @Inject constructor(
    private val presenter: PagePresenter
) : ScreenFragment() {

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_teamdrawer_page

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTeamdrawerPageBinding

    /**
     * The timer job.
     */
    private lateinit var timer: Job

    /**
     * The room ID.
     */
    private lateinit var roomId: UUID

    /**
     * The note ID.
     */
    private lateinit var noteId: UUID

    /**
     * The page ID.
     */
    private lateinit var pageId: UUID

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
     * Timestamp of the last stroke.
     */
    private var last: Long = 0

    /**
     * The list of strokes.
     */
    private var strokes: List<Stroke> = mutableListOf()

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTeamdrawerPageBinding.bind(view)

        if (parameters["roomId"] == null) {
            somethingHappened()
            return
        }
        roomId = UUID.fromString(parameters["roomId"])

        if (parameters["noteId"] == null) {
            somethingHappened()
            return
        }
        noteId = UUID.fromString(parameters["noteId"])

        if (parameters["pageId"] == null) {
            somethingHappened()
            return
        }
        pageId = UUID.fromString(parameters["pageId"])

        binding.buttonExport.setOnClickListener {
            val permissionGranted = checkPermissionGranted(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                getString(R.string.team_drawer_page_external_storage_write_permission_title),
                getString(R.string.team_drawer_page_external_storage_write_permission_message)
            )

            if (permissionGranted) {
                val title = "export-${Instant.now().epochSecond}"
                MediaStore.Images.Media.insertImage(
                    this@PageFragment.requireActivity().contentResolver,
                    bitmap,
                    title,
                    title
                )
                showMessage(getString(R.string.team_drawer_page_export_message).format(title), binding.root)
            }
        }

        binding.buttonErase.setOnClickListener {
            presenter.del(this, roomId, noteId, pageId)
        }

        touchHelper = TouchHelper.create(binding.surfaceView, callback)
        initializeSurface()
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.team_drawer_page_title))
        toolBar.toolbarPager.visibility = View.VISIBLE

        initializeSurface()
        touchHelper.setRawDrawingEnabled(true)

        presenter.last(this, roomId, noteId, pageId)
        timer = GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                presenter.last(this@PageFragment, roomId, noteId, pageId)
                delay(1000L)
            }
        }
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        toolBar.toolbarPager.visibility = View.GONE

        touchHelper.setRawDrawingEnabled(false)
        last = 0

        timer.cancel()
    }

    /**
     * OnDestroy hook.
     */
    override fun onDestroy() {
        super.onDestroy()

        touchHelper.closeRawDrawing()
        bitmap.recycle()
    }

    /**
     * Render the result of 'add' service call.
     *
     * @param stroke the saved stroke
     */
    fun addResult(stroke: Stroke) {
        presenter.last(this, roomId, noteId, pageId)
    }

    /**
     * Render the result of 'del' service call.
     *
     * @param strokes the strokes on the erased page
     */
    fun delResult(strokes: List<Stroke>) {
        this.strokes = strokes
        clearSurface()
    }

    /**
     * Render the result of 'del' service call.
     *
     * @param strokes the strokes on the erased page
     */
    fun delResult(stroke: Stroke) {
        presenter.list(this, roomId, noteId, pageId)
    }

    /**
     * Render the result of 'last' service call.
     *
     * @param last the last update timestamp
     */
    fun lastResult(last: Long) {
        if (this.last != last) {
            this.last = last
            presenter.list(this, roomId, noteId, pageId)
        }
    }

    /**
     * Render the result of 'list' service call.
     *
     * @param strokes the list of strokes
     */
    fun listResult(strokes: List<Stroke>) {
        this.strokes = strokes
        val lockCanvas = binding.surfaceView.holder.lockCanvas()

        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.WHITE
        val rect = Rect(0, 0, binding.surfaceView.width, binding.surfaceView.height)
        lockCanvas.drawRect(rect, fillPaint)

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
        binding.surfaceView.holder.unlockCanvasAndPost(lockCanvas)

        touchHelper.setRawDrawingEnabled(true)
    }

    /**
     * Show the progress bar.
     */
    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    /**
     * Hide the progress bar.
     */
    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }

    /**
     * Initialize the surface view of Onyx's drawing.
     */
    private fun initializeSurface() {
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 3.0f

        if (surfaceCallback == null) {
            surfaceCallback = object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    Timber.i("surfaceCreated")
                    val limit = Rect()
                    binding.surfaceView.getLocalVisibleRect(limit)
                    bitmap = Bitmap.createBitmap(
                        binding.surfaceView.width,
                        binding.surfaceView.height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.WHITE)
                    canvas = Canvas(bitmap)

                    if (binding.surfaceView.holder == null) {
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

        binding.surfaceView.holder.addCallback(surfaceCallback)
        binding.surfaceView.viewTreeObserver.addOnGlobalLayoutListener {
            Timber.i("addOnGlobalLayoutListener")
            touchHelper.setRawDrawingEnabled(true)
        }
    }

    /**
     * Clear the surface and the shadow canvas.
     */
    private fun clearSurface() {
        val lockerCanvas = binding.surfaceView.holder.lockCanvas() ?: return
        EpdController.enablePost(binding.surfaceView, 1)
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        val rect = Rect(0, 0, binding.surfaceView.width, binding.surfaceView.height)
        lockerCanvas.drawRect(rect, paint)
        binding.surfaceView.holder.unlockCanvasAndPost(lockerCanvas)

        canvas.drawRect(rect, paint)
    }

    /**
     * The raw input callback of Onyx's pen library.
     */
    private val callback: RawInputCallback = object : RawInputCallback() {

        var lastPoint: TouchPoint? = null

        private fun epsilon(touchPoint: TouchPoint, lastPoint: TouchPoint, epsilon: Float = 5.0f): Boolean {
            return epsilon(touchPoint.x, touchPoint.y, lastPoint.x, lastPoint.y, epsilon)
        }

        private fun epsilon(x1: Float, y1: Float, x2: Float, y2: Float, epsilon: Float = 5.0f): Boolean {
            val dx = abs(x1 - x2).toDouble()
            val dy = abs(y1 - y2).toDouble()
            val d = sqrt(dx * dx + dy * dy)
            return d > epsilon
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
                if (epsilon(tp, prevPoint)) {
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
            presenter.add(this@PageFragment, roomId, noteId, pageId, stroke)
        }

        override fun onBeginRawErasing(b: Boolean, touchPoint: TouchPoint) {
            Timber.i("onBeginRawErasing (${touchPoint.x} - ${touchPoint.y})")
        }

        override fun onEndRawErasing(b: Boolean, touchPoint: TouchPoint) {
            Timber.i("onEndRawErasing (${touchPoint.x} - ${touchPoint.y})")
        }

        override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint) {
            Timber.i("onRawErasingTouchPointMoveReceived (${touchPoint.x} - ${touchPoint.y})")
        }

        override fun onRawErasingTouchPointListReceived(touchPointList: TouchPointList) {
            Timber.i("onRawErasingTouchPointListReceived (${touchPointList.size()})")

            val eraserPoints: MutableList<TouchPoint> = mutableListOf()
            var prevPoint: TouchPoint = touchPointList[0]
            eraserPoints.add(prevPoint)
            for (tp in touchPointList) {
                if (epsilon(tp, prevPoint)) {
                    prevPoint = tp
                    eraserPoints.add(prevPoint)
                }
            }

            Timber.i("onRawErasingTouchPointListReceived ($eraserPoints)")
            for (ep in eraserPoints) {
                for (stroke in strokes) {
                    for (tp in stroke.strokePoints) {
                        if (!epsilon(ep.x, ep.y, tp.x, tp.y, 10.0f)) {
                            presenter.del(this@PageFragment, roomId, noteId, pageId, stroke.strokeId)
                            return
                        }
                    }
                }
            }
        }
    }
}

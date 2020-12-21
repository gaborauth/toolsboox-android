package online.toolboox.main.ui

import android.graphics.*
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.data.QueryArgs
import com.onyx.android.sdk.data.provider.RemoteDataProvider
import com.onyx.android.sdk.pen.BrushRender
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPoint
import com.onyx.android.sdk.pen.data.TouchPointList
import com.onyx.android.sdk.scribble.provider.RemoteNoteProvider
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.config.GeneratedDatabaseHolder
import com.raizlabs.android.dbflow.config.ShapeGeneratedDatabaseHolder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import online.toolboox.BuildConfig
import online.toolboox.R
import online.toolboox.main.di.MainSharedPreferencesModule
import online.toolboox.main.nw.domain.StrokePoint
import online.toolboox.ui.BaseActivity
import online.toolboox.utils.ReleaseTree
import retrofit2.Response
import timber.log.Timber
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A dashboard screen that offers the main menu.
 *
 * @author <a href="mailto:auth.gabor@gmail.com">Gábor AUTH</a>
 */
class MainActivity : BaseActivity<MainPresenter>(), MainView {

    /**
     * The Firebase analytics.
     */
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The back URL from intent extra.
     */
    private var backUrl: String? = null

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
     * The timer job.
     */
    private lateinit var timer: Job

    /**
     * Timestamp of the last stroke.
     */
    private var last: Long = 0

    /**
     * OnCreate hook.
     *
     * @param savedInstanceState the saved state of the instance
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)

        title = getString(R.string.drawer_title).format(getString(R.string.app_name), getString(R.string.main_title))

        backUrl = intent.getStringExtra("backUrl")

        val preferences = MainSharedPreferencesModule.provideSharedPreferences(this)
        preferences.edit().putLong("lastTimestamp", Date().time).apply()

        FlowManager.init(
            FlowConfig
                .builder(this)
                .addDatabaseHolder(GeneratedDatabaseHolder::class.java)
                .addDatabaseHolder(ShapeGeneratedDatabaseHolder::class.java)
                .build()
        )

        val noteProvider = RemoteNoteProvider()
        val dataProvider = RemoteDataProvider()
        val notes = noteProvider.loadAllNoteList()

        notes.stream().forEach {
            Timber.i(
                "uniqueId=%s, parentUniqueId=%s, isLibrary=%s, title=%s",
                it.uniqueId, it.parentUniqueId, it.isLibrary, it.title
            )
        }

        val libraries = dataProvider.loadAllLibrary(null, QueryArgs())
        Timber.i("Libraries: %d", libraries.size)
        libraries.stream().forEach {
            Timber.i(
                "storageId=%s, parentUniqueId=%s, name=%s",
                it.storageId, it.parentUniqueId, it.name
            )
        }

        touchHelper = TouchHelper.create(surfaceView, callback)
        initializeSurface()
    }

    override fun onResume() {
        initializeSurface()
        touchHelper.setRawDrawingEnabled(true)

        timer = GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                presenter.last()
                delay(5000L)
            }
        }

        super.onResume()
    }

    override fun onPause() {
        touchHelper.setRawDrawingEnabled(false)

        timer.cancel()
        last = 0

        super.onPause()
    }

    override fun onDestroy() {
        touchHelper.closeRawDrawing()
        bitmap.recycle()

        super.onDestroy()
    }

    /**
     * Displays an error in the view.
     *
     * @param t the optional throwable
     * @param errorResId the resource id of the error
     */
    override fun showError(t: Throwable?, @StringRes errorResId: Int) {
        t?.let { Timber.e(it, getString(errorResId)) }
    }

    /**
     * Displays an error in the view.
     *
     * @param messageResId the resource id of the error
     */
    override fun showMessage(@StringRes messageResId: Int) {
        Snackbar.make(mainToolbar, messageResId, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Show progress and hide login form.
     */
    override fun showLoading() {
        mainProgress.visibility = View.VISIBLE
    }

    /**
     * Hide progress and show login form.
     */
    override fun hideLoading() {
        mainProgress.visibility = View.INVISIBLE
    }

    override fun addResult(response: Response<List<StrokePoint>>) {
        Timber.i("$response")
    }

    override fun lastResult(response: Response<Long>) {
        Timber.i("$response")
        val responseLast: Long = response.body()!!
        if (last != responseLast) {
            last = responseLast
            presenter.list()
        }
    }

    override fun listResult(response: Response<List<List<StrokePoint>>>) {
        Timber.i("$response")

        val lockCanvas = surfaceView.holder.lockCanvas()

        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.WHITE
        val rect = Rect(0, 0, surfaceView.width, surfaceView.height)
        lockCanvas.drawRect(rect, fillPaint)

        val strokes: List<List<StrokePoint>> = response.body()!!
        for (stroke in strokes) {
            if (stroke.isNotEmpty()) {
                val path = Path()
                val prePoint = PointF(stroke[0].x, stroke[0].y)
                path.moveTo(prePoint.x, prePoint.y)
                for (point in stroke) {
                    path.quadTo(prePoint.x, prePoint.y, point.x, point.y)
                    prePoint.x = point.x
                    prePoint.y = point.y
                }

                lockCanvas.drawPath(path, paint)
            }
        }

        surfaceView.holder.unlockCanvasAndPost(lockCanvas)

        touchHelper.setRawDrawingEnabled(true)
    }

    /**
     * Instantiate the presenter.
     */
    override fun presenter(): MainPresenter {
        return MainPresenter(this)
    }

    private fun initializeSurface() {
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 3.0f

        if (surfaceCallback == null) {
            surfaceCallback = object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    Timber.w("surfaceCreated")
                    val limit = Rect()
                    surfaceView!!.getLocalVisibleRect(limit)
                    bitmap = Bitmap.createBitmap(
                        surfaceView!!.width,
                        surfaceView!!.height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.WHITE)
                    canvas = Canvas(bitmap)

                    if (surfaceView.holder == null) {
                        return
                    }
                    val canvas = surfaceView.holder.lockCanvas() ?: return
                    EpdController.enablePost(surfaceView, 1)
                    val paint = Paint()
                    paint.style = Paint.Style.FILL
                    paint.color = Color.WHITE
                    val rect = Rect(0, 0, surfaceView.width, surfaceView.height)
                    canvas.drawRect(rect, paint)

                    canvas.drawBitmap(bitmap, 0f, 0f, paint)
                    surfaceView.holder.unlockCanvasAndPost(canvas)
                    touchHelper.setLimitRect(limit, ArrayList())
                        .setStrokeWidth(3.0f)
                        .openRawDrawing()
                    touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_BRUSH)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    Timber.w("surfaceChanged")
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    Timber.w("surfaceDestroyed")
                    holder.removeCallback(surfaceCallback)
                    surfaceCallback = null
                }
            }
        }

        surfaceView!!.holder.addCallback(surfaceCallback)

        surfaceView!!.viewTreeObserver.addOnGlobalLayoutListener {
            Timber.w("addOnGlobalLayoutListener")
            touchHelper.setRawDrawingEnabled(true)
        }
    }
    private val callback: RawInputCallback = object : RawInputCallback() {

        var lastPoint: TouchPoint? = null

        private fun epsilon(touchPoint: TouchPoint): Boolean {
            val dx = abs(touchPoint.x - lastPoint!!.x).toDouble()
            val dy = abs(touchPoint.y - lastPoint!!.y).toDouble()
            val d = sqrt(dx * dx + dy * dy)
            return d > 5.0
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
            if (epsilon(touchPoint)) {
                lastPoint = touchPoint
                Timber.i("onRawDrawingTouchPointMoveReceived (${touchPoint.x}/${touchPoint.y} - ${touchPoint.pressure})")
            }
        }

        override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList) {
            Timber.i("onRawDrawingTouchPointListReceived (${touchPointList.size()})")

            val stroke: MutableList<StrokePoint> = mutableListOf()
            for (tp in touchPointList) {
                stroke.add(StrokePoint(tp.x, tp.y, tp.pressure))
            }
            presenter.add(stroke)

            val maxPressure = EpdController.getMaxTouchPressure()
            BrushRender.drawStroke(canvas, paint, touchPointList.points,1.0f, 3.0f, maxPressure, false)
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
        }
    }
}

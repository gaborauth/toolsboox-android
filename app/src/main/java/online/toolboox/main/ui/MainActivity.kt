package online.toolboox.main.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.data.QueryArgs
import com.onyx.android.sdk.data.provider.RemoteDataProvider
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPoint
import com.onyx.android.sdk.pen.data.TouchPointList
import com.onyx.android.sdk.scribble.provider.RemoteNoteProvider
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.config.GeneratedDatabaseHolder
import com.raizlabs.android.dbflow.config.ShapeGeneratedDatabaseHolder
import kotlinx.coroutines.*
import online.toolboox.BuildConfig
import online.toolboox.R
import online.toolboox.databinding.ActivityMainBinding
import online.toolboox.main.di.MainSharedPreferencesModule
import online.toolboox.plugin.teamdrawer.nw.domain.Stroke
import online.toolboox.plugin.teamdrawer.nw.domain.StrokePoint
import online.toolboox.ui.BaseActivity
import online.toolboox.utils.ReleaseTree
import retrofit2.Response
import timber.log.Timber
import java.time.Instant
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A dashboard screen that offers the main menu.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class MainActivity : BaseActivity<MainPresenter>(), MainView {

    /**
     * The Firebase analytics.
     */
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The view binding.
     */
    private lateinit var binding: ActivityMainBinding

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        setSupportActionBar(binding.mainToolbar)

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

        binding.buttonExport.setOnClickListener {
            Timber.i("Start of the export process")

            val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if (permission != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                    showExplanation(
                        "Permission Needed",
                        "Rationale",
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
                    )
                } else {
                    requestPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
                    )
                }
                return@setOnClickListener
            }

            val title = "export-${Instant.now().epochSecond}"
            MediaStore.Images.Media.insertImage(contentResolver, bitmap, title, "Export")
            Toast.makeText(this, "Exported as $title", Toast.LENGTH_LONG).show()
            Timber.i("End of the export process")
        }

        binding.buttonErase.setOnClickListener {
            presenter.del()
        }

        touchHelper = TouchHelper.create(binding.surfaceView, callback)
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
        Snackbar.make(binding.mainToolbar, messageResId, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Show progress and hide login form.
     */
    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    /**
     * Hide progress and show login form.
     */
    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }

    override fun addResult(response: Response<Stroke>) {
        Timber.i("$response")
    }

    override fun delResult(response: Response<List<Stroke>>) {
        clearSurface()
    }

    override fun lastResult(response: Response<Long>) {
        Timber.i("$response")
        val responseLast: Long = response.body()!!
        if (last != responseLast) {
            last = responseLast
            presenter.list()
        }
    }

    override fun listResult(response: Response<List<Stroke>>) {
        Timber.i("$response")

        val lockCanvas = binding.surfaceView.holder.lockCanvas()

        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.WHITE
        val rect = Rect(0, 0, binding.surfaceView.width, binding.surfaceView.height)
        lockCanvas.drawRect(rect, fillPaint)

        val strokes: List<Stroke> = response.body()!!
        for (stroke in strokes) {
            val points = stroke.strokePoints
            if (points.isNotEmpty()) {
                val path = Path()
                val prePoint = PointF(points[0].x, points[0].y)
                path.moveTo(prePoint.x, prePoint.y)
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
                    Timber.w("surfaceChanged")
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    Timber.w("surfaceDestroyed")
                    holder.removeCallback(surfaceCallback)
                    surfaceCallback = null
                }
            }
        }

        binding.surfaceView.holder.addCallback(surfaceCallback)
        binding.surfaceView.viewTreeObserver.addOnGlobalLayoutListener {
            Timber.w("addOnGlobalLayoutListener")
            touchHelper.setRawDrawingEnabled(true)
        }
    }

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

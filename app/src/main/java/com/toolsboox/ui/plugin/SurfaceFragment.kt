package com.toolsboox.ui.plugin

import android.Manifest
import android.content.SharedPreferences
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import com.onyx.android.sdk.utils.DeviceFeatureUtil
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.toolsboox.R
import com.toolsboox.da.Stroke
import com.toolsboox.da.StrokePoint
import com.toolsboox.da.TextElement
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.ot.StrokeClipboard
import com.toolsboox.plugin.calendar.CalendarNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.Instant
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * SurfaceView fragment of Boox pen and native stylus supports.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
abstract class SurfaceFragment : ScreenFragment() {

    companion object {
        const val CANVAS_WIDTH = 1404
        const val CANVAS_HEIGHT = 1872
        const val MIN_ZOOM = 1.0f
        const val MAX_ZOOM = 4.0f

        /**
         * Touch drawing state.
         */
        private var touchDrawingState: Boolean = false

        /**
         * When true, a single finger can pan / swipe-navigate (convenient for browsing).
         * When false (default), gestures require two fingers — palm rejection while
         * writing with a stylus.
         */
        @JvmStatic protected var singleFingerGesturesEnabled: Boolean = false

        // List of unrecognized actions.
        private val actions = mutableListOf<String>()

        // List of unrecognized buttons.
        private val buttons = mutableListOf<String>()
    }

    /**
     * The Firebase analytics.
     */
    @Inject
    lateinit var privateFirebaseAnalytics: FirebaseAnalytics

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
     * The stroke clipboard singleton.
     */
    @Inject
    lateinit var strokeClipboard: StrokeClipboard

    // --- Lasso selection state ---
    /** True while the lasso tool is active (drawing the selection polygon). */
    private var selectionMode = false

    /** True after a lasso is completed and strokes are selected, waiting for copy/paste. */
    private var hasSelection = false

    /** Points of the lasso polygon being drawn. */
    private var selectionPoints = mutableListOf<PointF>()

    /** Strokes that fell inside the lasso polygon. */
    private var selectedStrokes = mutableListOf<Stroke>()

    /** Bounding box (canvas-space) of the current selection, set when the lasso closes. */
    private var selBox: RectF? = null

    /** Which transform is currently being driven by the stylus (NONE = idle). */
    private enum class SelectionDrag { NONE, HANDLE_TL, HANDLE_TR, HANDLE_BL, HANDLE_BR, MOVE }
    private var selectionDrag = SelectionDrag.NONE

    /** Anchor (opposite corner) for the active scale drag and the bbox snapshot at drag start. */
    private var scaleAnchorX = 0f
    private var scaleAnchorY = 0f
    private var scaleOrigBox: RectF? = null

    /** Pen-down canvas coords when a MOVE drag begins. */
    private var moveStartX = 0f
    private var moveStartY = 0f

    /**
     * Snapshot of the original (x,y) for every point of every selected stroke at the
     * moment a scale drag starts. Each ACTION_MOVE re-derives the stroke positions from
     * this snapshot so the math stays linear and doesn't compound across ticks.
     */
    private var scaleOrigPoints: Map<UUID, List<Pair<Float, Float>>> = emptyMap()

    /** True while in paste-placement mode (next tap places the clipboard contents). */
    private var pasteMode = false

    // --- Undo/redo history ---
    private val undoStack = mutableListOf<List<Stroke>>()
    private val redoStack = mutableListOf<List<Stroke>>()

    // --- Zoom and pan state ---
    protected var twoFingerGesture = false
    private var zoomScale = 1.0f
    private var panX = 0.0f
    private var panY = 0.0f
    private var baseScale = 1.0f
    private val viewMatrix = Matrix()
    private val inverseViewMatrix = Matrix()
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var doubleTapDetector: GestureDetector? = null
    private var lastFingerX = 0f
    private var lastFingerY = 0f
    private var isPanning = false

    // --- Text tool state ---
    /** True while in text placement mode (next tap opens text input dialog). */
    private var textMode = false

    /** The list of text elements on the current page. */
    private var textElements: MutableList<TextElement> = mutableListOf()

    /** Paint used for rendering text elements on canvas. */
    private var textPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.BLACK
        textSize = 24f
    }

    /**
     * The paint in the bitmap.
     */
    private var paint = Paint()

    /**
     * Paint for the lasso polygon line.
     */
    private var lassoPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.DKGRAY
        strokeWidth = 2.0f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    /**
     * Paint for highlighting selected strokes.
     */
    private var selectionHighlightPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.DKGRAY
        strokeWidth = 5.0f
    }

    /**
     * TouchHelper of the Onyx's pen.
     */
    private var touchHelper: TouchHelper? = null

    /**
     * Viwoods AiPaper fast-ink backend. Non-null only on Viwoods hardware (where the Onyx
     * TouchHelper is inert); puts the panel into the FAST e-ink waveform so our software
     * stroke rendering refreshes quickly. Null on Boox.
     */
    private var viwoodsInk: com.toolsboox.ot.ViwoodsFastInk? = null

    /**
     * Coalescing guard for the Viwoods software live-ink preview. The digitizer fires many
     * ACTION_MOVE events per second (sometimes several per frame); each one would otherwise
     * trigger a full-stroke lockCanvas + EPD FAST refresh. Those serialize on the panel and
     * build a backlog, so the ink trails the pen. We capture every point but post at most
     * once per display frame via [viwoodsLivePostRunnable], which drains the backlog and
     * keeps the live stroke tight to the nib. Viwoods software path only.
     */
    private var viwoodsLivePostScheduled = false
    private val viwoodsLivePostRunnable = Runnable {
        viwoodsLivePostScheduled = false
        if (penState && stylusPointList.isNotEmpty()) renderLivePreviewSoftware()
    }

    /**
     * The gesture detector
     */
    protected lateinit var gestureDetector: GestureDetectorCompat

    /**
     * The gesture listener
     */
    protected lateinit var gestureListener: OnGestureListener

    /**
     * The bitmap of the canvas (for export).
     */
    private var bitmap: Bitmap? = null

    /**
     * The canvas of the surface view (for export).
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

    // First point timestamp.
    private var firstPointTimestamp = 0L

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

    // In case of eraser, is it procrastinator?
    private var procrastinator: Boolean = false

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
     * Strokes procrastinated callback.
     *
     * @param strokes the strokes to procrastinate
     */
    open fun onStrokesProcrastinated(strokes: List<Stroke>) {}

    /**
     * Text elements changed callback.
     *
     * @param textElements the current text elements
     */
    open fun onTextElementsChanged(textElements: MutableList<TextElement>) {}

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        // Immersive fullscreen: hide system bars + action bar, draw into the display
        // cutout area, so the canvas truly fills the whole screen. System bars can
        // still be revealed by swiping from the edge for back/home access.
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.hide()
        requireActivity().window.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            WindowInsetsControllerCompat(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        // The activity's DrawerLayout has fitsSystemWindows="true" which reserves
        // space for the now-hidden bars (visible as black strips). Turn it off here
        // and restore in onPause so the settings/about screens still inset correctly.
        requireActivity().findViewById<View>(R.id.drawerLayout)?.let { drawer ->
            drawer.fitsSystemWindows = false
            drawer.setPadding(0, 0, 0, 0)
        }

        initializeSurface()
        touchHelper?.setRawDrawingEnabled(true)
        touchHelper?.isRawDrawingRenderEnabled = true
        // Viwoods FAST waveform is (re)activated from surfaceChanged once dimensions are known.

        penState = true
        selectionMode = false
        hasSelection = false
        pasteMode = false
        textMode = false
        selectionPoints.clear()
        selectedStrokes.clear()
        provideToolbarDrawing().toolbarPen.background.setTint(Color.GRAY)
        provideToolbarDrawing().toolbarEraser.background.setTint(Color.WHITE)
        provideToolbarDrawing().toolbarProcrastinator.background.setTint(Color.WHITE)
        provideToolbarDrawing().toolbarLasso.background.setTint(Color.WHITE)
        provideToolbarDrawing().toolbarCopy.background.setTint(Color.WHITE)
        provideToolbarDrawing().toolbarPaste.background.setTint(Color.WHITE)
        provideToolbarDrawing().toolbarText.background.setTint(Color.WHITE)

        // Restore the single-finger-gestures preference, then sync the toolbar icon.
        singleFingerGesturesEnabled = sharedPreferences.getBoolean("singleFingerGesturesEnabled", false)
        if (singleFingerGesturesEnabled)
            provideToolbarDrawing().toolbarHandTouch.setImageResource(R.drawable.ic_toolbar_hand_draw)
        else
            provideToolbarDrawing().toolbarHandTouch.setImageResource(R.drawable.ic_toolbar_hand_touch)

        provideToolbarDrawing().toolbarPen.setOnClickListener {
            if (penState && !procrastinator) {
                showPenSettingsDialog()
            } else {
                penState = true
                procrastinator = false
                textMode = false
                exitSelectionMode()
                provideToolbarDrawing().toolbarPen.background.setTint(Color.GRAY)
                provideToolbarDrawing().toolbarEraser.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarProcrastinator.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarLasso.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarText.background.setTint(Color.WHITE)
            }
        }

        provideToolbarDrawing().toolbarEraser.setOnClickListener {
            penState = false
            procrastinator = false
            textMode = false
            exitSelectionMode()
            provideToolbarDrawing().toolbarPen.background.setTint(Color.WHITE)
            provideToolbarDrawing().toolbarEraser.background.setTint(Color.GRAY)
            provideToolbarDrawing().toolbarProcrastinator.background.setTint(Color.WHITE)
            provideToolbarDrawing().toolbarLasso.background.setTint(Color.WHITE)
            provideToolbarDrawing().toolbarText.background.setTint(Color.WHITE)
        }

        provideToolbarDrawing().toolbarProcrastinator.visibility = View.GONE
        provideToolbarDrawing().toolbarProcrastinator.setOnClickListener {
            penState = false
            procrastinator = true
            textMode = false
            exitSelectionMode()
            provideToolbarDrawing().toolbarPen.background.setTint(Color.WHITE)
            provideToolbarDrawing().toolbarEraser.background.setTint(Color.WHITE)
            provideToolbarDrawing().toolbarProcrastinator.background.setTint(Color.GRAY)
            provideToolbarDrawing().toolbarLasso.background.setTint(Color.WHITE)
            provideToolbarDrawing().toolbarText.background.setTint(Color.WHITE)
        }

        // --- Lasso selection button ---
        provideToolbarDrawing().toolbarLasso.setOnClickListener {
            if (selectionMode || hasSelection) {
                // Toggle off: exit selection mode
                exitSelectionMode()
                penState = true
                provideToolbarDrawing().toolbarPen.background.setTint(Color.GRAY)
                provideToolbarDrawing().toolbarLasso.background.setTint(Color.WHITE)
                applyStrokes(strokes, true)
            } else {
                // Enter lasso mode
                selectionMode = true
                pasteMode = false
                textMode = false
                penState = true // keep pen state so stylus draws the lasso
                procrastinator = false
                provideToolbarDrawing().toolbarPen.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarEraser.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarProcrastinator.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarLasso.background.setTint(Color.GRAY)
                provideToolbarDrawing().toolbarText.background.setTint(Color.WHITE)
            }
        }

        // --- Copy button ---
        provideToolbarDrawing().toolbarCopy.setOnClickListener {
            if (selectedStrokes.isNotEmpty()) {
                strokeClipboard.copy(selectedStrokes.toList())
                showMessage(R.string.calendar_drawing_toolbar_copied, provideSurfaceView())
            } else {
                showMessage(R.string.calendar_drawing_toolbar_nothing_selected, provideSurfaceView())
            }
        }

        // --- Paste button ---
        provideToolbarDrawing().toolbarPaste.setOnClickListener {
            if (!strokeClipboard.hasContent) {
                showMessage(R.string.calendar_drawing_toolbar_clipboard_empty, provideSurfaceView())
                return@setOnClickListener
            }
            pasteMode = true
            selectionMode = false
            textMode = false
            hasSelection = false
            selectedStrokes.clear()
            selectionPoints.clear()
            penState = true
            procrastinator = false
            provideToolbarDrawing().toolbarPen.background.setTint(Color.WHITE)
            provideToolbarDrawing().toolbarEraser.background.setTint(Color.WHITE)
            provideToolbarDrawing().toolbarProcrastinator.background.setTint(Color.WHITE)
            provideToolbarDrawing().toolbarLasso.background.setTint(Color.WHITE)
            provideToolbarDrawing().toolbarText.background.setTint(Color.WHITE)
            provideToolbarDrawing().toolbarPaste.background.setTint(Color.GRAY)
            showMessage(R.string.calendar_drawing_toolbar_paste, provideSurfaceView())
        }

        // --- Text tool button ---
        provideToolbarDrawing().toolbarText.setOnClickListener {
            if (textMode) {
                // Toggle off: exit text mode, return to pen
                textMode = false
                penState = true
                provideToolbarDrawing().toolbarText.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarPen.background.setTint(Color.GRAY)
            } else {
                // Enter text placement mode
                textMode = true
                pasteMode = false
                selectionMode = false
                hasSelection = false
                penState = true
                procrastinator = false
                selectedStrokes.clear()
                selectionPoints.clear()
                provideToolbarDrawing().toolbarPen.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarEraser.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarProcrastinator.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarLasso.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarPaste.background.setTint(Color.WHITE)
                provideToolbarDrawing().toolbarText.background.setTint(Color.GRAY)
            }
        }

        provideToolbarDrawing().toolbarHandTouch.setOnClickListener {
            singleFingerGesturesEnabled = !singleFingerGesturesEnabled
            sharedPreferences.edit().putBoolean("singleFingerGesturesEnabled", singleFingerGesturesEnabled).apply()
            if (singleFingerGesturesEnabled)
                provideToolbarDrawing().toolbarHandTouch.setImageResource(R.drawable.ic_toolbar_hand_draw)
            else
                provideToolbarDrawing().toolbarHandTouch.setImageResource(R.drawable.ic_toolbar_hand_touch)
            val msg = if (singleFingerGesturesEnabled) "One-finger gestures on" else "Two-finger gestures required"
            showMessage(msg, provideSurfaceView())
        }

        provideToolbarDrawing().toolbarUndo.setOnClickListener {
            if (undoStack.isNotEmpty()) {
                redoStack.add(Stroke.listDeepCopy(strokes))
                strokes = undoStack.removeAt(undoStack.size - 1).toMutableList()
                applyStrokes(strokes, true)
                onStrokeChanged(strokes)
            }
        }

        provideToolbarDrawing().toolbarRedo.setOnClickListener {
            if (redoStack.isNotEmpty()) {
                undoStack.add(Stroke.listDeepCopy(strokes))
                strokes = redoStack.removeAt(redoStack.size - 1).toMutableList()
                applyStrokes(strokes, true)
                onStrokeChanged(strokes)
            }
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

                    // Also clear text elements
                    textElements.clear()
                    onTextElementsChanged(textElements)

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

        provideToolbarDrawing().toolbarRotate.setOnClickListener {
            val activity = requireActivity()
            val current = activity.requestedOrientation

            // Build the cycle order from the user's preference (bitmask).
            // SCREEN_ORIENTATION_LANDSCAPE == landscape CCW (top tilts left in Android terms)
            // SCREEN_ORIENTATION_REVERSE_LANDSCAPE == landscape CW (top tilts right)
            val mask = sharedPreferences.getInt("rotationOrientationMask", 0b1111)
            val cycle = mutableListOf<Int>()
            if (mask and 0b0001 != 0) cycle.add(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            if (mask and 0b0010 != 0) cycle.add(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
            if (mask and 0b0100 != 0) cycle.add(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
            if (mask and 0b1000 != 0) cycle.add(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            if (cycle.isEmpty()) cycle.add(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

            val idx = cycle.indexOf(current).takeIf { it >= 0 } ?: -1
            activity.requestedOrientation = cycle[(idx + 1) % cycle.size]
        }

        // Hide the cloud sync feature in case of regular users or enable it generally.
        val androidId = sharedPreferences.getString("androidId", "")
        val earlyAdopterDeviceIdsJson = sharedPreferences.getString("earlyAdopterDeviceIds", "[]")

        val earlyAdopterDeviceIdsType = Types.newParameterizedType(MutableList::class.java, String::class.java)
        val jsonAdapter = moshi.adapter<List<String>>(earlyAdopterDeviceIdsType)
        val earlyAdopterDeviceIds = jsonAdapter.fromJson(earlyAdopterDeviceIdsJson!!)

        val googleDrivePluginEnabled = sharedPreferences.getString("googleDrivePluginEnabled", "false").toBoolean()
        Timber.i("Google Drive plugin enabled: $googleDrivePluginEnabled")

        val earlyAdopter = earlyAdopterDeviceIds?.contains(androidId) ?: false
        Timber.i("Early adopter: $earlyAdopter")

        provideToolbarDrawing().toolbarCloudSync.visibility = View.VISIBLE

        provideToolbarDrawing().toolbarSettings.setOnClickListener {
            CalendarNavigator.toSettings(this)
        }

        val toolbarCollapsed = sharedPreferences.getBoolean("toolbarCollapsed", false)
        applyToolbarCollapsedState(toolbarCollapsed)

        val toggleAction = View.OnClickListener {
            val collapsed = !sharedPreferences.getBoolean("toolbarCollapsed", false)
            sharedPreferences.edit().putBoolean("toolbarCollapsed", collapsed).apply()
            applyToolbarCollapsedState(collapsed)
        }
        provideToolbarDrawing().toolbarToggle.setOnClickListener(toggleAction)
        provideToolbarDrawing().root.setOnClickListener(toggleAction)

        templateBitmap = Bitmap.createBitmap(1404, 1872, Bitmap.Config.ARGB_8888)
        templateCanvas = Canvas(templateBitmap)

        navigatorBitmap = Bitmap.createBitmap(1404, 140, Bitmap.Config.ARGB_8888)
        navigatorCanvas = Canvas(navigatorBitmap)

        gestureListener = OnGestureListener()
        gestureDetector = GestureDetectorCompat(requireActivity(), gestureListener)

        // Opening the planner is the one moment the device is reliably awake and online,
        // so push any strokes stranded on disk from a prior session. Boox battery
        // management suppresses the 60-min periodic worker while the tablet sleeps for
        // days, and the onPause one-shot can be lost if Doze fires before it runs — so
        // without this trigger the cloud (and the downstream OCR pipeline) can sit stale
        // until the next charge. syncNow() carries the full current state, so a redundant
        // push here is harmless.
        try {
            com.toolsboox.plugin.calendar.nw.UltrabridgeSyncWorker.syncNow(requireContext())
        } catch (_: Exception) {}
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        // Restore system bars and action bar so other screens (settings, etc.) behave normally.
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.show()
        requireActivity().window.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
        requireActivity().findViewById<View>(R.id.drawerLayout)?.fitsSystemWindows = true

        touchHelper?.setRawDrawingEnabled(false)
        touchHelper?.isRawDrawingRenderEnabled = false

        touchHelper?.closeRawDrawing()
        bitmap?.recycle()

        // Tear down Viwoods AutoDraw and return the panel to reading mode. No-op on Boox.
        viwoodsInk?.disable()

        // Release the screen-on flag so the device can enter Doze when the planner is
        // backgrounded. Without this the SoC stays awake — on e-ink the frozen frame
        // looks "off" but Wi-Fi/CPU never sleep, draining the battery overnight.
        try {
            provideSurfaceView().keepScreenOn = false
        } catch (_: Exception) {}

        try {
            com.toolsboox.plugin.calendar.nw.UltrabridgeSyncWorker.syncNow(requireContext())
        } catch (_: Exception) {}
    }

    private fun applyToolbarCollapsedState(collapsed: Boolean) {
        val toolbar = provideToolbarDrawing()
        val group = toolbar.toolbarButtonGroup
        val density = resources.displayMetrics.density
        if (collapsed) {
            group.visibility = View.GONE
            toolbar.toolbarToggle.visibility = View.GONE
            toolbar.root.setBackgroundColor(Color.LTGRAY)
            toolbar.root.layoutParams?.let { lp ->
                lp.width = (12 * density).toInt()
                toolbar.root.layoutParams = lp
            }
        } else {
            group.visibility = View.VISIBLE
            toolbar.toolbarToggle.visibility = View.VISIBLE
            toolbar.root.setBackgroundColor(Color.TRANSPARENT)
            // Decide column count from the available height.
            // Single column needs ~17 buttons * 40dp + bottom toggle ≈ 720dp.
            // If the screen is shorter than that (e.g. Palma 2 Pro in landscape),
            // widen the toolbar to 80dp and split buttons into two columns.
            val needsTwoColumns = resources.configuration.screenHeightDp < 720
            // In two-column mode use 100dp so there's ~20dp of breathing room
            // between the left (40dp) and right (40dp) columns.
            val toolbarWidthDp = if (needsTwoColumns) 100 else 40
            toolbar.root.layoutParams?.let { lp ->
                lp.width = (toolbarWidthDp * density).toInt()
                toolbar.root.layoutParams = lp
            }
            applyToolbarTwoColumnLayout(needsTwoColumns)
        }
    }

    /**
     * Reposition the toolbar buttons into one or two vertical columns based on
     * available height. In single-column mode buttons stay as the XML defines them
     * (start+end anchored, vertically chained). In two-column mode the right-column
     * buttons are re-anchored to flow top-down from the toolbar top.
     */
    private fun applyToolbarTwoColumnLayout(twoColumns: Boolean) {
        if (!twoColumns) return  // rely on fresh XML on activity recreation (rotation)

        val toolbar = provideToolbarDrawing()
        val unset = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
        val parentId = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        // Right column flows top-down from parent top.
        val rightChain = listOf(
            toolbar.toolbarCalendarView.id,
            toolbar.toolbarSwipeUp.id,
            toolbar.toolbarSwipeDown.id,
            toolbar.toolbarSwitchSide.id,
            toolbar.toolbarRotate.id,
            toolbar.toolbarCloudSync.id,
            toolbar.toolbarSettings.id,
        )
        for ((idx, id) in rightChain.withIndex()) {
            val view = toolbar.root.findViewById<View>(id) ?: continue
            val lp = view.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams ?: continue
            lp.startToStart = unset
            lp.endToEnd = parentId
            lp.topToTop = if (idx == 0) parentId else unset
            lp.topToBottom = if (idx == 0) unset else rightChain[idx - 1]
            lp.bottomToBottom = unset
            lp.bottomToTop = unset
            lp.topMargin = 0
            view.layoutParams = lp
        }
    }

    /**
     * Exit lasso selection / paste mode and reset all selection state.
     */
    private fun exitSelectionMode() {
        selectionMode = false
        hasSelection = false
        pasteMode = false
        textMode = false
        selectionPoints.clear()
        selectedStrokes.clear()
        selBox = null
        selectionDrag = SelectionDrag.NONE
        scaleOrigBox = null
        provideToolbarDrawing().toolbarLasso.background.setTint(Color.WHITE)
        provideToolbarDrawing().toolbarPaste.background.setTint(Color.WHITE)
        provideToolbarDrawing().toolbarText.background.setTint(Color.WHITE)
    }

    // --- Zoom / pan infrastructure ---

    fun isZoomed(): Boolean = zoomScale > 1.01f

    fun resetZoom() {
        zoomScale = 1.0f
        panX = 0.0f
        panY = 0.0f
        updateTransformMatrix()
        applyStrokes(strokes, true)
    }

    private fun updateTransformMatrix() {
        val sw = surfaceSize.width().toFloat()
        val sh = surfaceSize.height().toFloat()
        if (sw <= 0f || sh <= 0f) return

        baseScale = minOf(sw / CANVAS_WIDTH.toFloat(), sh / CANVAS_HEIGHT.toFloat())
        val totalScale = baseScale * zoomScale

        val scaledWidth = CANVAS_WIDTH * totalScale
        val scaledHeight = CANVAS_HEIGHT * totalScale
        val baseOffX = (sw - scaledWidth) / 2f
        val baseOffY = (sh - scaledHeight) / 2f

        if (scaledWidth > sw) {
            val maxPan = (scaledWidth - sw) / 2f
            panX = panX.coerceIn(-maxPan, maxPan)
        } else {
            panX = 0f
        }
        if (scaledHeight > sh) {
            val maxPan = (scaledHeight - sh) / 2f
            panY = panY.coerceIn(-maxPan, maxPan)
        } else {
            panY = 0f
        }

        viewMatrix.reset()
        viewMatrix.postScale(totalScale, totalScale)
        viewMatrix.postTranslate(baseOffX + panX, baseOffY + panY)
        viewMatrix.invert(inverseViewMatrix)

        touchHelper?.setStrokeWidth(paint.strokeWidth * totalScale)

        onTransformChanged(viewMatrix)
    }

    open fun onTransformChanged(matrix: Matrix) {}

    fun handleZoomPanTouch(motionEvent: MotionEvent): Boolean {
        if (motionEvent.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) return false

        if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) twoFingerGesture = false
        if (motionEvent.pointerCount >= 2 || singleFingerGesturesEnabled) twoFingerGesture = true

        if (scaleGestureDetector == null) {
            scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val oldZoom = zoomScale
                    zoomScale = (zoomScale * detector.scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    if (zoomScale != oldZoom) {
                        val focusX = detector.focusX
                        val focusY = detector.focusY

                        val pts = floatArrayOf(focusX, focusY)
                        inverseViewMatrix.mapPoints(pts)
                        val canvasX = pts[0]
                        val canvasY = pts[1]

                        val sw = surfaceSize.width().toFloat()
                        val sh = surfaceSize.height().toFloat()
                        val totalScale = baseScale * zoomScale
                        val scaledW = CANVAS_WIDTH * totalScale
                        val scaledH = CANVAS_HEIGHT * totalScale
                        val baseOffX = (sw - scaledW) / 2f
                        val baseOffY = (sh - scaledH) / 2f

                        val newScreenX = canvasX * totalScale + baseOffX + panX
                        val newScreenY = canvasY * totalScale + baseOffY + panY
                        panX += focusX - newScreenX
                        panY += focusY - newScreenY

                        updateTransformMatrix()
                        applyStrokes(strokes, true)
                    }
                    return true
                }
            })
        }

        if (doubleTapDetector == null) {
            doubleTapDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (isZoomed()) {
                        resetZoom()
                    } else {
                        val pts = floatArrayOf(e.x, e.y)
                        inverseViewMatrix.mapPoints(pts)

                        zoomScale = 2.0f

                        val sw = surfaceSize.width().toFloat()
                        val sh = surfaceSize.height().toFloat()
                        val totalScale = baseScale * zoomScale
                        val scaledW = CANVAS_WIDTH * totalScale
                        val scaledH = CANVAS_HEIGHT * totalScale
                        val baseOffX = (sw - scaledW) / 2f
                        val baseOffY = (sh - scaledH) / 2f

                        val newScreenX = pts[0] * totalScale + baseOffX
                        val newScreenY = pts[1] * totalScale + baseOffY
                        panX = e.x - newScreenX
                        panY = e.y - newScreenY

                        updateTransformMatrix()
                        applyStrokes(strokes, true)
                    }
                    return true
                }
            })
        }

        scaleGestureDetector!!.onTouchEvent(motionEvent)
        doubleTapDetector!!.onTouchEvent(motionEvent)

        // Pan handling. By default we require two fingers (palm rejection). The user
        // can toggle the top toolbar button to allow single-finger panning while zoomed.
        // We only consume single-finger events for pan when actually zoomed in — at
        // zoom=1 single-finger events fall through to swipe-navigation.
        val allowOneFingerPan = singleFingerGesturesEnabled && isZoomed()
        if (motionEvent.pointerCount >= 2 || (allowOneFingerPan && motionEvent.pointerCount == 1)) {
            val cx: Float
            val cy: Float
            if (motionEvent.pointerCount >= 2) {
                cx = (motionEvent.getX(0) + motionEvent.getX(1)) / 2f
                cy = (motionEvent.getY(0) + motionEvent.getY(1)) / 2f
            } else {
                cx = motionEvent.x
                cy = motionEvent.y
            }
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_DOWN -> {
                    lastFingerX = cx
                    lastFingerY = cy
                    isPanning = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isPanning && isZoomed()) {
                        panX += cx - lastFingerX
                        panY += cy - lastFingerY
                        updateTransformMatrix()
                        applyStrokes(strokes, true)
                    }
                    lastFingerX = cx
                    lastFingerY = cy
                }
            }
            return true
        }

        isPanning = false
        if (isZoomed()) return true

        return false
    }

    private fun screenToCanvas(screenX: Float, screenY: Float): FloatArray {
        val pts = floatArrayOf(screenX, screenY)
        inverseViewMatrix.mapPoints(pts)
        return pts
    }

    // --- Selection-overlay geometry (canvas-space) ---

    private val handleSize = 36f
    private val handleHitPad = 30f
    private val chipSize = 88f
    private val chipGap = 16f

    private fun computeSelBox(s: List<Stroke>): RectF? {
        if (s.isEmpty()) return null
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (st in s) for (p in st.strokePoints) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        return RectF(minX, minY, maxX, maxY)
    }

    /** Y position of the chip row — above the box if there's room, otherwise below. */
    private fun chipBaseY(box: RectF): Float {
        return if (box.top > chipSize + 30f) box.top - chipSize - 20f else box.bottom + 20f
    }

    private fun cutChipRect(box: RectF): RectF {
        val top = chipBaseY(box)
        val right = box.right
        return RectF(right - chipSize, top, right, top + chipSize)
    }

    private fun copyChipRect(box: RectF): RectF {
        val top = chipBaseY(box)
        val right = box.right - chipSize - chipGap
        return RectF(right - chipSize, top, right, top + chipSize)
    }

    /** Returns which handle (if any) the canvas-space point hits. */
    private fun hitTestHandle(x: Float, y: Float, box: RectF): SelectionDrag {
        val pad = handleSize / 2f + handleHitPad
        fun near(hx: Float, hy: Float) = abs(x - hx) <= pad && abs(y - hy) <= pad
        return when {
            near(box.left, box.top) -> SelectionDrag.HANDLE_TL
            near(box.right, box.top) -> SelectionDrag.HANDLE_TR
            near(box.left, box.bottom) -> SelectionDrag.HANDLE_BL
            near(box.right, box.bottom) -> SelectionDrag.HANDLE_BR
            else -> SelectionDrag.NONE
        }
    }

    private fun anchorForHandle(handle: SelectionDrag, box: RectF): Pair<Float, Float> = when (handle) {
        SelectionDrag.HANDLE_TL -> box.right to box.bottom
        SelectionDrag.HANDLE_TR -> box.left to box.bottom
        SelectionDrag.HANDLE_BL -> box.right to box.top
        SelectionDrag.HANDLE_BR -> box.left to box.top
        SelectionDrag.NONE, SelectionDrag.MOVE -> 0f to 0f
    }

    /**
     * Redraw the current strokes with selected strokes highlighted (thicker stroke).
     * Also draws the lasso polygon if points exist, plus the selection bounding box,
     * corner handles, and Cut/Copy chips once a selection has been committed.
     */
    private fun drawWithSelection() {
        val lockCanvas = provideSurfaceView().holder.lockCanvas() ?: return

        lockCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val selectedIds = selectedStrokes.map { it.strokeId }.toSet()

        lockCanvas.save()
        lockCanvas.concat(viewMatrix)

        for (stroke in strokes) {
            val usePaint = if (stroke.strokeId in selectedIds) selectionHighlightPaint else paint
            drawStrokePath(lockCanvas, usePaint, stroke)
        }

        // Draw lasso polygon (points are in canvas space)
        if (selectionPoints.size > 1) {
            val lassoPath = Path()
            lassoPath.moveTo(selectionPoints[0].x, selectionPoints[0].y)
            for (i in 1 until selectionPoints.size) {
                lassoPath.lineTo(selectionPoints[i].x, selectionPoints[i].y)
            }
            if (hasSelection) {
                lassoPath.close()
            }
            lockCanvas.drawPath(lassoPath, lassoPaint)
        }

        // Selection overlay: bounding box, handles, and Cut/Copy chips
        val box = selBox
        if (hasSelection && box != null) {
            // Dashed bounding box
            lockCanvas.drawRect(box, lassoPaint)

            // Corner handles (filled squares with a border)
            val h = handleSize / 2f
            val handleFill = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }
            val handleStroke = Paint().apply {
                color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true
            }
            val corners = listOf(
                box.left to box.top, box.right to box.top,
                box.left to box.bottom, box.right to box.bottom,
            )
            for ((cx, cy) in corners) {
                lockCanvas.drawRect(cx - h, cy - h, cx + h, cy + h, handleFill)
                lockCanvas.drawRect(cx - h, cy - h, cx + h, cy + h, handleStroke)
            }

            // Chips
            val cutR = cutChipRect(box)
            val copyR = copyChipRect(box)
            val chipBg = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }
            val chipBorder = Paint().apply {
                color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true
            }
            for (r in listOf(cutR, copyR)) {
                lockCanvas.drawRoundRect(r, 12f, 12f, chipBg)
                lockCanvas.drawRoundRect(r, 12f, 12f, chipBorder)
            }
            // Draw icons inside chips
            val pad = 14
            val cutIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_toolbar_cut, null)
            cutIcon?.setBounds((cutR.left + pad).toInt(), (cutR.top + pad).toInt(),
                (cutR.right - pad).toInt(), (cutR.bottom - pad).toInt())
            cutIcon?.draw(lockCanvas)
            val copyIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_toolbar_copy, null)
            copyIcon?.setBounds((copyR.left + pad).toInt(), (copyR.top + pad).toInt(),
                (copyR.right - pad).toInt(), (copyR.bottom - pad).toInt())
            copyIcon?.draw(lockCanvas)
        }

        lockCanvas.restore()

        touchHelper?.setRawDrawingEnabled(false)
        touchHelper?.isRawDrawingRenderEnabled = false
        provideSurfaceView().holder.unlockCanvasAndPost(lockCanvas)
        touchHelper?.setRawDrawingEnabled(true)
        touchHelper?.isRawDrawingRenderEnabled = true
    }

    /**
     * Set the text elements for the current page and redraw.
     *
     * @param elements the text elements to display
     */
    fun setTextElements(elements: MutableList<TextElement>) {
        this.textElements = elements
    }

    /**
     * Render all text elements onto the given canvas.
     *
     * @param targetCanvas the canvas to draw on
     */
    private fun renderTextElements(targetCanvas: Canvas) {
        val typeface = try {
            ResourcesCompat.getFont(requireContext(), R.font.atkinson_hyperlegible)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        for (element in textElements) {
            textPaint.textSize = element.fontSize
            textPaint.color = element.color
            textPaint.typeface = typeface ?: Typeface.DEFAULT

            // Draw each line of the text (split on newline)
            val lines = element.text.split("\n")
            val lineHeight = textPaint.fontSpacing
            for ((index, line) in lines.withIndex()) {
                targetCanvas.drawText(
                    line,
                    element.x,
                    element.y + lineHeight * (index + 1),
                    textPaint
                )
            }
        }
    }

    /**
     * Show a text input dialog at the given canvas coordinates.
     *
     * @param x the x coordinate on the surface
     * @param y the y coordinate on the surface
     */
    private fun showPenSettingsDialog() {
        val colorNames = arrayOf("Black", "Red", "Blue", "Green")
        val colorValues = intArrayOf(Color.BLACK, Color.RED, Color.BLUE, Color.rgb(0, 128, 0))
        val widthNames = arrayOf("Fine", "Med", "Thick", "Bold")
        val widthValues = floatArrayOf(1.0f, 3.0f, 5.0f, 8.0f)

        var selColor = colorValues.indexOfFirst { it == paint.color }.coerceAtLeast(0)
        var selWidth = widthValues.indexOfFirst { it == paint.strokeWidth }.coerceAtLeast(1)

        val dp = resources.displayMetrics.density
        val ctx = requireContext()
        val root = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (4 * dp).toInt())
        }

        val colorGroup = android.widget.RadioGroup(ctx).apply { orientation = android.widget.RadioGroup.HORIZONTAL }
        val colorBtns = colorNames.mapIndexed { i, name ->
            android.widget.RadioButton(ctx).apply {
                text = name; id = i; isChecked = i == selColor
                textSize = 14f
            }.also { colorGroup.addView(it) }
        }
        colorGroup.setOnCheckedChangeListener { _, id -> selColor = id }
        root.addView(colorGroup)

        val widthGroup = android.widget.RadioGroup(ctx).apply { orientation = android.widget.RadioGroup.HORIZONTAL }
        widthNames.forEachIndexed { i, name ->
            android.widget.RadioButton(ctx).apply {
                text = name; id = i + 10; isChecked = i == selWidth
                textSize = 14f
            }.also { widthGroup.addView(it) }
        }
        widthGroup.setOnCheckedChangeListener { _, id -> selWidth = id - 10 }
        root.addView(widthGroup)

        AlertDialog.Builder(ctx).setView(root)
            .setPositiveButton("OK") { _, _ ->
                paint.color = colorValues[selColor]
                paint.strokeWidth = widthValues[selWidth]
                touchHelper?.setStrokeWidth(paint.strokeWidth * baseScale * zoomScale)
                touchHelper?.setStrokeColor(paint.color)
                provideToolbarDrawing().toolbarPen.background.setTint(
                    if (paint.color == Color.BLACK) Color.GRAY else paint.color
                )
            }
            .setNegativeButton("Cancel", null)
            .create().show()
    }

    private fun showTextInputDialog(x: Float, y: Float) {
        val context = this.requireContext()

        val editText = EditText(context)
        editText.hint = getString(R.string.calendar_text_dialog_hint)
        editText.setSingleLine(false)
        editText.setLines(3)

        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        val margin = (16 * resources.displayMetrics.density).toInt()
        params.setMargins(margin, 0, margin, 0)
        editText.layoutParams = params
        container.addView(editText)

        val builder = AlertDialog.Builder(context)
            .setTitle(R.string.calendar_text_dialog_title)
            .setView(container)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                val text = editText.text.toString().trim()
                if (text.isNotEmpty()) {
                    val element = TextElement(
                        x = x,
                        y = y,
                        text = text
                    )
                    textElements.add(element)
                    applyStrokes(strokes, true)
                    onTextElementsChanged(textElements)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }

        builder.create().show()
        editText.requestFocus()
    }

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
     * Initialize the surface view of drawing.
     *
     * @param first first initialization flag
     */
    fun initializeSurface(first: Boolean = false) {
        if (first) {
            if (com.toolsboox.ot.ViwoodsFastInk.isAvailable) {
                // Viwoods AiPaper: the Onyx TouchHelper.create() succeeds here but is inert
                // (no Onyx hardware) AND swallows the pen, which is why strokes only showed
                // on lift. Skip it entirely; we set the FAST e-ink waveform and render the
                // pen ourselves. Touch events reach us via the SurfaceView's onTouchListener.
                touchHelper = null
                try {
                    viwoodsInk = com.toolsboox.ot.ViwoodsFastInk().also { it.attach(requireContext()) }
                    Timber.i("Viwoods fast-ink backend active on ${Build.MODEL}")
                } catch (t: Throwable) {
                    Timber.w(t, "Viwoods attach() failed")
                    viwoodsInk = null
                }
            } else {
                // Try to create the Onyx TouchHelper on any Boox device. The SDK's
                // hasStylus() heuristic can return false on some models (e.g. Palma 2 Pro)
                // even when raw stylus drawing is supported, which forces the slow
                // MotionEvent fallback rendering path. Catch and fall back only on real
                // failure (non-Onyx device or SDK incompatibility).
                try {
                    touchHelper = TouchHelper.create(provideSurfaceView(), callback)
                    Timber.i("TouchHelper created successfully on ${Build.MODEL}")
                } catch (e: Throwable) {
                    Timber.w(e, "TouchHelper creation failed on ${Build.MODEL}; falling back to MotionEvent rendering")
                    touchHelper = null
                }
            }
            provideSurfaceView().setZOrderOnTop(true)
            provideSurfaceView().holder.setFormat(PixelFormat.TRANSPARENT)
            // Keep the screen on while the planner is showing — prevents the Onyx
            // power manager from idling the device every few seconds (which causes
            // first-stroke-after-idle latency on Palma 2 Pro).
            provideSurfaceView().keepScreenOn = true
        }

        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 3.0f

        if (surfaceCallback == null) {
            surfaceCallback = object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    Timber.i("surfaceCreated")
                    val view = provideSurfaceView()
                    // Use the SurfaceView's full dimensions for the TouchHelper limit rect.
                    // getLocalVisibleRect() can return a clipped rect when system bars or
                    // window insets overlap the view, which caused pen events near the
                    // bottom of the screen to fall through to Android's slow touch path
                    // instead of being captured by Onyx's raw drawing.
                    val limit = Rect(0, 0, view.width, view.height)
                    bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
                    bitmap!!.eraseColor(Color.TRANSPARENT)
                    canvas = Canvas(bitmap!!)

                    if (provideSurfaceView().holder == null) {
                        return
                    }

                    clearSurface()

                    touchHelper?.setLimitRect(limit, ArrayList())?.setStrokeWidth(paint.strokeWidth)?.openRawDrawing()
                    touchHelper?.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
                    touchHelper?.setStrokeColor(paint.color)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    Timber.i("surfaceChanged: ${width}x${height}")
                    surfaceSize = Rect(0, 0, width, height)
                    updateTransformMatrix()
                    // Activate Viwoods T1000 AutoDraw for this surface. The hardware then
                    // renders pen strokes live; we draw nothing during the stroke. Uses
                    // full-screen metrics (not just the surface) to register the region.
                    if (viwoodsInk != null && width > 0 && height > 0) {
                        try {
                            val dm = resources.displayMetrics
                            // The Viwoods T1000 hardware-ink overlay (initWriting) only covers
                            // ~the left third of the AiPaper Mini panel — a native writing-surface
                            // width mismatch we can't size from our process. Until that's solved,
                            // force the software FAST-waveform path, which renders uniformly across
                            // the whole panel (slightly less instant than the native overlay, but
                            // consistent everywhere). Flip this back to BuildConfig.VIWOODS_FAST_INK
                            // to re-enable the native hardware path for testing.
                            val useHardwareInk = false
                            viwoodsInk?.enable(
                                dm.widthPixels, dm.heightPixels,
                                useHardwareInk
                            )
                        } catch (t: Throwable) {
                            Timber.w(t, "Viwoods enable() failed")
                        }
                    }
                    // Re-apply the limit rect with the new dimensions (e.g. after rotation).
                    touchHelper?.let { th ->
                        th.setRawDrawingEnabled(false)
                        th.setLimitRect(Rect(0, 0, width, height), ArrayList())
                        th.setRawDrawingEnabled(true)
                    }
                    // On Android 10+, claim the whole SurfaceView area back from the
                    // system gesture-navigation handler. Without this, the bottom ~10%
                    // of the screen (the back-gesture strip) intercepts pen touches and
                    // never delivers them to the app — strokes there silently vanish.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        provideSurfaceView().systemGestureExclusionRects =
                            listOf(Rect(0, 0, width, height))
                    }
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
        // EpdController is Onyx-only and relies on the SDK being initialized by
        // TouchHelper.create(). On Viwoods we never create the TouchHelper, so this would
        // throw — skip it (AutoDraw owns the panel refresh) and guard defensively.
        if (viwoodsInk == null) {
            try {
                EpdController.enablePost(provideSurfaceView(), 1)
            } catch (t: Throwable) {
                Timber.w(t, "EpdController.enablePost failed")
            }
        }
        lockerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        provideSurfaceView().holder.unlockCanvasAndPost(lockerCanvas)

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    private fun drawStrokePath(targetCanvas: Canvas, strokePaint: Paint, stroke: Stroke) {
        val points = stroke.strokePoints
        if (points.isEmpty()) return
        strokePaint.color = stroke.color
        strokePaint.strokeWidth = stroke.strokeWidth
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
        targetCanvas.drawPath(path, strokePaint)
    }

    /**
     * Apply strokes on the surface. Strokes are in 1404x1872 canvas space.
     *
     * When clearPage is true we wipe and re-render the shadow bitmap too (used on page
     * load, undo/redo, paste etc.). When false (e.g. routine post-stroke refresh) we
     * skip the shadow redraw entirely — the shadow stays consistent because
     * [convertStrokes] appends new strokes to it incrementally. This keeps the per-stroke
     * cost on weaker devices (Palma 2 Pro) low while still committing the surface to the
     * e-ink layer.
     *
     * @param strokes the list of strokes
     * @param clearPage the clear page flag
     */
    fun applyStrokes(strokes: List<Stroke>, clearPage: Boolean) {
        this.strokes = strokes.toMutableList()
        val lockCanvas = provideSurfaceView().holder.lockCanvas() ?: return

        lockCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val strokePaint = Paint(paint)

        // Draw to screen with zoom matrix
        lockCanvas.save()
        lockCanvas.concat(viewMatrix)
        for (stroke in strokes) {
            drawStrokePath(lockCanvas, strokePaint, stroke)
        }
        renderTextElements(lockCanvas)
        lockCanvas.restore()

        // Shadow canvas full rebuild only when caller explicitly wants it (page load,
        // undo/redo, paste — anything that may have removed or relocated strokes).
        if (clearPage) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            for (stroke in strokes) {
                drawStrokePath(canvas, strokePaint, stroke)
            }
            renderTextElements(canvas)
        }

        touchHelper?.setRawDrawingEnabled(false)
        touchHelper?.isRawDrawingRenderEnabled = false
        provideSurfaceView().holder.unlockCanvasAndPost(lockCanvas)
        touchHelper?.setRawDrawingEnabled(true)
        touchHelper?.isRawDrawingRenderEnabled = true
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
     * The raw input callback of Onyx's pen library.
     */
    private val callback: RawInputCallback = object : RawInputCallback() {
        override fun onPenActive(touchPoint: TouchPoint) {
        }

        override fun onPenUpRefresh(refreshRect: RectF) {
        }

        override fun onBeginRawDrawing(b: Boolean, touchPoint: TouchPoint) {
        }

        override fun onEndRawDrawing(b: Boolean, touchPoint: TouchPoint) {
        }

        override fun onRawDrawingTouchPointMoveReceived(touchPoint: TouchPoint) {
        }

        override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList) {
        }

        override fun onBeginRawErasing(b: Boolean, touchPoint: TouchPoint) {
        }

        override fun onEndRawErasing(b: Boolean, touchPoint: TouchPoint) {
        }

        override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint) {
        }

        override fun onRawErasingTouchPointListReceived(touchPointList: TouchPointList) {
        }
    }

    /**
     * The input callback of stylus events.
     */
    fun callback(motionEvent: MotionEvent, hover: Boolean): Boolean {
        if (hover) {
            val actionHoverEnter = motionEvent.action == MotionEvent.ACTION_HOVER_ENTER
            val actionHoverMove = motionEvent.action == MotionEvent.ACTION_HOVER_MOVE
            val actionHoverExit = motionEvent.action == MotionEvent.ACTION_HOVER_EXIT

            if (actionHoverEnter) {
                return true
            } else if (actionHoverMove) {
                return true
            } else if (actionHoverExit) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (lastPoint == null) {
                        convertStrokes()
                    }
                }, 50)
                return true
            }

            return false
        }

        val toolTypeStylus = motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
        val toolTypeEraser = motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER
        val toolTypeFinger = motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER

        // TODO: check on other devices (stylus extra button)
        val actionDown = listOf(MotionEvent.ACTION_DOWN, 211).contains(motionEvent.action)
        val actionMove = listOf(MotionEvent.ACTION_MOVE, 213).contains(motionEvent.action)
        val actionUp = listOf(MotionEvent.ACTION_UP, 212).contains(motionEvent.action)

        val drawing = ((toolTypeStylus || toolTypeEraser) && !touchDrawingState) || (toolTypeFinger && touchDrawingState)
        val erasing = motionEvent.buttonState != 0 || toolTypeEraser

        if (drawing) {
            val canvasPts = screenToCanvas(motionEvent.x, motionEvent.y)
            val x = (10.0f * canvasPts[0]).roundToInt() / 10.0f
            val y = (10.0f * canvasPts[1]).roundToInt() / 10.0f
            val p = (10.0f * motionEvent.pressure).roundToInt() / 10.0f
            val t = Instant.now().toEpochMilli()
            val strokePoint = StrokePoint(x, y, p, t)

            // Note: we intentionally do NOT reject strokes whose coordinates fall outside
            // the 1404x1872 canvas. On devices whose aspect ratio doesn't match (Palma 2
            // Pro, Tab Mini, anything rotated), the canvas is fit-to-surface with
            // whitespace at the edges. The user can see and pen on those whitespace
            // regions — rejecting them caused the TouchHelper to paint a brief preview
            // that vanished on the next applyStrokes refresh, looking like the pen was
            // "blocked" near the edge of the screen.

            // --- Text mode: tap to place a text element ---
            if (textMode) {
                if (actionDown) {
                    showTextInputDialog(x, y)
                }
                return true
            }

            // --- Paste mode: tap to place clipboard contents ---
            if (pasteMode) {
                if (actionDown && strokeClipboard.hasContent) {
                    val pastedStrokes = strokeClipboard.stampAt(x, y)
                    strokes.addAll(pastedStrokes)
                    onStrokesAdded(pastedStrokes)
                    applyStrokes(strokes, true)
                    onStrokeChanged(strokes)
                    // Keep the freshly pasted strokes selected so the user can
                    // immediately drag-to-move or resize them via the lasso overlay.
                    selectedStrokes = pastedStrokes.toMutableList()
                    hasSelection = true
                    selectionMode = false
                    selBox = computeSelBox(selectedStrokes)
                    drawWithSelection()
                    showMessage(R.string.calendar_drawing_toolbar_pasted, provideSurfaceView())
                }
                if (actionUp) {
                    pasteMode = false
                    provideToolbarDrawing().toolbarPaste.background.setTint(Color.WHITE)
                    provideToolbarDrawing().toolbarPen.background.setTint(Color.GRAY)
                }
                return true
            }

            // --- Selection committed: handle chip taps and corner-handle drags ---
            if (hasSelection && !selectionMode) {
                val box = selBox
                if (box != null) {
                    if (actionDown) {
                        // Cut chip?
                        if (cutChipRect(box).contains(x, y)) {
                            undoStack.add(Stroke.listDeepCopy(strokes))
                            if (undoStack.size > 50) undoStack.removeAt(0)
                            redoStack.clear()
                            strokeClipboard.copy(selectedStrokes.toList())
                            val removedIds = selectedStrokes.map { it.strokeId }
                            strokes.removeAll { it.strokeId in removedIds.toSet() }
                            onStrokesDeleted(removedIds)
                            exitSelectionMode()
                            applyStrokes(strokes, true)
                            onStrokeChanged(strokes)
                            return true
                        }
                        // Copy chip?
                        if (copyChipRect(box).contains(x, y)) {
                            strokeClipboard.copy(selectedStrokes.toList())
                            showMessage(R.string.calendar_drawing_toolbar_copied, provideSurfaceView())
                            return true
                        }
                        // Corner handle?
                        val hit = hitTestHandle(x, y, box)
                        if (hit != SelectionDrag.NONE) {
                            undoStack.add(Stroke.listDeepCopy(strokes))
                            if (undoStack.size > 50) undoStack.removeAt(0)
                            redoStack.clear()
                            selectionDrag = hit
                            scaleOrigBox = RectF(box)
                            val (ax, ay) = anchorForHandle(hit, box)
                            scaleAnchorX = ax
                            scaleAnchorY = ay
                            scaleOrigPoints = selectedStrokes.associate { stroke ->
                                stroke.strokeId to stroke.strokePoints.map { it.x to it.y }
                            }
                            return true
                        }
                        // Inside the bbox (but not on a handle/chip)? Start a MOVE drag.
                        if (box.contains(x, y)) {
                            undoStack.add(Stroke.listDeepCopy(strokes))
                            if (undoStack.size > 50) undoStack.removeAt(0)
                            redoStack.clear()
                            selectionDrag = SelectionDrag.MOVE
                            moveStartX = x
                            moveStartY = y
                            scaleOrigPoints = selectedStrokes.associate { stroke ->
                                stroke.strokeId to stroke.strokePoints.map { it.x to it.y }
                            }
                            return true
                        }
                        // Tap outside the box / chips / handles → cancel selection.
                        exitSelectionMode()
                        applyStrokes(strokes, true)
                        return true
                    } else if (actionMove && selectionDrag == SelectionDrag.MOVE) {
                        val dx = x - moveStartX
                        val dy = y - moveStartY
                        for (stroke in selectedStrokes) {
                            val origs = scaleOrigPoints[stroke.strokeId] ?: continue
                            for ((i, pt) in stroke.strokePoints.withIndex()) {
                                if (i >= origs.size) break
                                val (ox, oy) = origs[i]
                                pt.x = ox + dx
                                pt.y = oy + dy
                            }
                        }
                        selBox = computeSelBox(selectedStrokes)
                        drawWithSelection()
                        return true
                    } else if (actionMove && selectionDrag != SelectionDrag.NONE) {
                        val orig = scaleOrigBox ?: return true
                        val origW = orig.width().coerceAtLeast(1f)
                        val origH = orig.height().coerceAtLeast(1f)
                        val newW = abs(x - scaleAnchorX).coerceAtLeast(1f)
                        val newH = abs(y - scaleAnchorY).coerceAtLeast(1f)
                        // Uniform scale based on the larger demanded factor so the
                        // bounding-box still touches the dragged corner exactly.
                        val s = maxOf(newW / origW, newH / origH).coerceIn(0.1f, 10.0f)
                        for (stroke in selectedStrokes) {
                            val origs = scaleOrigPoints[stroke.strokeId] ?: continue
                            for ((i, pt) in stroke.strokePoints.withIndex()) {
                                if (i >= origs.size) break
                                val (ox, oy) = origs[i]
                                pt.x = scaleAnchorX + (ox - scaleAnchorX) * s
                                pt.y = scaleAnchorY + (oy - scaleAnchorY) * s
                            }
                        }
                        selBox = computeSelBox(selectedStrokes)
                        drawWithSelection()
                        return true
                    } else if (actionUp && selectionDrag != SelectionDrag.NONE) {
                        selectionDrag = SelectionDrag.NONE
                        scaleOrigBox = null
                        scaleOrigPoints = emptyMap()
                        applyStrokes(strokes, true)
                        onStrokeChanged(strokes)
                        // applyStrokes wipes the bbox/handles/chips off the surface;
                        // redraw them so the selection stays visible and editable.
                        drawWithSelection()
                        return true
                    }
                }
                // Hover / unhandled — let the pen still flow through to the normal
                // drawing path so the user can write nearby; tapping outside clears.
            }

            // --- Lasso selection mode: collect polygon points ---
            if (selectionMode && !hasSelection) {
                if (actionDown) {
                    selectionPoints.clear()
                    selectionPoints.add(PointF(x, y))
                } else if (actionMove) {
                    selectionPoints.add(PointF(x, y))
                    drawWithSelection()
                } else if (actionUp) {
                    selectionPoints.add(PointF(x, y))
                    // Close polygon and test strokes
                    if (selectionPoints.size >= 3) {
                        selectedStrokes.clear()
                        for (stroke in strokes) {
                            if (StrokeClipboard.isStrokeInsidePolygon(stroke, selectionPoints)) {
                                selectedStrokes.add(stroke)
                            }
                        }
                        hasSelection = true
                        selectionMode = false
                        selBox = computeSelBox(selectedStrokes)
                        drawWithSelection()
                        if (selectedStrokes.isEmpty()) {
                            showMessage(R.string.calendar_drawing_toolbar_nothing_selected, provideSurfaceView())
                        }
                    }
                }
                return true
            }

            // --- Normal drawing / erasing path ---
            if (actionDown) {
                onBeginDrawing(strokePoint)
            } else if (actionMove) {
                val touchPoints = mutableListOf<StrokePoint>()
                for (i in 0 until motionEvent.historySize) {
                    val hPts = screenToCanvas(motionEvent.getHistoricalX(i), motionEvent.getHistoricalY(i))
                    val hx = (10.0f * hPts[0]).roundToInt() / 10.0f
                    val hy = (10.0f * hPts[1]).roundToInt() / 10.0f
                    val hp = (10.0f * motionEvent.getHistoricalPressure(i)).roundToInt() / 10.0f
                    val ht = Instant.now().toEpochMilli() + motionEvent.getHistoricalEventTime(i) - SystemClock.uptimeMillis()
                    touchPoints.add(StrokePoint(hx, hy, hp, ht))
                }

                touchPoints.add(strokePoint)
                onMoveDrawing(touchPoints)
            } else if (actionUp) {
                onEndDrawing(strokePoint, erasing, toolTypeFinger)
            } else {
                if (!actions.contains("${motionEvent.action}")) actions.add("${motionEvent.action}")
                if (!buttons.contains("${motionEvent.buttonState}")) buttons.add("${motionEvent.buttonState}")
            }

            return true
        }

        return false
    }

    private fun epsilon(touchPoint: StrokePoint, lastPoint: StrokePoint): Boolean {
        return epsilon(touchPoint.x, touchPoint.y, lastPoint.x, lastPoint.y, 3.0f)
    }

    private fun epsilon(x1: Float, y1: Float, x2: Float, y2: Float, epsilon: Float): Boolean {
        val dx = abs(x1 - x2).toDouble()
        val dy = abs(y1 - y2).toDouble()
        val d = sqrt(dx * dx + dy * dy)
        return d <= epsilon
    }

    private fun onBeginDrawing(touchPoint: StrokePoint) {
        Timber.i("onBeginDrawing (${touchPoint.x}/${touchPoint.y})")
        // Start the coalescer from a clean slate (drop any post left scheduled from a prior stroke).
        provideSurfaceView().removeCallbacks(viwoodsLivePostRunnable)
        viwoodsLivePostScheduled = false
        lastPoint = touchPoint
        firstPointTimestamp = Instant.now().toEpochMilli()
        touchPoint.t = 0L
        stylusPointList.add(touchPoint)
        if (penState) {
            viwoodsInk?.onStrokeStart()
            viwoodsInk?.reassertFastMode()
        }
    }

    private fun onMoveDrawing(touchPoints: List<StrokePoint>) {
        if (stylusPointList.isEmpty()) return
        val path = Path()
        path.moveTo(stylusPointList[0].x, stylusPointList[0].y)
        stylusPointList.forEach {
            path.lineTo(it.x, it.y)
        }
        touchPoints.forEach { touchPoint ->
            path.lineTo(touchPoint.x, touchPoint.y)
            if (!epsilon(touchPoint, lastPoint!!)) {
                touchPoint.t = Instant.now().toEpochMilli() - firstPointTimestamp
                lastPoint = touchPoint
                stylusPointList.add(touchPoint)
            }
        }

        // Software live rendering for the no-Onyx path. Skipped when Viwoods hardware ink is
        // active (the T1000 renders the live stroke natively — drawing it ourselves too would
        // double-image). On the FAST-waveform fallback the panel is in FAST mode so these
        // partial posts refresh quickly.
        if (touchHelper == null && penState && viwoodsInk?.hardwareInk != true) {
            if (viwoodsInk != null) {
                // Viwoods: coalesce the heavy full-stroke post to one per display frame so a
                // burst of MotionEvents can't build a backlog of full-area FAST refreshes that
                // makes the ink lag behind the nib. Points were already captured above; this
                // only throttles the panel post. See [viwoodsLivePostRunnable].
                if (!viwoodsLivePostScheduled) {
                    viwoodsLivePostScheduled = true
                    provideSurfaceView().postOnAnimation(viwoodsLivePostRunnable)
                }
            } else {
                // Non-Onyx Boox fallback (e.g. Palma 2 Pro): render immediately, unchanged.
                renderLivePreviewSoftware()
            }
        }
    }

    /**
     * Software live-stroke preview: redraw the whole in-progress stroke and post it to the
     * SurfaceView. The full path is redrawn into a whole-stroke dirty rect every time on
     * purpose — a multi-buffered SurfaceView leaves anything outside the dirty rect undefined
     * across buffer swaps, so a smaller/incremental post makes earlier segments blink and
     * vanish. Used by the no-Onyx path; on Viwoods it is driven by the per-frame coalescer.
     */
    private fun renderLivePreviewSoftware() {
        if (stylusPointList.isEmpty()) return
        val path = Path()
        path.moveTo(stylusPointList[0].x, stylusPointList[0].y)
        for (i in 1 until stylusPointList.size) {
            path.lineTo(stylusPointList[i].x, stylusPointList[i].y)
        }

        val totalScale = baseScale * zoomScale
        val sigma = paint.strokeWidth * totalScale * 4.0f

        // Transform canvas-space bounds to screen space for the dirty rect.
        val allX = stylusPointList.map { it.x }
        val allY = stylusPointList.map { it.y }
        val minPt = floatArrayOf(allX.min() - sigma / totalScale, allY.min() - sigma / totalScale)
        val maxPt = floatArrayOf(allX.max() + sigma / totalScale, allY.max() + sigma / totalScale)
        viewMatrix.mapPoints(minPt)
        viewMatrix.mapPoints(maxPt)
        val rect = Rect(
            minPt[0].toInt().coerceAtLeast(0),
            minPt[1].toInt().coerceAtLeast(0),
            maxPt[0].toInt().coerceAtMost(surfaceSize.width()),
            maxPt[1].toInt().coerceAtMost(surfaceSize.height())
        )

        val lockCanvas = provideSurfaceView().holder.lockCanvas(rect) ?: return
        lockCanvas.save()
        lockCanvas.concat(viewMatrix)
        // Use a non-anti-aliased predraw paint: the full path is redrawn every frame, and
        // re-blending AA edge pixels each frame is what fattens the live line. On the 1-bit
        // FAST waveform crisp pixels look the same and can't accumulate.
        lockCanvas.drawPath(path, viwoodsPredrawPaint())
        lockCanvas.restore()
        provideSurfaceView().holder.unlockCanvasAndPost(lockCanvas)
    }

    /** Lazily-built non-AA paint for the Viwoods software live preview (matches [paint] width/color). */
    private var _viwoodsPredrawPaint: Paint? = null
    private fun viwoodsPredrawPaint(): Paint {
        val p = _viwoodsPredrawPaint ?: Paint().also { _viwoodsPredrawPaint = it }
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.isAntiAlias = false
        p.color = paint.color
        p.strokeWidth = paint.strokeWidth
        return p
    }

    private fun onEndDrawing(touchPoint: StrokePoint, erasing: Boolean, finger: Boolean) {
        Timber.i("onEndDrawing (${touchPoint.x}/${touchPoint.y})")
        touchPoint.t = Instant.now().toEpochMilli() - firstPointTimestamp
        stylusPointList.add(touchPoint)

        undoStack.add(Stroke.listDeepCopy(strokes))
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()

        if (!penState || erasing) {
            val strokesToRemove: MutableSet<UUID> = mutableSetOf()
            for (ep in stylusPointList) {
                for (stroke in strokes) {
                    for (tp in stroke.strokePoints) {
                        if (epsilon(ep.x, ep.y, tp.x, tp.y, 25.0f)) {
                            strokesToRemove.add(stroke.strokeId)
                        }
                    }
                }
            }
            if (procrastinator) {
                onStrokesProcrastinated(strokes.filter { it.strokeId in strokesToRemove }.toList())
            }
            strokesToRemove.forEach { strokes.removeIf { stroke -> stroke.strokeId == it } }
            onStrokesDeleted(strokesToRemove.toList())

            applyStrokes(strokes, true)
            onStrokeChanged(strokes)
        } else {
            val stroke = Stroke(UUID.randomUUID(), firstPointTimestamp, stylusPointList.toList(), paint.color, paint.strokeWidth)
            strokes.add(stroke)
            strokesToAdd.add(stroke)

            // Viwoods: the T1000 overlay shows the live stroke then auto-clears ~800ms
            // after pen-up. Re-render the committed strokes to the SurfaceView just after
            // that, so the permanent ink replaces the overlay with no flicker or gap.
            if (viwoodsInk != null) {
                // Drop any coalesced live post still queued, so it can't repaint the preview
                // over the committed strokes we're about to render.
                provideSurfaceView().removeCallbacks(viwoodsLivePostRunnable)
                viwoodsLivePostScheduled = false
                viwoodsInk?.onStrokeEnd()
                if (viwoodsInk?.hardwareInk == true) {
                    // Hardware ink: the native overlay shows the stroke then self-clears ~800ms
                    // after pen-up. Repaint our committed strokes just after, for a seamless
                    // hand-off from the fast (1-bit) overlay to the quality (GL16) layer.
                    val snapshot = Stroke.listDeepCopy(strokes)
                    Handler(Looper.getMainLooper()).postDelayed({ applyStrokes(snapshot, true) }, 900)
                } else {
                    // Software fallback: partial lockCanvas posts don't form a stable buffer,
                    // so repaint immediately to persist the completed mark.
                    applyStrokes(strokes, true)
                }
                onStrokeChanged(strokes)
            }

            // Commit the stroke to storage on pen-up. Previously only finger drawing
            // committed here; stylus drawing relied on the hover-exit handler firing
            // convertStrokes() later. On Boox that hover-exit event isn't reliably
            // delivered, so the stroke stayed only in memory — and a page reload
            // (renderPage → applyStrokes with clearPage) would wipe it before it was
            // ever saved. Committing here persists every stroke immediately, so it
            // survives a reload. convertStrokes() no-ops if there's nothing to add.
            convertStrokes()
        }

        if (actions.isNotEmpty() || buttons.isNotEmpty()) {
            privateFirebaseAnalytics.logEvent("actionsAndButtons") {
                param("brand", Build.BRAND.lowercase())
                param("device", Build.DEVICE.lowercase())
                param("manufacturer", Build.MANUFACTURER.lowercase())
                param("actions", actions.sortedBy { it }.joinToString(","))
                param("buttons", buttons.sortedBy { it }.joinToString(","))
            }
        }

        lastPoint = null
        stylusPointList.clear()
    }

    /**
     * Convert and add the strokes to the internal format and execute the events, like recognition.
     */
    private fun convertStrokes() {
        if (strokesToAdd.isEmpty()) return

        applyStrokes(strokes, false)
        onStrokeChanged(strokes)
        onStrokesAdded(strokesToAdd.toList())

        processStrokes(strokesToAdd)
        strokesToAdd.clear()
    }

    /**
     * Process the strokes, recognize the written text with ML Kit Digital Ink Recognition.
     */
    private fun processStrokes(strokes: List<Stroke>) {
        val inkBuilder = Ink.builder()
        strokes.forEach { stroke ->
            val strokeBuilder: Ink.Stroke.Builder = Ink.Stroke.builder()
            stroke.strokePoints.forEach { point ->
                strokeBuilder.addPoint(Ink.Point.create(point.x, point.y, point.t))
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }
        val ink = inkBuilder.build()

        val remoteModelManager = RemoteModelManager.getInstance()
        DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")?.let { mi ->
            val model = DigitalInkRecognitionModel.builder(mi).build()
            this@SurfaceFragment.lifecycleScope.launch(Dispatchers.IO) {
                if (remoteModelManager.isModelDownloaded(model).await()) {
                    val recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
                    recognizer.recognize(ink).addOnSuccessListener { result ->
                        Timber.i("Recognition result: ${result.candidates}")
                    }.addOnFailureListener { e ->
                        Timber.e("Recognition failed: $e")
                    }
                } else {
                    Timber.w("Model not downloaded yet")
                }
            }
        }
    }
}
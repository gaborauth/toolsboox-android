package com.toolsboox.plugin.kanban.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.GestureDetectorCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.pen.TouchHelper
import com.toolsboox.R
import com.toolsboox.da.Stroke
import com.toolsboox.da.StrokePoint
import com.toolsboox.databinding.FragmentKanbanMainBinding
import com.toolsboox.ot.DeepCopy
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.ot.OnxyPenInputCallback
import com.toolsboox.plugin.kanban.da.v1.CardItem
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/**
 * Kanban planner main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class KanbanMainFragment @Inject constructor() : ScreenFragment() {

    /**
     * The Firebase analytics.
     */
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var presenter: KanbanMainPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_kanban_main

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentKanbanMainBinding

    /**
     * The current selected card and positions.
     */
    private var currentCarouselPositions: MutableList<Int> = mutableListOf(0, 0, 0)

    /**
     * Map of card items and views by id.
     */
    private val cardsById: MutableMap<UUID, CardItem> = mutableMapOf()
    private val cardsByLane: List<MutableList<CardItem>> = listOf(mutableListOf(), mutableListOf(), mutableListOf())

    /**
     * Carousel card views.
     */
    private val carouselCardViews: MutableList<MutableList<CardView?>?> = mutableListOf()

    /**
     * The width of the screen.
     */
    private var width: Int = 0

    /**
     * The height of the screen.
     */
    private var height: Int = 0

    /**
     * Margin of the widgets.
     */
    private var margin: Int = 0

    /**
     * The grid size of the layout.
     */
    private var gridSize: Int = 0

    /**
     * Title of lanes.
     */
    private val laneTitles = mutableListOf<TextView>()

    /**
     * The bitmap of the preview canvas.
     */
    private lateinit var previewBitmap: Bitmap

    /**
     * The canvas of the card preview.
     */
    private lateinit var previewCanvas: Canvas

    /**
     * The bitmap of the edit canvas.
     */
    private lateinit var editBitmap: Bitmap

    /**
     * The canvas of the card edit.
     */
    private lateinit var editCanvas: Canvas

    /**
     * The currently edited card.
     */
    private var editedCard: CardItem? = null

    /**
     * The pen raw input callback class.
     */
    private lateinit var callback: OnxyPenInputCallback

    /**
     * TouchHelper of the Onyx's pen.
     */
    private lateinit var touchHelper: TouchHelper

    /**
     * The surface view.
     */
    private lateinit var surfaceView: SurfaceView

    /**
     * The callback of the surface holder.
     */
    private var surfaceCallback: SurfaceHolder.Callback? = null

    /**
     * The surface offset.
     */
    private var surfaceOffset = Rect()

    /**
     * The gesture detector
     */
    private lateinit var gestureDetector: GestureDetectorCompat

    /**
     * The gesture listener
     */
    private lateinit var gestureListener: OnGestureListener

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentKanbanMainBinding.bind(view)

        binding.gridLayout.post {
            width = resources.displayMetrics.widthPixels
            height = resources.displayMetrics.heightPixels
            margin = (4 * resources.displayMetrics.density).toInt()
            gridSize = (width / 9.0f).toInt()

            surfaceView = createEditCard()
            callback = OnxyPenInputCallback(penCallback, eraseCallback)
            touchHelper = TouchHelper.create(binding.drawLayout, callback)
            initializeSurface()

            laneTitles.clear()
            binding.titleLayout.removeAllViews()
            createLaneTitle(0, getString(R.string.kanban_planner_lane_dueSoonBacklogTitle, 0))
            createLaneTitle(1, getString(R.string.kanban_planner_lane_plannedTodayTitle, 0))
            createLaneTitle(2, getString(R.string.kanban_planner_lane_doneTodayTitle, 0))

            createCarouselCardViews()
            placeCardItems()
            presenter.loadLocal(this)
        }

        binding.fabAddItem.setOnClickListener {
            addNewCardItem()
        }

        gestureListener = OnGestureListener()
        gestureDetector = GestureDetectorCompat(requireActivity(), gestureListener)
    }

    /**
     * Add new (blank) card item.
     */
    private fun addNewCardItem() {
        val c = Calendar.getInstance()
        c.add(Calendar.DATE, 1)

        val newCard = CardItem(UUID.randomUUID(), 1, 0, c.time, null, mutableListOf())
        cardsById[newCard.id] = newCard
        cardsByLane[0].add(newCard)
        presenter.saveLocal(this, newCard)
        placeCardItems()
    }

    /**
     * Create static carousel view of cards.
     */
    private fun createCarouselCardViews() {
        for (lane in 0..2) {
            carouselCardViews.add(mutableListOf())
            for (position in -4..4) {
                carouselCardViews[lane]!!.add(null)
            }

            createCarouselCardView(lane, -4)
            createCarouselCardView(lane, 4)
            createCarouselCardView(lane, -3)
            createCarouselCardView(lane, 3)
            createCarouselCardView(lane, -2)
            createCarouselCardView(lane, 2)
            createCarouselCardView(lane, -1)
            createCarouselCardView(lane, 1)
            createCarouselCardView(lane, 0)
        }
    }

    /**
     * Create static carousel view of a card.
     *
     * @param lane the lane
     * @param position the position
     */
    private fun createCarouselCardView(lane: Int, position: Int) {
        val itemView = LayoutInflater.from(context).inflate(R.layout.card_kanban, binding.gridLayout, false) as CardView
        itemView.id = View.generateViewId()
        carouselCardViews[lane]!![position + 4] = itemView

        val w = gridSize * 3 - 2 * margin
        val h = gridSize * 2 - 2 * margin
        val layoutParams = RelativeLayout.LayoutParams(w, h)

        val hh = (binding.gridLayout.height / 2) - h / 2 - margin * 5
        var o = hh
        if (position != 0) {
            o = hh + (sign(position * 1.0) * (h - h / 2 * 0.75.pow(abs(position) - 1.0))).toInt()
        }

        layoutParams.leftMargin = 3 * lane * gridSize + margin
        layoutParams.topMargin = margin * 10 + o
        itemView.layoutParams = layoutParams
        binding.gridLayout.addView(itemView)

        /**
         * Render actions only the top card.
         */
        if (position != 0) return

        val imageView = itemView.findViewById<ImageView>(R.id.cardPreview)
        imageView.setOnTouchListener { v, e ->
            val result = gestureListener.onTouchEvent(gestureDetector, v, e)
            if (result != OnGestureListener.NONE) {
                when (result) {
                    OnGestureListener.UTD -> {
                        if (currentCarouselPositions[lane] > 0) {
                            currentCarouselPositions[lane] = currentCarouselPositions[lane] - 1
                            placeCardItems()
                        }
                    }

                    OnGestureListener.DTU -> {
                        if (currentCarouselPositions[lane] < cardsByLane[lane].size - 1) {
                            currentCarouselPositions[lane] = currentCarouselPositions[lane] + 1
                            placeCardItems()
                        }
                    }

                    OnGestureListener.LTR -> {
                        if (lane < 2) {
                            val cardToMove = cardsByLane[lane][currentCarouselPositions[lane]]
                            if (lane == 1) {
                                cardToMove.doneDate = Date()
                            }

                            cardToMove.lane = cardToMove.lane + 1
                            cardsByLane[lane].removeAt(currentCarouselPositions[lane])
                            cardsByLane[lane + 1].add(cardToMove)
                            presenter.saveLocal(this, cardToMove)

                            currentCarouselPositions[lane] = 0
                            currentCarouselPositions[lane + 1] = 0
                        }

                        placeCardItems()
                    }

                    OnGestureListener.RTL -> {
                        if (lane > 0) {
                            val cardToMove = cardsByLane[lane][currentCarouselPositions[lane]]
                            cardToMove.lane = cardToMove.lane - 1
                            cardToMove.doneDate = null
                            cardsByLane[lane].removeAt(currentCarouselPositions[lane])
                            cardsByLane[lane - 1].add(cardToMove)
                            presenter.saveLocal(this, cardToMove)

                            currentCarouselPositions[lane] = 0
                            currentCarouselPositions[lane - 1] = 0
                        }

                        placeCardItems()
                    }
                }
            }
            v.performClick()
            true
        }

        val editButton = itemView.findViewById<ImageView>(R.id.editButton)
        editButton.visibility = View.VISIBLE
        editButton.setOnClickListener { v ->
            binding.titleLayout.visibility = View.INVISIBLE
            binding.gridLayout.visibility = View.INVISIBLE
            binding.drawLayout.visibility = View.VISIBLE

            editedCard = DeepCopy.deepCopy(cardsByLane[lane][currentCarouselPositions[lane]])
            clearSurface()
            touchHelper.setRawDrawingEnabled(true)

            val dueDate = binding.drawLayout.findViewById<TextView>(R.id.dueDate)
            if (editedCard?.dueDate == null) {
                dueDate.text = getString(R.string.kanban_planner_no_due_date)
            } else {
                dueDate.text = SimpleDateFormat("yyyy-MM-dd HH:mm").format(editedCard!!.dueDate)
            }
        }

        val settingsButton = itemView.findViewById<ImageView>(R.id.settingsButton)
        settingsButton.visibility = View.VISIBLE
        settingsButton.setOnClickListener { v -> showDateTimePicker(lane) }
    }

    /**
     * Render the result of the load method.
     *
     * @param cardItems the loaded card items
     */
    fun renderLoad(cardItems: MutableMap<UUID, CardItem>) {
        cardsById.clear()
        cardsById.putAll(cardItems)
        cardsById.values.forEach { cardsByLane[it.lane].add(it) }

        placeCardItems()

        /**
         * Add a blank card item if there is no saved cards.
         */
        if (cardsById.isEmpty()) addNewCardItem()
    }

    /**
     * Place card items to the carousel view.
     */
    private fun placeCardItems() {
        laneTitles[0].text = getString(R.string.kanban_planner_lane_dueSoonBacklogTitle, cardsByLane[0].size)
        laneTitles[1].text = getString(R.string.kanban_planner_lane_plannedTodayTitle, cardsByLane[1].size)
        laneTitles[2].text = getString(R.string.kanban_planner_lane_doneTodayTitle, cardsByLane[2].size)

        cardsByLane[0].sortBy { it.dueDate }
        cardsByLane[1].sortBy { it.dueDate }
        cardsByLane[2].sortBy { it.dueDate }

        for (lane in 0..2) {
            for (position in -4..4) {
                val view = carouselCardViews[lane]!![position + 4]!!
                view.visibility = View.INVISIBLE
            }
            Timber.i("Lane: $lane")
            for (position in 0 until cardsByLane[lane].size) {
                val currentCarouselPosition = position - currentCarouselPositions[lane]
                Timber.i("Current carousel position: $currentCarouselPosition = $position - ${currentCarouselPositions[lane]}")
                if (currentCarouselPosition in -4..4) {
                    val view = carouselCardViews[lane]!![currentCarouselPosition + 4]!!
                    view.visibility = View.VISIBLE

                    val currentCard = cardsByLane[lane][position]
                    drawStroke(currentCard, previewCanvas)
                    val cardPreview = view.findViewById<ImageView>(R.id.cardPreview)
                    cardPreview.setImageBitmap(previewBitmap.copy(previewBitmap.config!!, false))
                    cardPreview.invalidate()

                    val dueDate = view.findViewById<TextView>(R.id.dueDateText)
                    dueDate.text = SimpleDateFormat("yyyy-MM-dd HH:mm").format(currentCard.dueDate)
                }
            }
        }
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.kanban_planner_main_title))

        firebaseAnalytics.logEvent("kanbanBoard") {}
    }

    /**
     * Render the result of 'list' service call.
     *
     * @param cards the list of cards
     */
    fun listResult(cards: List<CardItem>) {
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
        Timber.i("Initialize SurfaceView with size ${surfaceView.width} x ${surfaceView.height}")

        surfaceCallback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Timber.i("surfaceCreated")
                surfaceView.getGlobalVisibleRect(surfaceOffset)
                Timber.i("SurfaceOffset: $surfaceOffset")

                val frameLayoutOffset = Rect()
                binding.frameLayout.getGlobalVisibleRect(frameLayoutOffset)
                Timber.i("FrameLayoutOffset: $frameLayoutOffset")

                surfaceOffset.top = surfaceOffset.top - frameLayoutOffset.top
                surfaceOffset.bottom = surfaceOffset.bottom - frameLayoutOffset.top
                Timber.i("Final SurfaceOffset: $surfaceOffset")

                editBitmap = Bitmap.createBitmap(
                    surfaceView.width,
                    surfaceView.height,
                    Bitmap.Config.ARGB_8888
                )
                editBitmap.eraseColor(Color.WHITE)
                editCanvas = Canvas(editBitmap)
                Timber.i("Edit bitmap and canvas created: ${surfaceView.width} x ${surfaceView.height}")

                previewBitmap = Bitmap.createBitmap(
                    surfaceView.width,
                    surfaceView.height,
                    Bitmap.Config.ARGB_8888
                )
                previewBitmap.eraseColor(Color.WHITE)
                previewCanvas = Canvas(previewBitmap)
                Timber.i("Preview bitmap and canvas created: ${surfaceView.width} x ${surfaceView.height}")

                if (surfaceView.holder == null) {
                    return
                }

                clearSurface()

                touchHelper.closeRawDrawing()
                touchHelper.setLimitRect(listOf(surfaceOffset), listOf())
                touchHelper.setStrokeWidth(3.0f)
                touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_NEO_BRUSH)
                touchHelper.openRawDrawing()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Timber.i("surfaceChanged with format $format and size $width x $height")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Timber.i("surfaceDestroyed")
                holder.removeCallback(surfaceCallback)
                surfaceCallback = null
            }
        }

        surfaceView.holder.addCallback(surfaceCallback)
        surfaceView.invalidate()
    }

    /**
     * Clear the surface and the shadow canvas.
     */
    private fun clearSurface() {
        val lockerCanvas = surfaceView.holder.lockCanvas() ?: return
        EpdController.enablePost(surfaceView, 1)

        if (editedCard != null) {
            drawStroke(editedCard!!, lockerCanvas)
        }

        surfaceView.holder.unlockCanvasAndPost(lockerCanvas)
    }

    /**
     * Draw strokes of the card to the canvas.
     *
     * @param cardItem the card item
     * @param lockerCanvas the canvas
     */
    private fun drawStroke(cardItem: CardItem, lockerCanvas: Canvas) {
        val linePaint = Paint()
        linePaint.isAntiAlias = true
        linePaint.style = Paint.Style.STROKE
        linePaint.color = Color.BLACK
        linePaint.strokeWidth = 3.0f

        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.WHITE
        val rect = Rect(0, 0, surfaceView.width, surfaceView.height)
        lockerCanvas.drawRect(rect, fillPaint)
        editCanvas.drawRect(rect, fillPaint)

        for (stroke in cardItem.strokes) {
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

                lockerCanvas.drawPath(path, linePaint)
                editCanvas.drawPath(path, linePaint)
            }
        }
    }

    /**
     * Create lane title
     *
     * @param position the position
     * @param title the title
     */
    private fun createLaneTitle(position: Int, title: String) {
        laneTitles.add(TextView(requireContext()))
        laneTitles[position].text = title
        laneTitles[position].gravity = Gravity.CENTER
        laneTitles[position].textSize = margin * 2.5f

        val layoutParams = RelativeLayout.LayoutParams(gridSize * 3 - 2 * margin, margin * 10)
        layoutParams.leftMargin = 3 * position * gridSize + margin
        layoutParams.topMargin = 0
        laneTitles[position].layoutParams = layoutParams

        binding.titleLayout.addView(laneTitles[position])
    }

    /**
     * Create editable view of card.
     */
    private fun createEditCard(): SurfaceView {
        val itemEdit = LayoutInflater.from(context).inflate(
            R.layout.card_kanban_edit, binding.gridLayout, false
        ) as CardView

        val layoutParams = RelativeLayout.LayoutParams(
            gridSize * 3 - 2 * margin,
            gridSize * 2 - 2 * margin
        )

        layoutParams.leftMargin = 3 * gridSize + margin
        layoutParams.topMargin = margin * 10 + (1.5f * gridSize).toInt() + margin
        itemEdit.layoutParams = layoutParams
        binding.drawLayout.addView(itemEdit)

        val saveButton = itemEdit.findViewById<ImageView>(R.id.saveButton)
        saveButton.setOnClickListener { v ->
            binding.drawLayout.visibility = View.INVISIBLE
            binding.gridLayout.visibility = View.VISIBLE
            binding.titleLayout.visibility = View.VISIBLE
            touchHelper.setRawDrawingEnabled(false)

            if (editedCard != null) {
                val deepCardCopy = DeepCopy.deepCopy(editedCard!!)

                val lane = deepCardCopy.lane
                val currentCarouselPosition = currentCarouselPositions[lane]

                cardsById[deepCardCopy.id] = deepCardCopy
                cardsByLane[lane][currentCarouselPosition] = deepCardCopy

                placeCardItems()

                presenter.saveLocal(this, deepCardCopy)
            }
        }

        val eraseButton = itemEdit.findViewById<ImageView>(R.id.eraseButton)
        eraseButton.setOnClickListener { v ->
            if (editedCard != null) {
                editedCard!!.strokes.clear()
                clearSurface()
            }
        }

        val cancelButton = itemEdit.findViewById<ImageView>(R.id.cancelButton)
        cancelButton.setOnClickListener { v ->
            binding.drawLayout.visibility = View.INVISIBLE
            binding.gridLayout.visibility = View.VISIBLE
            binding.titleLayout.visibility = View.VISIBLE
            touchHelper.setRawDrawingEnabled(false)
        }

        binding.drawLayout.requestLayout()

        return itemEdit.findViewById(R.id.surfaceView)
    }

    /**
     * Pen callback of the raw input callback.
     */
    private val penCallback = object : OnxyPenInputCallback.PenCallback {
        override fun addStrokes(strokes: List<StrokePoint>) {
            val offsetStrokes = mutableListOf<StrokePoint>()
            for (stroke in strokes) {
                offsetStrokes.add(StrokePoint(stroke.x - surfaceOffset.left, stroke.y - surfaceOffset.top, stroke.p))
            }
            editedCard?.strokes?.add(Stroke(UUID.randomUUID(), 0L, offsetStrokes))
        }
    }

    /**
     * Pen callback of the raw input callback.
     */
    private val eraseCallback = object : OnxyPenInputCallback.EraseCallback {
        override fun removeStroke(strokeId: UUID) {
            Timber.i("Not implemented yet.")
        }
    }

    /**
     * Show date and time picker.
     *
     * @param lane the lane of the settings
     */
    private fun showDateTimePicker(lane: Int) {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.time = cardsByLane[lane][currentCarouselPositions[lane]].dueDate

        DatePickerDialog(requireContext(), { dv, year, month, dayOfMonth ->
            TimePickerDialog(context, { tv, hourOfDay, minute ->
                val newCal = Calendar.getInstance()
                newCal.clear()
                newCal.set(Calendar.YEAR, year)
                newCal.set(Calendar.MONTH, month)
                newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                newCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                newCal.set(Calendar.MINUTE, minute)

                val card = cardsByLane[lane][currentCarouselPositions[lane]]
                card.dueDate = newCal.time

                cardsById[card.id] = card
                cardsByLane[lane][currentCarouselPositions[lane]] = card

                presenter.saveLocal(this, card)

                placeCardItems()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }
}

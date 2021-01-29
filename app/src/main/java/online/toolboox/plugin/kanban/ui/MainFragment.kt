package online.toolboox.plugin.kanban.ui

import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.GestureDetectorCompat
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.pen.TouchHelper
import kotlinx.coroutines.*
import online.toolboox.R
import online.toolboox.databinding.FragmentKanbanMainBinding
import online.toolboox.ot.OnGestureListener
import online.toolboox.ot.PenRawInputCallback
import online.toolboox.plugin.kanban.da.CardItem
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Kanban planner main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class MainFragment @Inject constructor(
    private val presenter: MainPresenter,
    private val router: Router
) : ScreenFragment() {

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_kanban_main

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentKanbanMainBinding

    /**
     * The card items on the five lane.
     */
    private val cardAllBacklog = mutableListOf<CardItem>()
    private val cardBacklog = mutableListOf<CardItem>()
    private val cardToday = mutableListOf<CardItem>()
    private val cardTodayDone = mutableListOf<CardItem>()
    private val cardAllDone = mutableListOf<CardItem>()

    /**
     * The current card.
     */
    private var currentCard: CardItem? = null

    /**
     * Map of card views by id.
     */
    private val cardsById: MutableMap<UUID, CardView> = mutableMapOf()

    /**
     * The timer job.
     */
    private lateinit var timer: Job

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
     * The bitmap of the canvas.
     */
    private lateinit var bitmap: Bitmap

    /**
     * The canvas of the surface view.
     */
    private lateinit var canvas: Canvas

    /**
     * The pen raw input callback class.
     */
    private lateinit var callback: PenRawInputCallback

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

        laneTitles.add(TextView(requireContext()))
        laneTitles.add(TextView(requireContext()))
        laneTitles.add(TextView(requireContext()))

        binding.root.post {
            width = resources.displayMetrics.widthPixels
            height = resources.displayMetrics.heightPixels
            margin = (4 * resources.displayMetrics.density).toInt()
            gridSize = (width / 9.0f).toInt()

            surfaceView = createEditCard()
            callback = PenRawInputCallback(null, null)
            touchHelper = TouchHelper.create(binding.drawLayout, callback)
            initializeSurface()

            cardBacklog.forEachIndexed { position, it -> createCard(0, position, it) }
            cardToday.forEachIndexed { position, it -> createCard(1, position, it) }
            cardTodayDone.forEachIndexed { position, it -> createCard(2, position, it) }

            createLaneTitle(0, "Due soon backlog (${cardBacklog.size})")
            createLaneTitle(1, "Planned today (${cardToday.size})")
            createLaneTitle(2, "Done today (${cardTodayDone.size})")
        }

        gestureListener = OnGestureListener()
        gestureDetector = GestureDetectorCompat(requireActivity(), gestureListener)

        cardBacklog.add(CardItem(UUID.randomUUID(), 1, mutableListOf()))
        cardBacklog.add(CardItem(UUID.randomUUID(), 1, mutableListOf()))
        cardBacklog.add(CardItem(UUID.randomUUID(), 1, mutableListOf()))
        cardBacklog.add(CardItem(UUID.randomUUID(), 1, mutableListOf()))

        cardToday.add(CardItem(UUID.randomUUID(), 1, mutableListOf()))

        cardTodayDone.add(CardItem(UUID.randomUUID(), 1, mutableListOf()))
        cardTodayDone.add(CardItem(UUID.randomUUID(), 1, mutableListOf()))
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.kanban_planner_main_title))

        timer = GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                presenter.list(this@MainFragment)
                delay(30000L)
            }
        }
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        timer.cancel()
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
                Timber.i("Limit: $surfaceOffset")

                bitmap = Bitmap.createBitmap(
                    surfaceView.width,
                    surfaceView.height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(Color.WHITE)
                canvas = Canvas(bitmap)

                if (surfaceView.holder == null) {
                    return
                }

                clearSurface(null)

                touchHelper.closeRawDrawing()
                touchHelper.setLimitRect(listOf(surfaceOffset), listOf())
                touchHelper.setStrokeWidth(3.0f)
                touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_BRUSH)
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
     *
     * @param cardItem
     */
    private fun clearSurface(cardItem: CardItem?) {
        val lockerCanvas = surfaceView.holder.lockCanvas() ?: return

        EpdController.enablePost(surfaceView, 1)
        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.WHITE
        val rect = Rect(0, 0, surfaceView.width, surfaceView.height)
        lockerCanvas.drawRect(rect, fillPaint)
        canvas.drawRect(rect, fillPaint)

        val linePaint = Paint()
        linePaint.isAntiAlias = true
        linePaint.style = Paint.Style.STROKE
        linePaint.color = Color.BLACK
        linePaint.strokeWidth = 3.0f

        if (cardItem != null) {
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
                    canvas.drawPath(path, linePaint)
                }
            }
        }

        surfaceView.holder.unlockCanvasAndPost(lockerCanvas)
    }

    /**
     * Create lane title
     *
     * @param position the position
     * @param title the title
     */
    private fun createLaneTitle(position: Int, title: String) {
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
     *
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

            if (currentCard != null) {
                val itemView = cardsById[currentCard!!.id]
                val cardPreview = itemView!!.findViewById<ImageView>(R.id.cardPreview)
                cardPreview.setImageBitmap(bitmap)
                cardPreview.invalidate()
            }
        }

        binding.drawLayout.requestLayout()

        return itemEdit.findViewById(R.id.surfaceView)
    }

    /**
     * Create card by item
     *
     * @param lane displayed lane
     * @param position the position
     * @param cardItem the card item
     */
    private fun createCard(lane: Int, position: Int, cardItem: CardItem) {
        val itemView = LayoutInflater.from(context).inflate(R.layout.card_kanban, binding.gridLayout, false) as CardView
        itemView.id = View.generateViewId()

        cardItem.id = UUID.randomUUID()
        cardsById[cardItem.id] = itemView

        val layoutParams = RelativeLayout.LayoutParams(
            gridSize * 3 - 2 * margin,
            gridSize * 2 - 2 * margin
        )

        layoutParams.leftMargin = 3 * lane * gridSize + margin
        layoutParams.topMargin = margin * 10 + position * 2 * gridSize + margin
        itemView.layoutParams = layoutParams

        binding.gridLayout.addView(itemView)

        val imageView = itemView.findViewById<ImageView>(R.id.cardPreview)
        imageView.setOnTouchListener { v, e ->
            val result = gestureListener.onTouchEvent(gestureDetector, v, e)
            if (result != OnGestureListener.NONE) {
                Timber.i("Swipe result: $result")
            }
            v.performClick()
            true
        }

        val editButton = itemView.findViewById<ImageView>(R.id.editButton)
        editButton.setOnClickListener { v ->
            binding.titleLayout.visibility = View.INVISIBLE
            binding.gridLayout.visibility = View.INVISIBLE
            binding.drawLayout.visibility = View.VISIBLE

            currentCard = cardItem
            clearSurface(cardItem)
            touchHelper.setRawDrawingEnabled(true)
        }

        itemView.post {
            val bitmap = Bitmap.createBitmap(itemView.width, itemView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val fillPaint = Paint()
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = Color.WHITE

            canvas.drawRect(0.0f, 0.0f, itemView.width.toFloat(), itemView.height.toFloat(), fillPaint)
            imageView.setImageBitmap(bitmap)
        }
    }
}

package online.toolboox.plugin.kanban.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.GestureDetectorCompat
import kotlinx.coroutines.*
import online.toolboox.R
import online.toolboox.databinding.FragmentKanbanMainBinding
import online.toolboox.ot.OnGestureListener
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

            cardBacklog.forEachIndexed { position, it -> createCard(0, position, it) }
            cardToday.forEachIndexed { position, it -> createCard(1, position, it) }
            cardTodayDone.forEachIndexed { position, it -> createCard(2, position, it) }

            createLaneTitle(0, "Due soon backlog (${cardBacklog.size})")
            createLaneTitle(1, "Planned today (${cardToday.size})")
            createLaneTitle(2, "Done today (${cardTodayDone.size})")
        }

        gestureListener = OnGestureListener()
        gestureDetector = GestureDetectorCompat(requireActivity(), gestureListener)

        cardBacklog.add(CardItem(UUID.randomUUID().toString(), 1, "content1"))
        cardBacklog.add(CardItem(UUID.randomUUID().toString(), 1, "content2"))
        cardBacklog.add(CardItem(UUID.randomUUID().toString(), 1, "content3"))
        cardBacklog.add(CardItem(UUID.randomUUID().toString(), 1, "content4"))

        cardToday.add(CardItem(UUID.randomUUID().toString(), 1, "content1"))

        cardTodayDone.add(CardItem(UUID.randomUUID().toString(), 1, "content1"))
        cardTodayDone.add(CardItem(UUID.randomUUID().toString(), 1, "content2"))
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
     * Create card by item
     *
     * @param lane displayed lane
     * @param position the position
     * @param cardItem the card item
     */
    private fun createCard(lane: Int, position: Int, cardItem: CardItem) {
        val itemView = LayoutInflater.from(context).inflate(R.layout.card_kanban, binding.gridLayout, false) as CardView
        itemView.id = View.generateViewId()

        cardItem.id = UUID.randomUUID().toString()

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

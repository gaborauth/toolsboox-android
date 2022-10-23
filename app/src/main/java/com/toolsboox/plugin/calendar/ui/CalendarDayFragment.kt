package com.toolsboox.plugin.calendar.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarDayBinding
import com.toolsboox.plugin.calendar.da.Calendar
import com.toolsboox.plugin.calendar.da.CalendarDay
import com.toolsboox.plugin.calendar.ot.CalendarDayCreator
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.SurfaceFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*
import javax.inject.Inject

/**
 * Calendar day view fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarDayFragment @Inject constructor() : SurfaceFragment() {

    /**
     * The injected router.
     */
    @Inject
    lateinit var router: Router

    /**
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarDayPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_calendar_day

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarDayBinding

    /**
     * The current date.
     */
    private var currentDate: LocalDate = LocalDate.now()

    /**
     * The timer job.
     */
    private lateinit var timer: Job

    /**
     * The canvas of the template.
     */
    private lateinit var templateCanvas: Canvas

    /**
     * The bitmap of the template.
     */
    private lateinit var templateBitmap: Bitmap

    /**
     * The data class.
     */
    private lateinit var calendarDay: CalendarDay

    /**
     * SurfaceView provide method.
     *
     * @return the actual surfaceView
     */
    override fun provideSurfaceView(): SurfaceView = binding.surfaceView

    /**
     * Stroke changed callback.
     *
     * @param strokes the actual strokes
     */
    override fun onStrokeChanged(strokes: MutableList<Stroke>) {
        val year = currentDate.year
        val month = currentDate.monthValue
        val day = currentDate.dayOfMonth
        val locale = calendarDay.locale ?: Locale.getDefault()

        calendarDay = CalendarDay(year, month, day, locale, Calendar.listDeepCopy(strokes))
        calendarDay.normalizeStrokes(getSurfaceSize().width(), getSurfaceSize().height(), 1404, 1872)

        presenter.save(this, binding, calendarDay, currentDate)
    }

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarDayBinding.bind(view)

        currentDate = LocalDate.now()
        parameters["year"]?.toIntOrNull()?.let { year ->
            Timber.i("Set year to '$year' from parameter")
            currentDate = LocalDate.of(year, 1, 1)
            parameters["month"]?.toIntOrNull()?.let { month ->
                Timber.i("Set year and month to '$year'/'$month' from parameter")
                currentDate = LocalDate.of(year, month, 1)
                parameters["day"]?.toIntOrNull()?.let { day ->
                    Timber.i("Set year, month and day to '$year'/'$month'/'$day' from parameter")
                    currentDate = LocalDate.of(year, month, day)
                }
            }
        }

        binding.buttonPrev.setOnClickListener {
            currentDate = currentDate.minusDays(1L)
            presenter.load(this, binding, currentDate, getSurfaceSize())
        }
        binding.buttonNext.setOnClickListener {
            currentDate = currentDate.plusDays(1L)
            presenter.load(this, binding, currentDate, getSurfaceSize())
        }

        binding.buttonYear.setOnClickListener {
            val year = currentDate.year
            Timber.i("Route to the '$year' yearly calendar")
            router.dispatch("/calendar/year/$year", false)
        }

        binding.buttonMonth.setOnClickListener {
            val year = currentDate.year
            val month = currentDate.monthValue
            Timber.i("Route to the '$year'/'$month' monthly calendar")
            router.dispatch("/calendar/month/$year/$month", false)
        }

        binding.buttonDay.setOnClickListener {
            val year = currentDate.year
            val month = currentDate.monthValue
            val day = currentDate.dayOfMonth
            Timber.i("Route to the '$year'/'$month'/'$day' daily calendar")
            router.dispatch("/calendar/day/$year/$month/$day", false)
        }

        binding.buttonWeek.setOnClickListener {
        }
        binding.buttonDayOfWeek.setOnClickListener {
        }

        toolBar.toolbarPager.visibility = View.GONE
        updateNavigator(true)

        templateBitmap = Bitmap.createBitmap(1404, 1872, Bitmap.Config.ARGB_8888)
        templateCanvas = Canvas(templateBitmap)

        binding.templateImage.setImageBitmap(templateBitmap)
        binding.templateImage.postInvalidate()

        initializeSurface(true)
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        timer = GlobalScope.launch(Dispatchers.Main) {
            presenter.load(this@CalendarDayFragment, binding, currentDate, getSurfaceSize())
        }
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        toolBar.toolbarPager.visibility = View.GONE
        timer.cancel()
    }

    /**
     * Reload the current page.
     */
    fun renderPage(calendarDay: CalendarDay) {
        this.calendarDay = calendarDay
        updateNavigator()

        CalendarDayCreator.drawPage(this.requireContext(), templateCanvas, calendarDay)
        binding.templateImage.postInvalidate()

        applyStrokes(calendarDay.strokes.toMutableList(), true)
    }

    /**
     * Update navigator bar.
     */
    private fun updateNavigator(first: Boolean = false) {
        val locale = if (first) Locale.getDefault() else calendarDay.locale ?: Locale.getDefault()

        val year = currentDate.year
        val monthName = currentDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val day = currentDate.dayOfMonth
        val week = currentDate.plusWeeks(0L).get(WeekFields.of(locale).weekOfWeekBasedYear())
        val dayOfWeek = currentDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

        val pageTitle = getString(R.string.calendar_day_title).format("$year $monthName $day")
        toolBar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        binding.buttonYear.text = "$year"
        binding.buttonMonth.text = monthName
        binding.buttonDay.text = "$day"
        binding.buttonWeek.text = "W$week"
        binding.buttonDayOfWeek.text = dayOfWeek
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
}

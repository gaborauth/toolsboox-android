package com.toolsboox.plugin.calendar.ui

import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarMonthBinding
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.Calendar
import com.toolsboox.plugin.calendar.da.CalendarMonth
import com.toolsboox.plugin.calendar.ot.CalendarExtendedMonthCreator
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
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
 * Calendar extended month view fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarExtendedMonthFragment @Inject constructor() : SurfaceFragment() {

    /**
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarExtendedMonthPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_calendar_month

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarMonthBinding

    /**
     * The current date.
     */
    private var currentDate: LocalDate = LocalDate.now()

    /**
     * The timer job.
     */
    private lateinit var timer: Job

    /**
     * The data class.
     */
    private lateinit var calendarMonth: CalendarMonth

    /**
     * SurfaceView provide method.
     *
     * @return the actual surfaceView
     */
    override fun provideSurfaceView(): SurfaceView = binding.surfaceView

    /**
     * Provide toolbar of drawing's bindings.
     *
     * @return the actual bindings of toolbar of drawings
     */
    override fun provideToolbarDrawing(): ToolbarDrawingBinding = binding.toolbarDrawing

    /**
     * Stroke changed callback.
     *
     * @param strokes the actual strokes
     */
    override fun onStrokeChanged(strokes: MutableList<Stroke>) {
        val year = currentDate.year
        val month = currentDate.monthValue
        val locale = calendarMonth.locale ?: Locale.getDefault()

        calendarMonth = CalendarMonth(year, month, locale, Calendar.listDeepCopy(strokes))
        calendarMonth.normalizeStrokes(getSurfaceSize().width(), getSurfaceSize().height(), 1404, 1872)

        presenter.save(this, binding, calendarMonth, currentDate)
    }

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarMonthBinding.bind(view)

        currentDate = LocalDate.of(LocalDate.now().year, LocalDate.now().monthValue, 1)
        arguments?.getString("year")?.toIntOrNull()?.let { year ->
            Timber.i("Set year to '$year' from parameter")
            currentDate = LocalDate.of(year, 1, 1)
            arguments?.getString("month")?.toIntOrNull()?.let { month ->
                Timber.i("Set year and month to '$year'/'$month' from parameter")
                currentDate = LocalDate.of(year, month, 1)
            }
        }
        calendarMonth = CalendarMonth(
            currentDate.year, currentDate.monthValue, Locale.getDefault(), mutableListOf()
        )

        binding.buttonPrev.setOnClickListener {
            currentDate = currentDate.minusMonths(1L)
            presenter.load(this, binding, currentDate, getSurfaceSize())
        }
        binding.buttonNext.setOnClickListener {
            currentDate = currentDate.plusMonths(1L)
            presenter.load(this, binding, currentDate, getSurfaceSize())
        }

        binding.buttonYearQuarter.setOnClickListener {
            CalendarNavigator.toQuarter(this, currentDate)
        }

        binding.buttonWeek1.setOnClickListener {
            CalendarNavigator.toWeek(this, currentDate.plusWeeks(0L), calendarMonth.locale)
        }
        binding.buttonWeek2.setOnClickListener {
            CalendarNavigator.toWeek(this, currentDate.plusWeeks(1L), calendarMonth.locale)
        }
        binding.buttonWeek3.setOnClickListener {
            CalendarNavigator.toWeek(this, currentDate.plusWeeks(2L), calendarMonth.locale)
        }
        binding.buttonWeek4.setOnClickListener {
            CalendarNavigator.toWeek(this, currentDate.plusWeeks(3L), calendarMonth.locale)
        }
        binding.buttonWeek5.setOnClickListener {
            CalendarNavigator.toWeek(this, currentDate.plusWeeks(4L), calendarMonth.locale)
        }
        binding.buttonWeek6.setOnClickListener {
            CalendarNavigator.toWeek(this, currentDate.plusWeeks(5L), calendarMonth.locale)
        }

        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)
            CalendarExtendedMonthCreator.onTouchEvent(
                view, motionEvent, gestureResult, this@CalendarExtendedMonthFragment, calendarMonth
            )
        }

        toolbar.toolbarPager.visibility = View.GONE
        updateNavigator(true)

        initializeSurface(true)
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        binding.templateImage.setImageBitmap(templateBitmap)
        binding.templateImage.postInvalidate()

        timer = GlobalScope.launch(Dispatchers.Main) {
            presenter.load(this@CalendarExtendedMonthFragment, binding, currentDate, getSurfaceSize())
        }
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        toolbar.toolbarPager.visibility = View.GONE
        timer.cancel()
    }

    /**
     * Reload the current page.
     */
    fun renderPage(calendarMonth: CalendarMonth) {
        this.calendarMonth = calendarMonth
        updateNavigator()

        CalendarExtendedMonthCreator.drawPage(this.requireContext(), templateCanvas, calendarMonth)
        binding.templateImage.postInvalidate()

        applyStrokes(calendarMonth.strokes.toMutableList(), true)
    }

    /**
     * Update navigator bar.
     */
    private fun updateNavigator(first: Boolean = false) {
        val locale = if (first) Locale.getDefault() else calendarMonth.locale ?: Locale.getDefault()

        val year = currentDate.year
        val month = currentDate.monthValue
        val quarter = (month - 1) / 3 + 1
        val monthName = currentDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

        val pageTitle = getString(R.string.calendar_month_title).format(monthName, year)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        binding.buttonYearQuarter.text = "$year Q$quarter"

        val weekOfYear = WeekFields.of(locale).weekOfWeekBasedYear()
        binding.buttonWeek1.text = "W${currentDate.plusWeeks(0L).get(weekOfYear)}"
        binding.buttonWeek2.text = "W${currentDate.plusWeeks(1L).get(weekOfYear)}"
        binding.buttonWeek3.text = "W${currentDate.plusWeeks(2L).get(weekOfYear)}"
        binding.buttonWeek4.text = "W${currentDate.plusWeeks(3L).get(weekOfYear)}"
        binding.buttonWeek5.text = "W${currentDate.plusWeeks(4L).get(weekOfYear)}"
        binding.buttonWeek6.text = "W${currentDate.plusWeeks(5L).get(weekOfYear)}"
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

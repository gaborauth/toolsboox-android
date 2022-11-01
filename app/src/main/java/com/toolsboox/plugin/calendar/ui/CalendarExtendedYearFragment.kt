package com.toolsboox.plugin.calendar.ui

import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarYearBinding
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.Calendar
import com.toolsboox.plugin.calendar.da.CalendarYear
import com.toolsboox.plugin.calendar.ot.CalendarExtendedYearCreator
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import com.toolsboox.ui.plugin.SurfaceFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.util.*
import javax.inject.Inject


/**
 * Calendar extended year view fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarExtendedYearFragment @Inject constructor() : SurfaceFragment() {

    /**
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarExtendedYearPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_calendar_year

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarYearBinding

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
    private lateinit var calendarYear: CalendarYear

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
        val locale = calendarYear.locale ?: Locale.getDefault()

        calendarYear = CalendarYear(year, locale, Calendar.listDeepCopy(strokes))
        calendarYear.normalizeStrokes(getSurfaceSize().width(), getSurfaceSize().height(), 1404, 1872)

        presenter.save(this, binding, calendarYear, currentDate)
    }

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarYearBinding.bind(view)

        currentDate = LocalDate.ofYearDay(LocalDate.now().year, 1)
        arguments?.getString("year")?.toIntOrNull()?.let {
            Timber.i("Set year to '$it' from parameter")
            currentDate = LocalDate.ofYearDay(it, 1)
        }
        calendarYear = CalendarYear(
            currentDate.year, Locale.getDefault(), mutableListOf()
        )

        binding.buttonPrev.setOnClickListener {
            currentDate = currentDate.minusYears(1L)
            presenter.load(this, binding, currentDate, getSurfaceSize())
        }
        binding.buttonNext.setOnClickListener {
            currentDate = currentDate.plusYears(1L)
            presenter.load(this, binding, currentDate, getSurfaceSize())
        }

        binding.buttonYear.setOnClickListener {
            CalendarNavigator.toYear(this, currentDate)
        }

        binding.buttonQ1.setOnClickListener {
            CalendarNavigator.toQuarter(this, currentDate.plusMonths(0L))
        }
        binding.buttonQ2.setOnClickListener {
            CalendarNavigator.toQuarter(this, currentDate.plusMonths(3L))
        }
        binding.buttonQ3.setOnClickListener {
            CalendarNavigator.toQuarter(this, currentDate.plusMonths(6L))
        }
        binding.buttonQ4.setOnClickListener {
            CalendarNavigator.toQuarter(this, currentDate.plusMonths(9L))
        }

        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)
            CalendarExtendedYearCreator.onTouchEvent(
                view, motionEvent, gestureResult, this@CalendarExtendedYearFragment, calendarYear
            )
        }

        toolbar.toolbarPager.visibility = View.GONE
        updateNavigator()

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
            presenter.load(this@CalendarExtendedYearFragment, binding, currentDate, getSurfaceSize())
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
    fun renderPage(calendarYear: CalendarYear) {
        this.calendarYear = calendarYear
        updateNavigator()

        CalendarExtendedYearCreator.drawPage(this.requireContext(), templateCanvas, calendarYear)
        binding.templateImage.postInvalidate()

        applyStrokes(calendarYear.strokes.toMutableList(), true)
    }

    /**
     * Update navigator bar.
     */
    private fun updateNavigator() {
        val year = currentDate.year

        val pageTitle = getString(R.string.calendar_year_title).format(year)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        binding.buttonYear.text = "$year"
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

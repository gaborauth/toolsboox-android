package com.toolsboox.plugin.calendar.ui

import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarQuarterBinding
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.Calendar
import com.toolsboox.plugin.calendar.da.CalendarQuarter
import com.toolsboox.plugin.calendar.ot.CalendarExtendedQuarterCreator
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
import java.util.*
import javax.inject.Inject

/**
 * Calendar extended quarter view fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarExtendedQuarterFragment @Inject constructor() : SurfaceFragment() {

    /**
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarExtendedQuarterPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_calendar_quarter

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarQuarterBinding

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
    private lateinit var calendarQuarter: CalendarQuarter

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
        val quarter = (currentDate.monthValue - 1) / 3 + 1
        val locale = calendarQuarter.locale ?: Locale.getDefault()

        calendarQuarter = CalendarQuarter(year, quarter, locale, Calendar.listDeepCopy(strokes))
        calendarQuarter.normalizeStrokes(getSurfaceSize().width(), getSurfaceSize().height(), 1404, 1872)

        presenter.save(this, binding, calendarQuarter, currentDate)
    }

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarQuarterBinding.bind(view)

        currentDate = LocalDate.ofYearDay(LocalDate.now().year, 1)
        arguments?.getString("year")?.toIntOrNull()?.let { year ->
            Timber.i("Set year to '$year' from parameter")
            currentDate = LocalDate.of(year, 1, 1)
            arguments?.getString("quarter")?.toIntOrNull()?.let { quarter ->
                Timber.i("Set year and quarter to '$year'/'$quarter' from parameter")
                currentDate = LocalDate.of(year, (quarter - 1) * 3 + 1, 1)
            }
        }
        calendarQuarter = CalendarQuarter(
            currentDate.year, (currentDate.monthValue - 1 / 3) + 1, Locale.getDefault(), mutableListOf()
        )

        binding.buttonPrev.setOnClickListener {
            currentDate = currentDate.minusMonths(3L)
            presenter.load(this, binding, currentDate, getSurfaceSize())
        }
        binding.buttonNext.setOnClickListener {
            currentDate = currentDate.plusMonths(3L)
            presenter.load(this, binding, currentDate, getSurfaceSize())
        }

        binding.buttonYear.setOnClickListener {
            CalendarNavigator.toYear(this, currentDate)
        }

        binding.buttonMonth1.setOnClickListener {
            CalendarNavigator.toMonth(this, currentDate.plusMonths(0L))
        }
        binding.buttonMonth2.setOnClickListener {
            CalendarNavigator.toMonth(this, currentDate.plusMonths(1L))
        }
        binding.buttonMonth3.setOnClickListener {
            CalendarNavigator.toMonth(this, currentDate.plusMonths(2L))
        }

        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)
            CalendarExtendedQuarterCreator.onTouchEvent(
                view, motionEvent, gestureResult, this@CalendarExtendedQuarterFragment, calendarQuarter
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
            presenter.load(this@CalendarExtendedQuarterFragment, binding, currentDate, getSurfaceSize())
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
    fun renderPage(calendarQuarter: CalendarQuarter) {
        this.calendarQuarter = calendarQuarter
        updateNavigator()

        CalendarExtendedQuarterCreator.drawPage(this.requireContext(), templateCanvas, calendarQuarter)
        binding.templateImage.postInvalidate()

        applyStrokes(calendarQuarter.strokes.toMutableList(), true)
    }

    /**
     * Update navigator bar.
     */
    private fun updateNavigator() {
        val year = currentDate.year
        val quarter = (currentDate.monthValue - 1) / 3 + 1

        val pageTitle = getString(R.string.calendar_quarter_title).format(quarter, year)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        binding.buttonYear.text = "$year"

        currentDate.plusMonths(0L).month.getDisplayName(TextStyle.FULL, Locale.getDefault()).let {
            binding.buttonMonth1.text = it
        }
        currentDate.plusMonths(1L).month.getDisplayName(TextStyle.FULL, Locale.getDefault()).let {
            binding.buttonMonth2.text = it
        }
        currentDate.plusMonths(2L).month.getDisplayName(TextStyle.FULL, Locale.getDefault()).let {
            binding.buttonMonth3.text = it
        }
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

package com.toolsboox.plugin.calendar.ui

import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.plugin.calendar.da.Calendar
import com.toolsboox.plugin.calendar.da.CalendarWeek
import com.toolsboox.plugin.calendar.ot.CalendarWeekNavigator
import com.toolsboox.plugin.calendar.ot.CalendarWeekPage
import com.toolsboox.plugin.calendar.ot.CalendarWeekPageNotes
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import com.toolsboox.ui.plugin.SurfaceFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*
import javax.inject.Inject

/**
 * Calendar week view fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarWeekFragment @Inject constructor() : SurfaceFragment() {

    /**
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarWeekPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_calendar

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarBinding

    /**
     * The current date.
     */
    private var currentDate: LocalDate = LocalDate.now()

    /**
     * Flag of notes view.
     */
    private var notes: Boolean = false

    /**
     * The timer job.
     */
    private lateinit var timer: Job

    /**
     * The data class.
     */
    private lateinit var calendarWeek: CalendarWeek

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
        val locale = calendarWeek.locale
        val weekOfYear = WeekFields.of(locale).weekOfWeekBasedYear()
        val weekOfYearValue = currentDate.plusWeeks(0L).get(weekOfYear)

        calendarWeek =
            if (notes) {
                CalendarWeek(
                    year, weekOfYearValue, locale,
                    Calendar.listDeepCopy(calendarWeek.strokes), Calendar.listDeepCopy(strokes)
                )
            } else {
                CalendarWeek(
                    year, weekOfYearValue, locale,
                    Calendar.listDeepCopy(strokes), Calendar.listDeepCopy(calendarWeek.notesStrokes)
                )
            }

        presenter.save(this, binding, calendarWeek, currentDate, getSurfaceSize())
    }

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarBinding.bind(view)

        currentDate = LocalDate.of(LocalDate.now().year, LocalDate.now().monthValue, 1)
        arguments?.getString("year")?.toIntOrNull()?.let { year ->
            Timber.i("Set year to '$year' from parameter")
            currentDate = LocalDate.of(year, 1, 1)
            arguments?.getString("weekOfYear")?.toIntOrNull()?.let { weekOfYear ->
                val weekFields = WeekFields.of(Locale.getDefault())
                Timber.i("Set year and week to '$year'/'$weekOfYear' from parameter")
                currentDate = LocalDate.ofYearDay(year, 1)
                    .with(weekFields.weekOfYear(), weekOfYear.toLong())
                    .with(weekFields.dayOfWeek(), 1)
            }
        }
        notes = arguments?.getString("notes")?.toBoolean() ?: false

        val weekOfWeekBasedYear = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
        calendarWeek = CalendarWeek(currentDate.year, currentDate.get(weekOfWeekBasedYear))

        binding.navigatorImageView.setOnTouchListener { view, motionEvent ->
            CalendarWeekNavigator.onTouchEvent(view, motionEvent, this@CalendarWeekFragment, calendarWeek)
        }

        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)

            if (notes) {
                CalendarWeekPageNotes.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarWeekFragment, calendarWeek
                )
            } else {
                CalendarWeekPage.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarWeekFragment, calendarWeek
                )
            }
        }

        toolbar.toolbarPager.visibility = View.GONE

        initializeSurface(true)
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        binding.templateImageView.setImageBitmap(templateBitmap)
        binding.templateImageView.postInvalidate()

        binding.navigatorImageView.setImageBitmap(navigatorBitmap)
        binding.navigatorImageView.postInvalidate()

        updateNavigator(true)

        timer = GlobalScope.launch(Dispatchers.Main) {
            presenter.load(this@CalendarWeekFragment, binding, currentDate, getSurfaceSize())
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
    fun renderPage(calendarWeek: CalendarWeek) {
        this.calendarWeek = calendarWeek
        updateNavigator()

        if (notes) {
            CalendarWeekPageNotes.drawPage(this.requireContext(), templateCanvas, calendarWeek)
            applyStrokes(calendarWeek.notesStrokes.toMutableList(), true)
        } else {
            CalendarWeekPage.drawPage(this.requireContext(), templateCanvas, calendarWeek)
            applyStrokes(calendarWeek.strokes.toMutableList(), true)
        }

        binding.templateImageView.postInvalidate()
    }

    /**
     * Update navigator bar.
     *
     * @param first flag of first start
     */
    private fun updateNavigator(first: Boolean = false) {
        if (first) return

        val locale = calendarWeek.locale
        val year = currentDate.year
        val weekOfYearField = WeekFields.of(locale).weekOfWeekBasedYear()
        val weekOfYear = currentDate.get(weekOfYearField)

        val pageTitle = getString(R.string.calendar_week_title).format(year, weekOfYear)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        CalendarWeekNavigator.draw(this.requireContext(), navigatorCanvas, calendarWeek)
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

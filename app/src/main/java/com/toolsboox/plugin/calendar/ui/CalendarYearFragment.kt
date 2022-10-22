package com.toolsboox.plugin.calendar.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarYearBinding
import com.toolsboox.plugin.calendar.da.Calendar
import com.toolsboox.plugin.calendar.da.CalendarYear
import com.toolsboox.plugin.calendar.ot.CalendarYearCreator
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
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject


/**
 * Calendar year view fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarYearFragment @Inject constructor() : SurfaceFragment() {

    /**
     * The injected router.
     */
    @Inject
    lateinit var router: Router

    /**
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarYearPresenter

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
    private lateinit var calendarYear: CalendarYear

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

        currentDate = LocalDate.now()
        parameters["year"]?.toIntOrNull()?.let {
            Timber.i("Set year to '$it' from parameter")
            currentDate = LocalDate.ofYearDay(it, 1)
        }

        binding.buttonPrev.setOnClickListener {
            currentDate = currentDate.minusYears(1L)
            presenter.load(this, binding, currentDate, getSurfaceSize())
        }
        binding.buttonNext.setOnClickListener {
            currentDate = currentDate.plusYears(1L)
            presenter.load(this, binding, currentDate, getSurfaceSize())
        }

        binding.buttonYear.setOnClickListener {
            val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
            Timber.i("Route to the '$year' yearly calendar")
            router.dispatch("/calendar/year/$year", false)
        }

        binding.buttonQ1.setOnClickListener {
            val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
            Timber.i("Route to the '$year'/'1' quarterly calendar")
            router.dispatch("/calendar/quarter/$year/1", false)
        }
        binding.buttonQ2.setOnClickListener {
            val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
            Timber.i("Route to the '$year'/'2' quarterly calendar")
            router.dispatch("/calendar/quarter/$year/2", false)
        }
        binding.buttonQ3.setOnClickListener {
            val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
            Timber.i("Route to the '$year'/'3' quarterly calendar")
            router.dispatch("/calendar/quarter/$year/3", false)
        }
        binding.buttonQ4.setOnClickListener {
            val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
            Timber.i("Route to the '$year'/'4' quarterly calendar")
            router.dispatch("/calendar/quarter/$year/4", false)
        }

        toolBar.toolbarPager.visibility = View.GONE
        updateNavigator()

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
            presenter.load(this@CalendarYearFragment, binding, currentDate, getSurfaceSize())
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
    fun renderPage(calendarYear: CalendarYear) {
        this.calendarYear = calendarYear
        updateNavigator()

        CalendarYearCreator.drawPage(this.requireContext(), templateCanvas, calendarYear)
        binding.templateImage.postInvalidate()

        applyStrokes(calendarYear.strokes.toMutableList(), true)
    }

    /**
     * Update navigator bar.
     */
    private fun updateNavigator() {
        val year = currentDate.year

        val pageTitle = getString(R.string.calendar_year_title).format(year)
        toolBar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

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

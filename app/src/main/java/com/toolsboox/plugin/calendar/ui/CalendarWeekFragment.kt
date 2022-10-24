package com.toolsboox.plugin.calendar.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarWeekBinding
import com.toolsboox.plugin.calendar.da.Calendar
import com.toolsboox.plugin.calendar.da.CalendarWeek
import com.toolsboox.plugin.calendar.ot.CalendarWeekCreator
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
    override val view = R.layout.fragment_calendar_week

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarWeekBinding

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
    private lateinit var calendarWeek: CalendarWeek

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
        val locale = calendarWeek.locale ?: Locale.getDefault()
        val weekOfYear = WeekFields.of(locale).weekOfWeekBasedYear()
        val weekOfYearValue = currentDate.plusWeeks(0L).get(weekOfYear)

        calendarWeek = CalendarWeek(year, weekOfYearValue, locale, Calendar.listDeepCopy(strokes))
        calendarWeek.normalizeStrokes(getSurfaceSize().width(), getSurfaceSize().height(), 1404, 1872)

        presenter.save(this, binding, calendarWeek, currentDate)
    }

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarWeekBinding.bind(view)

        currentDate = LocalDate.now()
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

        binding.buttonPrev.setOnClickListener {
            currentDate = currentDate.minusWeeks(1L)
            presenter.load(this, binding, currentDate, getSurfaceSize(), calendarWeek.locale ?: Locale.getDefault())
        }
        binding.buttonNext.setOnClickListener {
            currentDate = currentDate.plusWeeks(1L)
            presenter.load(this, binding, currentDate, getSurfaceSize(), calendarWeek.locale ?: Locale.getDefault())
        }

        binding.buttonMonth.setOnClickListener {
            val year = currentDate.year
            val month = currentDate.monthValue
            Timber.i("Route to the '$year'/'$month' monthly calendar")
            val bundle = bundleOf()
            bundle.putString("year", "$year")
            bundle.putString("month", "$month")
            findNavController().navigate(R.id.action_to_calendar_month, bundle)
        }

        binding.buttonDayOfWeek1.setOnClickListener {
            val year = currentDate.plusDays(0L).year
            val month = currentDate.plusDays(0L).monthValue
            val day = currentDate.plusDays(0L).dayOfMonth
            Timber.i("Route to the '$year'/'$month'/'$day' daily calendar")
            val bundle = bundleOf()
            bundle.putString("year", "$year")
            bundle.putString("month", "$month")
            bundle.putString("day", "$day")
            findNavController().navigate(R.id.action_to_calendar_day, bundle)
        }
        binding.buttonDayOfWeek2.setOnClickListener {
            val year = currentDate.plusDays(1L).year
            val month = currentDate.plusDays(1L).monthValue
            val day = currentDate.plusDays(1L).dayOfMonth
            Timber.i("Route to the '$year'/'$month'/'$day' daily calendar")
            val bundle = bundleOf()
            bundle.putString("year", "$year")
            bundle.putString("month", "$month")
            bundle.putString("day", "$day")
            findNavController().navigate(R.id.action_to_calendar_day, bundle)
        }
        binding.buttonDayOfWeek3.setOnClickListener {
            val year = currentDate.plusDays(2L).year
            val month = currentDate.plusDays(2L).monthValue
            val day = currentDate.plusDays(2L).dayOfMonth
            Timber.i("Route to the '$year'/'$month'/'$day' daily calendar")
            val bundle = bundleOf()
            bundle.putString("year", "$year")
            bundle.putString("month", "$month")
            bundle.putString("day", "$day")
            findNavController().navigate(R.id.action_to_calendar_day, bundle)
        }
        binding.buttonDayOfWeek4.setOnClickListener {
            val year = currentDate.plusDays(3L).year
            val month = currentDate.plusDays(3L).monthValue
            val day = currentDate.plusDays(3L).dayOfMonth
            Timber.i("Route to the '$year'/'$month'/'$day' daily calendar")
            val bundle = bundleOf()
            bundle.putString("year", "$year")
            bundle.putString("month", "$month")
            bundle.putString("day", "$day")
            findNavController().navigate(R.id.action_to_calendar_day, bundle)
        }
        binding.buttonDayOfWeek5.setOnClickListener {
            val year = currentDate.plusDays(4L).year
            val month = currentDate.plusDays(4L).monthValue
            val day = currentDate.plusDays(4L).dayOfMonth
            Timber.i("Route to the '$year'/'$month'/'$day' daily calendar")
            val bundle = bundleOf()
            bundle.putString("year", "$year")
            bundle.putString("month", "$month")
            bundle.putString("day", "$day")
            findNavController().navigate(R.id.action_to_calendar_day, bundle)
        }
        binding.buttonDayOfWeek6.setOnClickListener {
            val year = currentDate.plusDays(5L).year
            val month = currentDate.plusDays(5L).monthValue
            val day = currentDate.plusDays(5L).dayOfMonth
            Timber.i("Route to the '$year'/'$month'/'$day' daily calendar")
            val bundle = bundleOf()
            bundle.putString("year", "$year")
            bundle.putString("month", "$month")
            bundle.putString("day", "$day")
            findNavController().navigate(R.id.action_to_calendar_day, bundle)
        }
        binding.buttonDayOfWeek7.setOnClickListener {
            val year = currentDate.plusDays(6L).year
            val month = currentDate.plusDays(6L).monthValue
            val day = currentDate.plusDays(6L).dayOfMonth
            Timber.i("Route to the '$year'/'$month'/'$day' daily calendar")
            val bundle = bundleOf()
            bundle.putString("year", "$year")
            bundle.putString("month", "$month")
            bundle.putString("day", "$day")
            findNavController().navigate(R.id.action_to_calendar_day, bundle)
        }

        toolbar.toolbarPager.visibility = View.GONE
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
            presenter.load(this@CalendarWeekFragment, binding, currentDate, getSurfaceSize(), Locale.getDefault())
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

        CalendarWeekCreator.drawPage(this.requireContext(), templateCanvas, calendarWeek)
        binding.templateImage.postInvalidate()

        applyStrokes(calendarWeek.strokes.toMutableList(), true)
    }

    /**
     * Update navigator bar.
     */
    private fun updateNavigator(first: Boolean = false) {
        val locale = if (first) Locale.getDefault() else calendarWeek.locale ?: Locale.getDefault()

        val year = currentDate.year
        val monthName = currentDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val weekOfYear = WeekFields.of(locale).weekOfWeekBasedYear()
        val week = "W${currentDate.plusWeeks(0L).get(weekOfYear)}"

        val pageTitle = getString(R.string.calendar_week_title).format(week, year)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        binding.buttonMonth.text = "$year $monthName"

        currentDate.plusDays(0L).dayOfWeek.getDisplayName(TextStyle.NARROW, locale).let {
            binding.buttonDayOfWeek1.text = it
        }
        currentDate.plusDays(1L).dayOfWeek.getDisplayName(TextStyle.NARROW, locale).let {
            binding.buttonDayOfWeek2.text = it
        }
        currentDate.plusDays(2L).dayOfWeek.getDisplayName(TextStyle.NARROW, locale).let {
            binding.buttonDayOfWeek3.text = it
        }
        currentDate.plusDays(3L).dayOfWeek.getDisplayName(TextStyle.NARROW, locale).let {
            binding.buttonDayOfWeek4.text = it
        }
        currentDate.plusDays(4L).dayOfWeek.getDisplayName(TextStyle.NARROW, locale).let {
            binding.buttonDayOfWeek5.text = it
        }
        currentDate.plusDays(5L).dayOfWeek.getDisplayName(TextStyle.NARROW, locale).let {
            binding.buttonDayOfWeek6.text = it
        }
        currentDate.plusDays(6L).dayOfWeek.getDisplayName(TextStyle.NARROW, locale).let {
            binding.buttonDayOfWeek7.text = it
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

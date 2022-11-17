package com.toolsboox.plugin.calendar.ui

import android.content.SharedPreferences
import android.icu.text.DateFormat
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.plugin.calendar.da.Calendar
import com.toolsboox.plugin.calendar.da.CalendarMonth
import com.toolsboox.plugin.calendar.da.CalendarPattern
import com.toolsboox.plugin.calendar.ot.CalendarMonthNavigator
import com.toolsboox.plugin.calendar.ot.CalendarMonthPage
import com.toolsboox.plugin.calendar.ot.CalendarMonthPageNotes
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import com.toolsboox.ui.plugin.SurfaceFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

/**
 * Calendar month view fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarMonthFragment @Inject constructor() : SurfaceFragment() {

    /**
     * The shared preferences.
     */
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    /**
     * The Firebase analytics.
     */
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The presenter of the pattern fragment.
     */
    @Inject
    lateinit var patternPresenter: CalendarPatternPresenter

    /**
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarMonthPresenter

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
     * The current locale.
     */
    private var locale: Locale = Locale.getDefault()

    /**
     * The timer job.
     */
    private lateinit var timer: Job

    /**
     * The data class.
     */
    private lateinit var calendarMonth: CalendarMonth

    /**
     * The pattern data class.
     */
    private var calendarPattern: CalendarPattern? = null

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
        val locale = calendarMonth.locale

        calendarMonth =
            if (notes) {
                CalendarMonth(
                    year, month, locale,
                    Calendar.listDeepCopy(calendarMonth.strokes), Calendar.listDeepCopy(strokes)
                )
            } else {
                CalendarMonth(
                    year, month, locale,
                    Calendar.listDeepCopy(strokes), Calendar.listDeepCopy(calendarMonth.notesStrokes)
                )
            }

        presenter.save(this, binding, calendarMonth, currentDate, getSurfaceSize())
        patternPresenter.save(this, binding, calendarPattern, currentDate)
    }

    /**
     * Calendar pattern loaded.
     *
     * @param calendarPattern the calendar pattern
     */
    override fun onCalendarPatternLoaded(calendarPattern: CalendarPattern) {
        this.calendarPattern = calendarPattern
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

        val savedLocaleLanguageTag = sharedPreferences.getString("calendarLocale", Locale.getDefault().toLanguageTag())
        if (savedLocaleLanguageTag != null) {
            if (Locale.forLanguageTag(savedLocaleLanguageTag).toLanguageTag() == savedLocaleLanguageTag) {
                locale = Locale.forLanguageTag(savedLocaleLanguageTag)
                Timber.i("Locale switched to: ${locale.toLanguageTag()}")
            }
        }

        currentDate = LocalDate.of(LocalDate.now().year, LocalDate.now().monthValue, 1)
        arguments?.getString("year")?.toIntOrNull()?.let { year ->
            Timber.i("Set year to '$year' from parameter")
            currentDate = LocalDate.of(year, 1, 1)
            arguments?.getString("month")?.toIntOrNull()?.let { month ->
                Timber.i("Set year and month to '$year'/'$month' from parameter")
                currentDate = LocalDate.of(year, month, 1)
            }
        }
        notes = arguments?.getString("notes")?.toBoolean() ?: false

        calendarMonth = CalendarMonth(currentDate.year, currentDate.monthValue, locale)

        binding.navigatorImageView.setOnTouchListener { view, motionEvent ->
            CalendarMonthNavigator.onTouchEvent(view, motionEvent, this@CalendarMonthFragment, calendarMonth)
        }

        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)

            if (notes) {
                CalendarMonthPageNotes.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarMonthFragment, calendarMonth
                )
            } else {
                CalendarMonthPage.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarMonthFragment, calendarMonth
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
        binding.navigatorImageView.setImageBitmap(navigatorBitmap)
        updateNavigator(true)

        timer = GlobalScope.launch(Dispatchers.Main) {
            patternPresenter.load(this@CalendarMonthFragment, binding, currentDate, locale)
            presenter.load(this@CalendarMonthFragment, binding, currentDate, getSurfaceSize(), locale)
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

        if (notes) {
            CalendarMonthPageNotes.drawPage(this.requireContext(), templateCanvas, calendarMonth)
            applyStrokes(calendarMonth.notesStrokes.toMutableList(), true)
        } else {
            CalendarMonthPage.drawPage(this.requireContext(), templateCanvas, calendarMonth)
            applyStrokes(calendarMonth.strokes.toMutableList(), true)
        }
    }

    /**
     * Update navigator bar.
     *
     * @param first flag of first start
     */
    private fun updateNavigator(first: Boolean = false) {
        if (first) return

        val dateFormat = DateFormat.getPatternInstance(DateFormat.YEAR_MONTH)
        val titleDate = dateFormat.format(Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))

        val pageTitle = getString(R.string.calendar_month_title).format(titleDate)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        CalendarMonthNavigator.draw(this.requireContext(), navigatorCanvas, calendarMonth)

        firebaseAnalytics.logEvent("calendarMonth") {
            param("currentDate", currentDate.format(DateTimeFormatter.ISO_DATE))
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

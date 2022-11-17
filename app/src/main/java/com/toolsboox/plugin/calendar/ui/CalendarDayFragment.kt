package com.toolsboox.plugin.calendar.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.plugin.calendar.da.Calendar
import com.toolsboox.plugin.calendar.da.CalendarDay
import com.toolsboox.plugin.calendar.da.CalendarPattern
import com.toolsboox.plugin.calendar.da.GoogleCalendarEvent
import com.toolsboox.plugin.calendar.ot.CalendarDayNavigator
import com.toolsboox.plugin.calendar.ot.CalendarDayPage
import com.toolsboox.plugin.calendar.ot.CalendarDayPageNotes
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import com.toolsboox.ui.plugin.SurfaceFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    lateinit var presenter: CalendarDayPresenter

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
    private lateinit var calendarDay: CalendarDay

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
        val day = currentDate.dayOfMonth
        val locale = calendarDay.locale

        calendarDay =
            if (notes) {
                CalendarDay(
                    year, month, day, locale,
                    Calendar.listDeepCopy(calendarDay.strokes), Calendar.listDeepCopy(strokes)
                )
            } else {
                CalendarDay(
                    year, month, day, locale,
                    Calendar.listDeepCopy(strokes), Calendar.listDeepCopy(calendarDay.notesStrokes)
                )
            }

        presenter.save(this, binding, calendarDay, currentDate, getSurfaceSize())
        patternPresenter.save(this, binding, calendarPattern, currentDate)
    }

    /**
     * Calendar pattern loaded.
     *
     * @param calendarPattern the calendar pattern
     */
    override fun onCalendarPatternLoaded(calendarPattern: CalendarPattern) {
        this.calendarPattern = calendarPattern
        Timber.e("$calendarPattern")
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

        currentDate = LocalDate.now()
        arguments?.getString("year")?.toIntOrNull()?.let { year ->
            Timber.i("Set year to '$year' from parameter")
            currentDate = LocalDate.of(year, 1, 1)
            arguments?.getString("month")?.toIntOrNull()?.let { month ->
                Timber.i("Set year and month to '$year'/'$month' from parameter")
                currentDate = LocalDate.of(year, month, 1)
                arguments?.getString("day")?.toIntOrNull()?.let { day ->
                    Timber.i("Set year, month and day to '$year'/'$month'/'$day' from parameter")
                    currentDate = LocalDate.of(year, month, day)
                }
            }
        }
        notes = arguments?.getString("notes")?.toBoolean() ?: false

        calendarDay = CalendarDay(currentDate.year, currentDate.monthValue, currentDate.dayOfMonth, locale)

        binding.navigatorImageView.setOnTouchListener { view, motionEvent ->
            CalendarDayNavigator.onTouchEvent(view, motionEvent, this@CalendarDayFragment, calendarDay)
        }

        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)

            if (notes)
                CalendarDayPageNotes.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarDayFragment, calendarDay
                )
            else
                CalendarDayPage.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarDayFragment, calendarDay
                )
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
            patternPresenter.load(this@CalendarDayFragment, binding, currentDate, locale)
            presenter.load(this@CalendarDayFragment, binding, currentDate, getSurfaceSize(), locale)
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
     *
     * @param calendarDay the data class
     * @param googleCalendarEvents the Google Calender events
     */
    fun renderPage(calendarDay: CalendarDay, googleCalendarEvents: List<GoogleCalendarEvent>) {
        this.calendarDay = calendarDay
        updateNavigator()

        if (notes) {
            CalendarDayPageNotes.drawPage(this.requireContext(), templateCanvas, calendarDay)
            applyStrokes(calendarDay.notesStrokes.toMutableList(), true)
        } else {
            CalendarDayPage.drawPage(this.requireContext(), templateCanvas, calendarDay, googleCalendarEvents)
            applyStrokes(calendarDay.strokes.toMutableList(), true)
        }
    }

    /**
     * Update navigator bar.
     *
     * @param first flag of first start
     */
    private fun updateNavigator(first: Boolean = false) {
        if (first) return

        val titleDate = currentDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))

        val pageTitle = getString(R.string.calendar_day_title).format(titleDate)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        CalendarDayNavigator.draw(this.requireContext(), navigatorCanvas, calendarDay)

        firebaseAnalytics.logEvent("calendarDay") {
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

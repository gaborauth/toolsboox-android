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
import com.toolsboox.plugin.calendar.da.CalendarPattern
import com.toolsboox.plugin.calendar.da.CalendarQuarter
import com.toolsboox.plugin.calendar.ot.CalendarQuarterNavigator
import com.toolsboox.plugin.calendar.ot.CalendarQuarterPage
import com.toolsboox.plugin.calendar.ot.CalendarQuarterPageNotes
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
 * Calendar quarter view fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarQuarterFragment @Inject constructor() : SurfaceFragment() {

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
    lateinit var presenter: CalendarQuarterPresenter

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
    private lateinit var calendarQuarter: CalendarQuarter

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
        val quarter = (currentDate.monthValue - 1) / 3 + 1
        val locale = calendarQuarter.locale

        calendarQuarter =
            if (notes) {
                CalendarQuarter(
                    year, quarter, locale,
                    Calendar.listDeepCopy(calendarQuarter.strokes), Calendar.listDeepCopy(strokes)
                )
            } else {
                CalendarQuarter(
                    year, quarter, locale,
                    Calendar.listDeepCopy(strokes), Calendar.listDeepCopy(calendarQuarter.notesStrokes)
                )
            }

        presenter.save(this, binding, calendarQuarter, currentDate, getSurfaceSize())
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

        currentDate = LocalDate.ofYearDay(LocalDate.now().year, 1)
        arguments?.getString("year")?.toIntOrNull()?.let { year ->
            Timber.i("Set year to '$year' from parameter")
            currentDate = LocalDate.of(year, 1, 1)
            arguments?.getString("quarter")?.toIntOrNull()?.let { quarter ->
                Timber.i("Set year and quarter to '$year'/'$quarter' from parameter")
                currentDate = LocalDate.of(year, (quarter - 1) * 3 + 1, 1)
            }
        }
        notes = arguments?.getString("notes")?.toBoolean() ?: false

        calendarQuarter = CalendarQuarter(currentDate.year, (currentDate.monthValue - 1 / 3) + 1, locale)

        binding.navigatorImageView.setOnTouchListener { view, motionEvent ->
            CalendarQuarterNavigator.onTouchEvent(view, motionEvent, this@CalendarQuarterFragment, calendarQuarter)
        }

        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)

            if (notes) {
                CalendarQuarterPageNotes.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarQuarterFragment, calendarQuarter
                )
            } else {
                CalendarQuarterPage.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarQuarterFragment, calendarQuarter
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
            patternPresenter.load(this@CalendarQuarterFragment, binding, currentDate, locale)
            presenter.load(this@CalendarQuarterFragment, binding, currentDate, getSurfaceSize(), locale)
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

        if (notes) {
            CalendarQuarterPageNotes.drawPage(this.requireContext(), templateCanvas, calendarQuarter)
            applyStrokes(calendarQuarter.notesStrokes.toMutableList(), true)
        } else {
            CalendarQuarterPage.drawPage(this.requireContext(), templateCanvas, calendarQuarter)
            applyStrokes(calendarQuarter.strokes.toMutableList(), true)
        }
    }

    /**
     * Update navigator bar.
     *
     * @param first flag of first start
     */
    private fun updateNavigator(first: Boolean = false) {
        if (first) return

        val dateFormat = DateFormat.getPatternInstance(DateFormat.YEAR_QUARTER)
        val titleDate = dateFormat.format(Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))

        val pageTitle = getString(R.string.calendar_quarter_title).format(titleDate)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        CalendarQuarterNavigator.draw(this.requireContext(), navigatorCanvas, calendarQuarter)

        firebaseAnalytics.logEvent("calendarQuarter") {
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

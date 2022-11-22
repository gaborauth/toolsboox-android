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
import com.toolsboox.plugin.calendar.da.v1.Calendar
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v1.CalendarYear
import com.toolsboox.plugin.calendar.ot.CalendarYearNavigator
import com.toolsboox.plugin.calendar.ot.CalendarYearPage
import com.toolsboox.plugin.calendar.ot.CalendarYearPageNotes
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
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarYearPresenter

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
    private lateinit var calendarYear: CalendarYear

    /**
     * The pattern data class.
     */
    private lateinit var calendarPattern: CalendarPattern

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
        val locale = calendarYear.locale

        calendarYear =
            if (notes) {
                CalendarYear(
                    year, locale,
                    Calendar.listDeepCopy(calendarYear.strokes), Calendar.listDeepCopy(strokes)
                )
            } else {
                CalendarYear(
                    year, locale,
                    Calendar.listDeepCopy(strokes), Calendar.listDeepCopy(calendarYear.notesStrokes)
                )
            }

        val pages = if (calendarYear.strokes.isEmpty()) 0 else 1
        val notes = if (calendarYear.notesStrokes.isEmpty()) 0 else 1
        calendarPattern.updateYear(pages, notes)

        presenter.save(this, binding, calendarYear, calendarPattern, currentDate, getSurfaceSize())
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
        arguments?.getString("year")?.toIntOrNull()?.let {
            Timber.i("Set year to '$it' from parameter")
            currentDate = LocalDate.ofYearDay(it, 1)
        }
        notes = arguments?.getString("notes")?.toBoolean() ?: false

        calendarYear = CalendarYear(currentDate.year, locale)

        binding.navigatorImageView.setOnTouchListener { view, motionEvent ->
            CalendarYearNavigator.onTouchEvent(view, motionEvent, this@CalendarYearFragment, calendarYear)
        }

        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)

            if (notes) {
                CalendarYearPageNotes.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarYearFragment, calendarYear
                )
            } else {
                CalendarYearPage.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarYearFragment, calendarYear
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
            presenter.load(this@CalendarYearFragment, binding, currentDate, getSurfaceSize(), locale)
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
     * @param calendarYear the data class
     * @param calendarPattern the pattern data class
     */
    fun renderPage(calendarYear: CalendarYear, calendarPattern: CalendarPattern) {
        this.calendarYear = calendarYear
        this.calendarPattern = calendarPattern
        updateNavigator()

        if (notes) {
            CalendarYearPageNotes.drawPage(this.requireContext(), templateCanvas, calendarYear, calendarPattern)
            applyStrokes(calendarYear.notesStrokes.toMutableList(), true)
        } else {
            CalendarYearPage.drawPage(this.requireContext(), templateCanvas, calendarYear, calendarPattern)
            applyStrokes(calendarYear.strokes.toMutableList(), true)
        }
    }

    /**
     * Update navigator bar.
     *
     * @param first flag of first start
     */
    private fun updateNavigator(first: Boolean = false) {
        if (first) return

        val year = currentDate.year

        val pageTitle = getString(R.string.calendar_year_title).format(year)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        CalendarYearNavigator.draw(this.requireContext(), navigatorCanvas, calendarYear, calendarPattern)

        firebaseAnalytics.logEvent("calendarYear") {
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

package com.toolsboox.plugin.calendar.ui

import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.toolsboox.R
import com.toolsboox.da.Stroke
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v2.CalendarYear
import com.toolsboox.plugin.calendar.ot.CalendarUtils
import com.toolsboox.plugin.calendar.ot.CalendarYearNavigator
import com.toolsboox.plugin.calendar.ot.CalendarYearPage
import com.toolsboox.plugin.calendar.ot.CalendarYearPageNotes
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
     * The Firebase analytics.
     */
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarYearPresenter

    // The Google Drive sync presenter.
    @Inject
    lateinit var syncPresenter: CalendarGoogleDriveSyncPresenter

    /**
     * The calendar utils.
     */
    @Inject
    lateinit var utils: CalendarUtils

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
     * Style of calendar view.
     */
    private var calendarStyle: String? = null

    /**
     * Page of notes view.
     */
    private var notePage: String? = null

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
        val normalizedStrokes = surfaceFrom(strokes)
        if (notePage != null) {
            calendarYear.noteStrokes[notePage!!] = normalizedStrokes
        } else {
            calendarYear.calendarStrokes[calendarStyle ?: CalendarYear.DEFAULT_STYLE] = normalizedStrokes
        }

        calendarPattern.updateYear(calendarYear)

        presenter.save(this, binding, calendarYear, calendarPattern, currentDate)
    }

    /**
     * On side switched event.
     */
    override fun onSideSwitched() {
        utils.updateToolbar(binding, true)

        if (notePage != null)
            CalendarNavigator.toYearNote(this, currentDate, notePage!!)
        else
            CalendarNavigator.toYearPage(this, currentDate, CalendarYear.DEFAULT_STYLE)
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
        calendarStyle = arguments?.getString("calendarStyle") ?: CalendarYear.DEFAULT_STYLE
        notePage = arguments?.getString("notePage")

        calendarYear = CalendarYear(currentDate.year, locale)

        binding.navigatorImageView.setOnTouchListener { view, motionEvent ->
            CalendarYearNavigator.onTouchEvent(view, motionEvent, this@CalendarYearFragment, calendarYear)
        }

        binding.surfaceView.setOnHoverListener { _, motionEvent ->
            return@setOnHoverListener callback(motionEvent, true)
        }
        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            if (callback(motionEvent, false)) return@setOnTouchListener true

            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)

            if (notePage != null) {
                CalendarYearPageNotes.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarYearFragment, calendarYear, notePage!!
                )
            } else {
                CalendarYearPage.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarYearFragment, calendarYear
                )
            }
        }

        toolbar.toolbarPager.visibility = View.GONE

        binding.toolbarDrawing.toolbarSwipeUp.setOnClickListener {
            if (notePage != null) {
                val page = notePage!!.toIntOrNull() ?: 0
                if (page == 0) {
                    CalendarNavigator.toYearPage(this, currentDate, CalendarYear.DEFAULT_STYLE)
                } else {
                    CalendarNavigator.toYearNote(this, currentDate, "${page - 1}")
                }
            }
        }
        binding.toolbarDrawing.toolbarSwipeDown.setOnClickListener {
            if (notePage != null) {
                val page = notePage!!.toIntOrNull() ?: 0
                CalendarNavigator.toYearNote(this, currentDate, "${page + 1}")
            } else {
                CalendarNavigator.toYearNote(this, currentDate, "0")
            }
        }

        utils.updateToolbar(binding)
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
            presenter.load(this@CalendarYearFragment, binding, currentDate, locale)
            syncPresenter.backgroundSync(this@CalendarYearFragment, UUID.randomUUID())
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

        if (notePage != null) {
            val noteTemplate = sharedPreferences.getInt("calendarNoteTemplate", 0)
            val noteStrokes = calendarYear.noteStrokes[notePage] ?: listOf()
            CalendarYearPageNotes.drawPage(this.requireContext(), templateCanvas, calendarYear, noteTemplate, notePage!!)
            applyStrokes(surfaceTo(noteStrokes), true)
        } else {
            val calendarStrokes = calendarYear.calendarStrokes[calendarStyle ?: CalendarYear.DEFAULT_STYLE] ?: listOf()
            CalendarYearPage.drawPage(this.requireContext(), templateCanvas, calendarYear, calendarPattern)
            applyStrokes(surfaceTo(calendarStrokes), true)
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
        toolbar.root.title = getString(R.string.calendar_main_title, pageTitle)

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

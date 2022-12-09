package com.toolsboox.plugin.calendar.ui

import android.content.SharedPreferences
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
import com.toolsboox.plugin.calendar.da.v2.CalendarMonth
import com.toolsboox.plugin.calendar.da.v2.CalendarWeek
import com.toolsboox.plugin.calendar.ot.CalendarUtils
import com.toolsboox.plugin.calendar.ot.CalendarWeekNavigator
import com.toolsboox.plugin.calendar.ot.CalendarWeekPage
import com.toolsboox.plugin.calendar.ot.CalendarWeekPageNotes
import com.toolsboox.ui.plugin.SurfaceFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    lateinit var presenter: CalendarWeekPresenter

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
    private lateinit var calendarWeek: CalendarWeek

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
            calendarWeek.noteStrokes[notePage!!] = normalizedStrokes
        } else {
            calendarWeek.calendarStrokes[calendarStyle ?: CalendarMonth.DEFAULT_STYLE] = normalizedStrokes
        }

        calendarPattern.updateWeek(calendarWeek)

        presenter.save(this, binding, calendarWeek, calendarPattern, currentDate)
    }

    /**
     * On side switched event.
     */
    override fun onSideSwitched() {
        utils.updateToolbar(binding, true)

        if (notePage != null)
            CalendarNavigator.toWeekNote(this, currentDate, calendarWeek.locale, notePage!!)
        else
            CalendarNavigator.toWeekPage(this, currentDate, calendarWeek.locale, CalendarWeek.DEFAULT_STYLE)
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
            arguments?.getString("weekOfYear")?.toIntOrNull()?.let { weekOfYear ->
                val weekFields = WeekFields.of(locale)
                Timber.i("Set year and week to '$year'/'$weekOfYear' from parameter")
                currentDate = LocalDate.ofYearDay(year, 1)
                    .with(weekFields.weekOfYear(), weekOfYear.toLong())
                    .with(weekFields.dayOfWeek(), 1)
            }
        }
        calendarStyle = arguments?.getString("calendarStyle") ?: CalendarWeek.DEFAULT_STYLE
        notePage = arguments?.getString("notePage")

        val weekOfWeekBasedYear = WeekFields.of(locale).weekOfWeekBasedYear()
        calendarWeek = CalendarWeek(currentDate.year, currentDate.get(weekOfWeekBasedYear), locale)

        binding.navigatorImageView.setOnTouchListener { view, motionEvent ->
            CalendarWeekNavigator.onTouchEvent(view, motionEvent, this@CalendarWeekFragment, calendarWeek)
        }

        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)

            if (notePage != null) {
                CalendarWeekPageNotes.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarWeekFragment, calendarWeek, notePage!!
                )
            } else {
                CalendarWeekPage.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarWeekFragment, calendarWeek
                )
            }
        }

        toolbar.toolbarPager.visibility = View.GONE

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
            presenter.load(this@CalendarWeekFragment, binding, currentDate, locale)
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
     * @param calendarWeek the data class
     * @param calendarPattern the pattern data class
     */
    fun renderPage(calendarWeek: CalendarWeek, calendarPattern: CalendarPattern) {
        this.calendarWeek = calendarWeek
        this.calendarPattern = calendarPattern
        updateNavigator()

        if (notePage != null) {
            val noteTemplate = sharedPreferences.getInt("calendarNoteTemplate", 0)
            val noteStrokes = calendarWeek.noteStrokes[notePage] ?: listOf()
            CalendarWeekPageNotes.drawPage(this.requireContext(), templateCanvas, calendarWeek, noteTemplate, notePage!!)
            applyStrokes(surfaceTo(noteStrokes), true)
        } else {
            val calendarStrokes = calendarWeek.calendarStrokes[calendarStyle ?: CalendarMonth.DEFAULT_STYLE] ?: listOf()
            CalendarWeekPage.drawPage(this.requireContext(), templateCanvas, calendarWeek, calendarPattern)
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

        val locale = calendarWeek.locale
        val year = currentDate.year
        val weekOfYearField = WeekFields.of(locale).weekOfWeekBasedYear()
        val weekOfYear = currentDate.get(weekOfYearField)

        val pageTitle = getString(R.string.calendar_week_title).format(year, weekOfYear)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        CalendarWeekNavigator.draw(this.requireContext(), navigatorCanvas, calendarWeek, calendarPattern)

        firebaseAnalytics.logEvent("calendarWeek") {
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

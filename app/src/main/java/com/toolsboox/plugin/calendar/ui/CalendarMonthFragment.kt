package com.toolsboox.plugin.calendar.ui

import android.content.SharedPreferences
import android.icu.text.DateFormat
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
import com.toolsboox.plugin.calendar.ot.CalendarMonthNavigator
import com.toolsboox.plugin.calendar.ot.CalendarMonthPage
import com.toolsboox.plugin.calendar.ot.CalendarMonthPageNotes
import com.toolsboox.plugin.calendar.ot.CalendarUtils
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
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarMonthPresenter

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
    private lateinit var calendarMonth: CalendarMonth

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
            calendarMonth.noteStrokes[notePage!!] = normalizedStrokes
        } else {
            calendarMonth.calendarStrokes[calendarStyle ?: CalendarMonth.DEFAULT_STYLE] = normalizedStrokes
        }

        calendarPattern.updateMonth(calendarMonth)

        presenter.save(this, binding, calendarMonth, calendarPattern, currentDate)
    }

    /**
     * On side switched event.
     */
    override fun onSideSwitched() {
        utils.updateToolbar(binding, true)

        if (notePage != null)
            CalendarNavigator.toMonthNote(this, currentDate, notePage!!)
        else
            CalendarNavigator.toMonthPage(this, currentDate, CalendarMonth.DEFAULT_STYLE)
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
        calendarStyle = arguments?.getString("calendarStyle") ?: CalendarMonth.DEFAULT_STYLE
        notePage = arguments?.getString("notePage")

        calendarMonth = CalendarMonth(currentDate.year, currentDate.monthValue, locale)

        binding.navigatorImageView.setOnTouchListener { view, motionEvent ->
            CalendarMonthNavigator.onTouchEvent(view, motionEvent, this@CalendarMonthFragment, calendarMonth)
        }

        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)

            if (notePage != null) {
                CalendarMonthPageNotes.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarMonthFragment, calendarMonth, notePage!!
                )
            } else {
                CalendarMonthPage.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarMonthFragment, calendarMonth
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
            presenter.load(this@CalendarMonthFragment, binding, currentDate, locale)
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
     * @param calendarMonth the data class
     * @param calendarPattern the pattern data class
     */
    fun renderPage(calendarMonth: CalendarMonth, calendarPattern: CalendarPattern) {
        this.calendarMonth = calendarMonth
        this.calendarPattern = calendarPattern
        updateNavigator()

        if (notePage != null) {
            val noteTemplate = sharedPreferences.getInt("calendarNoteTemplate", 0)
            val noteStrokes = calendarMonth.noteStrokes[notePage] ?: listOf()
            CalendarMonthPageNotes.drawPage(this.requireContext(), templateCanvas, calendarMonth, noteTemplate, notePage!!)
            applyStrokes(surfaceTo(noteStrokes), true)
        } else {
            val calendarStrokes = calendarMonth.calendarStrokes[calendarStyle ?: CalendarMonth.DEFAULT_STYLE] ?: listOf()
            CalendarMonthPage.drawPage(this.requireContext(), templateCanvas, calendarMonth, calendarPattern)
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

        val dateFormat = DateFormat.getPatternInstance(DateFormat.YEAR_MONTH)
        val titleDate = dateFormat.format(Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))

        val pageTitle = getString(R.string.calendar_month_title).format(titleDate)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        CalendarMonthNavigator.draw(this.requireContext(), navigatorCanvas, calendarMonth, calendarPattern)

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

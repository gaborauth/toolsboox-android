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
import com.toolsboox.plugin.calendar.da.v1.GoogleCalendarEvent
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import com.toolsboox.plugin.calendar.ot.*
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
     * The Firebase analytics.
     */
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarDayPresenter

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
    private var calendarStyle: String = CalendarDay.DEFAULT_STYLE

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
    private lateinit var calendarDay: CalendarDay

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
            calendarDay.noteStrokes[notePage!!] = normalizedStrokes
        } else {
            calendarDay.calendarStrokes[calendarStyle] = normalizedStrokes
        }

        if (calendarStyle == CalendarDay.HEALTH_V1_STYLE) {
            HealthDayPage.drawPage(this.requireContext(), templateCanvas, calendarDay)
        }

        calendarPattern.updateDay(calendarDay)

        presenter.save(this, binding, calendarDay, calendarPattern, currentDate)
    }

    /**
     * On side switched event.
     */
    override fun onSideSwitched() {
        utils.updateToolbar(binding, true)

        if (notePage != null)
            CalendarNavigator.toDayNote(this, currentDate, notePage!!)
        else
            CalendarNavigator.toDayPage(this, currentDate, CalendarDay.DEFAULT_STYLE)
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
        calendarStyle = arguments?.getString("calendarStyle") ?: CalendarDay.DEFAULT_STYLE
        notePage = arguments?.getString("notePage")

        calendarDay = CalendarDay(currentDate.year, currentDate.monthValue, currentDate.dayOfMonth, locale)

        binding.navigatorImageView.setOnTouchListener { view, motionEvent ->
            CalendarDayNavigator.onTouchEvent(view, motionEvent, this@CalendarDayFragment, calendarDay)
        }

        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            val gestureResult = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)

            if (notePage != null)
                CalendarDayPageNotes.onTouchEvent(
                    view, motionEvent, gestureResult, this@CalendarDayFragment, calendarDay, notePage!!
                )
            else {
                when (calendarStyle) {
                    CalendarDay.DEFAULT_STYLE -> {
                        CalendarDayPage.onTouchEvent(
                            view, motionEvent, gestureResult, this@CalendarDayFragment, calendarDay
                        )
                    }

                    CalendarDay.HEALTH_V1_STYLE -> {
                        HealthDayPage.onTouchEvent(
                            view, motionEvent, gestureResult, this@CalendarDayFragment, calendarDay
                        )
                    }

                    CalendarDay.TIME_BOX_V1_STYLE -> {
                        TimeBoxDayPage.onTouchEvent(
                            view, motionEvent, gestureResult, this@CalendarDayFragment, calendarDay
                        )
                    }

                    else -> return@setOnTouchListener true
                }
            }
        }

        toolbar.toolbarPager.visibility = View.GONE

        binding.toolbarDrawing.toolbarSwipeUp.setOnClickListener {
            if (notePage != null) {
                val page = notePage!!.toIntOrNull() ?: 0
                if (page == 0) {
                    CalendarNavigator.toDayPage(this, currentDate, CalendarDay.DEFAULT_STYLE)
                } else {
                    CalendarNavigator.toDayNote(this, currentDate, "${page - 1}")
                }
            } else {
                CalendarNavigator.toWeekPage(this, currentDate, locale)
            }
        }
        binding.toolbarDrawing.toolbarSwipeDown.setOnClickListener {
            if (notePage != null) {
                val page = notePage!!.toIntOrNull() ?: 0
                CalendarNavigator.toDayNote(this, currentDate, "${page + 1}")
            } else {
                CalendarNavigator.toDayNote(this, currentDate, "0")
            }
        }
        binding.toolbarDrawing.toolbarCalendarView.setOnClickListener {
            calendarStyle = CalendarDay.DEFAULT_STYLE
            presenter.load(this@CalendarDayFragment, binding, currentDate, locale)
        }
        binding.toolbarDrawing.toolbarHealthView.setOnClickListener {
            calendarStyle = CalendarDay.HEALTH_V1_STYLE
            presenter.load(this@CalendarDayFragment, binding, currentDate, locale)
        }
        binding.toolbarDrawing.toolbarTimeboxView.setOnClickListener {
            calendarStyle = CalendarDay.TIME_BOX_V1_STYLE
            presenter.load(this@CalendarDayFragment, binding, currentDate, locale)
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
            presenter.load(this@CalendarDayFragment, binding, currentDate, locale)
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
     * @param calendarPattern the pattern data class
     * @param googleCalendarEvents the Google Calender events
     */
    fun renderPage(
        calendarDay: CalendarDay, calendarPattern: CalendarPattern, googleCalendarEvents: List<GoogleCalendarEvent>
    ) {
        this.calendarDay = calendarDay
        this.calendarPattern = calendarPattern
        updateNavigator()

        if (notePage != null) {
            val noteTemplate = sharedPreferences.getInt("calendarNoteTemplate", 0)
            val noteStrokes = calendarDay.noteStrokes[notePage] ?: listOf()
            CalendarDayPageNotes.drawPage(this.requireContext(), templateCanvas, calendarDay, noteTemplate, notePage!!)
            applyStrokes(surfaceTo(noteStrokes), true)
        } else {
            val startHour = sharedPreferences.getInt("calendarStartHour", 0)
            val calendarStrokes = calendarDay.calendarStrokes[calendarStyle] ?: listOf()
            if (calendarStyle == CalendarDay.DEFAULT_STYLE) {
                CalendarDayPage.drawPage(this.requireContext(), templateCanvas, calendarDay, googleCalendarEvents, startHour)
            }
            if (calendarStyle == CalendarDay.HEALTH_V1_STYLE) {
                HealthDayPage.drawPage(this.requireContext(), templateCanvas, calendarDay)
            }
            if (calendarStyle == CalendarDay.TIME_BOX_V1_STYLE) {
                TimeBoxDayPage.drawPage(this.requireContext(), templateCanvas, calendarDay, startHour)
            }
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

        val titleDate = currentDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))

        val pageTitle = getString(R.string.calendar_day_title).format(titleDate)
        toolbar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        CalendarDayNavigator.draw(this.requireContext(), navigatorCanvas, calendarDay, calendarPattern)

        if (calendarStyle == CalendarDay.DEFAULT_STYLE) {
            firebaseAnalytics.logEvent("calendarDay") {
                param("currentDate", currentDate.format(DateTimeFormatter.ISO_DATE))
            }
        }
        if (calendarStyle == CalendarDay.HEALTH_V1_STYLE) {
            firebaseAnalytics.logEvent("healthDay_v1") {
                param("currentDate", currentDate.format(DateTimeFormatter.ISO_DATE))
            }
        }
        if (calendarStyle == CalendarDay.TIME_BOX_V1_STYLE) {
            firebaseAnalytics.logEvent("timeBoxDay_v1") {
                param("currentDate", currentDate.format(DateTimeFormatter.ISO_DATE))
            }
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

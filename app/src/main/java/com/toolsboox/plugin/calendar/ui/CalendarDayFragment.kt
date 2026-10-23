package com.toolsboox.plugin.calendar.ui

import android.graphics.Matrix
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.toolsboox.R
import com.toolsboox.da.Stroke
import com.toolsboox.da.TextElement
import com.toolsboox.databinding.FragmentCalendarBinding
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v1.CalendarEvent
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
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
        val strokesCopy = Stroke.listDeepCopy(strokes)
        if (notePage != null) {
            calendarDay.noteStrokes[notePage!!] = strokesCopy
        } else {
            calendarDay.calendarStrokes[calendarStyle] = strokesCopy
        }

        if (calendarStyle == CalendarDay.HEALTH_V1_STYLE) {
            HealthDayPage.drawPage(this.requireContext(), templateCanvas, calendarDay)
        }

        calendarPattern.updateDay(calendarDay)

        presenter.save(this, binding, calendarDay, calendarPattern, currentDate)
    }

    /**
     * Text elements changed callback.
     *
     * @param textElements the current text elements
     */
    override fun onTextElementsChanged(textElements: MutableList<TextElement>) {
        calendarDay.textElements = textElements
        calendarPattern.updateDay(calendarDay)
        presenter.save(this, binding, calendarDay, calendarPattern, currentDate)
    }

    /**
     * On strokes procrastinated event.
     *
     * @param strokes the strokes to procrastinate
     */
    override fun onStrokesProcrastinated(strokes: List<Stroke>) {
        firebaseAnalytics.logEvent("procrastinator", null)
        presenter.procrastinate(this, binding, Stroke.listDeepCopy(strokes), currentDate, calendarDay, calendarStyle)
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

        val defaultStartHour = sharedPreferences.getInt("calendarStartHour", 5)
        calendarDay = CalendarDay(
            currentDate.year, currentDate.monthValue, currentDate.dayOfMonth, locale,
            mutableListOf(), mutableListOf(), true, defaultStartHour
        )

        binding.navigatorImageView.setOnTouchListener { view, motionEvent ->
            CalendarDayNavigator.onTouchEvent(view, motionEvent, this@CalendarDayFragment, calendarDay)
        }

        binding.surfaceView.setOnHoverListener { _, motionEvent ->
            return@setOnHoverListener callback(motionEvent, true)
        }
        binding.surfaceView.setOnTouchListener { view, motionEvent ->
            if (callback(motionEvent, false)) return@setOnTouchListener true
            if (handleZoomPanTouch(motionEvent)) return@setOnTouchListener true

            val rawGesture = gestureListener.onTouchEvent(gestureDetector, view, motionEvent)
            val gestureResult = if (twoFingerGesture) rawGesture else OnGestureListener.NONE

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
                if (notePage == "gratitude") {
                    CalendarNavigator.toDayPage(this, currentDate, CalendarDay.DEFAULT_STYLE)
                } else {
                    val page = notePage!!.toIntOrNull() ?: 0
                    if (page == 0) {
                        CalendarNavigator.toDayNote(this, currentDate, "gratitude")
                    } else {
                        CalendarNavigator.toDayNote(this, currentDate, "${page - 1}")
                    }
                }
            } else {
                CalendarNavigator.toWeekPage(this, currentDate, locale)
            }
        }
        binding.toolbarDrawing.toolbarSwipeDown.setOnClickListener {
            if (notePage != null) {
                if (notePage == "gratitude") {
                    CalendarNavigator.toDayNote(this, currentDate, "0")
                } else {
                    val page = notePage!!.toIntOrNull() ?: 0
                    CalendarNavigator.toDayNote(this, currentDate, "${page + 1}")
                }
            } else {
                CalendarNavigator.toDayNote(this, currentDate, "gratitude")
            }
        }
        binding.toolbarDrawing.toolbarCalendarView.setOnClickListener {
            // Always jump to today's day page (Default style), regardless of what
            // date you're currently viewing or whether you're on a note page.
            CalendarNavigator.toDayPage(this, LocalDate.now(), CalendarDay.DEFAULT_STYLE)
        }
        binding.toolbarDrawing.toolbarHealthView.setOnClickListener {
            calendarStyle = CalendarDay.HEALTH_V1_STYLE
            presenter.load(this@CalendarDayFragment, binding, currentDate, defaultStartHour, locale)
        }
        binding.toolbarDrawing.toolbarTimeboxView.setOnClickListener {
            calendarStyle = CalendarDay.TIME_BOX_V1_STYLE
            presenter.load(this@CalendarDayFragment, binding, currentDate, defaultStartHour, locale)
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

        val defaultStartHour = sharedPreferences.getInt("calendarStartHour", 5)
        timer = GlobalScope.launch(Dispatchers.Main) {
            presenter.load(this@CalendarDayFragment, binding, currentDate, defaultStartHour, locale)
            syncPresenter.backgroundSync(this@CalendarDayFragment, UUID.randomUUID())
        }
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        toolbar.toolbarPager.visibility = View.GONE
        timer.cancel()
        syncPresenter.backgroundSync(this@CalendarDayFragment, UUID.randomUUID())
    }

    override fun onTransformChanged(matrix: Matrix) {
        if (::binding.isInitialized) {
            binding.templateImageView.scaleType = ImageView.ScaleType.MATRIX
            binding.templateImageView.imageMatrix = matrix
        }
    }

    /**
     * Reload the current page.
     *
     * @param calendarDay the data class
     * @param calendarPattern the pattern data class
     * @param calendarEvents the calendar events
     */
    fun renderPage(
        calendarDay: CalendarDay, calendarPattern: CalendarPattern, calendarEvents: List<CalendarEvent>
    ) {
        this.calendarDay = calendarDay
        this.calendarPattern = calendarPattern
        updateNavigator()

        // Load text elements from the calendar data
        setTextElements(calendarDay.textElements)

        if (notePage != null) {
            binding.toolbarDrawing.toolbarProcrastinator.visibility = View.GONE
            val noteTemplate = sharedPreferences.getInt("calendarNoteTemplate", 0)
            val noteStrokes = calendarDay.noteStrokes[notePage] ?: listOf()
            CalendarDayPageNotes.drawPage(this.requireContext(), templateCanvas, calendarDay, noteTemplate, notePage!!)
            applyStrokes(Stroke.listDeepCopy(noteStrokes), true)
        } else {
            binding.toolbarDrawing.toolbarProcrastinator.visibility = View.VISIBLE
            val calendarStrokes = calendarDay.calendarStrokes[calendarStyle] ?: listOf()
            if (calendarStyle == CalendarDay.DEFAULT_STYLE) {
                CalendarDayPage.drawPage(this.requireContext(), templateCanvas, calendarDay, calendarEvents)
            }
            if (calendarStyle == CalendarDay.HEALTH_V1_STYLE) {
                HealthDayPage.drawPage(this.requireContext(), templateCanvas, calendarDay)
            }
            if (calendarStyle == CalendarDay.TIME_BOX_V1_STYLE) {
                TimeBoxDayPage.drawPage(this.requireContext(), templateCanvas, calendarDay)
            }
            applyStrokes(Stroke.listDeepCopy(calendarStrokes), true)
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
        toolbar.root.title = getString(R.string.calendar_main_title, pageTitle)

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

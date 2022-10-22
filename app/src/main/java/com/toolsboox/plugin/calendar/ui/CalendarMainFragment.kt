package com.toolsboox.plugin.calendar.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.view.SurfaceView
import android.view.View
import com.google.gson.GsonBuilder
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarMainBinding
import com.toolsboox.plugin.calendar.da.CalendarDay
import com.toolsboox.plugin.calendar.ot.CalendarDayCreator
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.SurfaceFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.*
import javax.inject.Inject

/**
 * Calendar main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarMainFragment @Inject constructor() : SurfaceFragment() {

    /**
     * The injected router.
     */
    @Inject
    lateinit var router: Router

    @Inject
    lateinit var presenter: CalendarMainPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_calendar_main

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarMainBinding

    /**
     * The current date.
     */
    private var currentDate: Date = Date()

    /**
     * The timer job.
     */
    private lateinit var timer: Job

    /**
     * Timestamp of the last stroke.
     */
    private var last: Long = 0

    /**
     * The canvas of the template.
     */
    private lateinit var templateCanvas: Canvas

    /**
     * The bitmap of the template.
     */
    private lateinit var templateBitmap: Bitmap

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
        val calendarDay = CalendarDay(
            true, true, true, 6, strokes
        )

        val permissionGranted = checkPermissionGranted(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
            getString(R.string.main_write_external_storage_permission_title),
            getString(R.string.main_write_external_storage_permission_message)
        )
        if (permissionGranted) {
            val rootPath = Environment.getExternalStorageDirectory()
            File("$rootPath/toolsBoox/").mkdirs()

            val filename = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentDate)
            val path = "$rootPath/toolsBoox/daily-$filename.json"
            try {
                PrintWriter(FileWriter(File(path))).use {
                    it.write(GsonBuilder().create().toJson(calendarDay).toString())
                }
            } catch (e: IOException) {
                somethingHappened(e)
            }
        }
    }

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarMainBinding.bind(view)

        currentDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
        binding.buttonPrev.setOnClickListener {
            currentDate = Date.from(currentDate.toInstant().minus(1L, ChronoUnit.DAYS))
            renderPage()
        }
        binding.buttonNext.setOnClickListener {
            currentDate = Date.from(currentDate.toInstant().plus(1L, ChronoUnit.DAYS))
            renderPage()
        }

        binding.buttonYear.setOnClickListener {
            val year = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy"))
            Timber.i("Route to the '$year' year calendar")
            router.dispatch("/calendar/year/$year", false)
        }

        toolBar.toolbarNext.setOnClickListener { renderPage() }
        toolBar.toolbarPrevious.setOnClickListener { renderPage() }
        toolBar.toolbarPager.visibility = View.GONE

        initializeSurface(true)

        templateBitmap = Bitmap.createBitmap(1404, 1872, Bitmap.Config.ARGB_8888)
        templateCanvas = Canvas(templateBitmap)

        binding.templateImage.setImageBitmap(templateBitmap)
        binding.templateImage.invalidate()
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        timer = GlobalScope.launch(Dispatchers.Main) {
            renderPage()
        }
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        toolBar.toolbarPager.visibility = View.GONE

        last = 0

        timer.cancel()
    }

    /**
     * Reload the current page.
     */
    private fun renderPage() {
        val permissionGranted = checkPermissionGranted(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            REQUEST_PERMISSION_READ_EXTERNAL_STORAGE,
            getString(R.string.main_read_external_storage_permission_title),
            getString(R.string.main_read_external_storage_permission_message)
        )

        var calendarDay = CalendarDay(true, true, true, 6, mutableListOf())
        if (permissionGranted) {
            val rootPath = Environment.getExternalStorageDirectory()
            File("$rootPath/toolsBoox/").mkdirs()

            val filename = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentDate)
            val path = "$rootPath/toolsBoox/daily-$filename.json"
            if (File(path).exists()) {
                FileReader(File(path)).use {
                    calendarDay = GsonBuilder().create().fromJson(it, CalendarDay::class.java)
                }
            }
        }

        val localDate = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val dateFormat = DateFormat.getDateFormat(context)
        val formattedDate = dateFormat.format(currentDate)

        val pageTitle = getString(R.string.calendar_day_title).format(formattedDate)
        toolBar.root.title = getString(R.string.drawer_title).format(getString(R.string.calendar_main_title), pageTitle)

        binding.buttonYear.text = SimpleDateFormat("yyyy", Locale.getDefault()).format(currentDate)
        binding.buttonMonth.text = SimpleDateFormat("MMMM", Locale.getDefault()).format(currentDate)
        binding.buttonDay.text = SimpleDateFormat("dd", Locale.getDefault()).format(currentDate)
        binding.buttonWeek.text = "W" + localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear())
        binding.buttonDayOfWeek.text = SimpleDateFormat("EEE", Locale.getDefault()).format(currentDate)

        CalendarDayCreator.drawPage(
            this.requireContext(),
            templateCanvas,
            calendarDay.withNotes, calendarDay.withTasks, calendarDay.withHours, calendarDay.startHours
        )

        applyStrokes(calendarDay.strokes.toMutableList(), true)

        last = 0
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

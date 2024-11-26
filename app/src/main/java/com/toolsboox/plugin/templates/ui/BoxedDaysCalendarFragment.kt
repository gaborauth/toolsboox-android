package com.toolsboox.plugin.templates.ui

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.toolsboox.R
import com.toolsboox.databinding.FragmentTemplatesBoxedDaysCalendarBinding
import com.toolsboox.plugin.templates.ot.BoxedDayCalendarCreator
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * Templates 'boxed week's calendar' fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class BoxedDaysCalendarFragment @Inject constructor() : ScreenFragment() {

    /**
     * The Firebase analytics.
     */
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var presenter: BoxedDaysCalendarPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_templates_boxed_days_calendar

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTemplatesBoxedDaysCalendarBinding

    /**
     * The canvas of the template.
     */
    private lateinit var canvas: Canvas

    /**
     * The bitmap of the template.
     */
    private lateinit var bitmap: Bitmap

    private var selectedDate = LocalDate.now()

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTemplatesBoxedDaysCalendarBinding.bind(view)

        binding.buttonPreview.isChecked = true
        binding.buttonPreview.setOnClickListener {
            binding.previewPane.visibility = View.VISIBLE
            binding.settingsPane.visibility = View.GONE
            binding.exportPane.visibility = View.GONE

            createPreview()
        }
        binding.buttonSettings.setOnClickListener {
            binding.previewPane.visibility = View.GONE
            binding.settingsPane.visibility = View.VISIBLE
            binding.exportPane.visibility = View.GONE
        }
        binding.buttonExport.setOnClickListener {
            binding.previewPane.visibility = View.GONE
            binding.settingsPane.visibility = View.GONE
            binding.exportPane.visibility = View.VISIBLE

            presenter.export(this, binding, selectedDate)
        }

        binding.settingsWithNotes.isChecked = true
        binding.settingsWithTasks.isChecked = true
        binding.settingsWithHours.isChecked = true

        binding.textSelectedDate.text = selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        binding.buttonDatePicker.setOnClickListener {
            val dpd = DatePickerDialog(requireContext(), { view, year, monthOfYear, dayOfMonth ->
                selectedDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                binding.textSelectedDate.text = selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
            }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
            dpd.show()
        }

        binding.preview.post {
            createPreview()
        }
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.templates_boxed_days_calendar_title))

        firebaseAnalytics.logEvent("templates") {
            param("view", "boxedDaysCalendar")
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

    /**
     * Create preview on the canvas.
     */
    private fun createPreview() {
        bitmap = Bitmap.createBitmap(1404, 1872, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)

        Timber.i("$selectedDate")
        BoxedDayCalendarCreator.drawPage(
            this.requireContext(),
            canvas,
            selectedDate.dayOfMonth.toLong(),
            selectedDate,
            binding.settingsWithNotes.isChecked,
            binding.settingsWithTasks.isChecked,
            binding.settingsWithHours.isChecked,
        )

        binding.preview.setImageBitmap(bitmap)
        binding.preview.invalidate()
    }
}

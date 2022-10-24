package com.toolsboox.plugin.templates.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import com.toolsboox.R
import com.toolsboox.databinding.FragmentTemplatesFlatWeeksCalendarBinding
import com.toolsboox.plugin.templates.ot.FlatWeekCalendarCreator
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*
import javax.inject.Inject

/**
 * Templates 'flat week's calendar' fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class FlatWeeksCalendarFragment @Inject constructor() : ScreenFragment() {

    @Inject
    lateinit var presenter: FlatWeeksCalendarPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_templates_flat_weeks_calendar

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTemplatesFlatWeeksCalendarBinding

    /**
     * The canvas of the template.
     */
    private lateinit var canvas: Canvas

    /**
     * The bitmap of the template.
     */
    private lateinit var bitmap: Bitmap

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTemplatesFlatWeeksCalendarBinding.bind(view)

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

            presenter.export(this, binding)
        }

        binding.settingsWithDays.isChecked = true

        binding.preview.post {
            createPreview()
        }
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.templates_flat_weeks_calendar_title))
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

        val localDate = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
        val weekOfYear = localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear())
        FlatWeekCalendarCreator.drawPage(
            this.requireContext(),
            canvas,
            weekOfYear.toLong() - 1,
            0.5f,
            binding.settingsWithDays.isChecked

        )

        binding.preview.setImageBitmap(bitmap)
        binding.preview.invalidate()
    }
}

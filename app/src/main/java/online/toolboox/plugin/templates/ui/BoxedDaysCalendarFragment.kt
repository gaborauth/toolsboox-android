package online.toolboox.plugin.templates.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import online.toolboox.R
import online.toolboox.databinding.FragmentTemplatesBoxedDaysCalendarBinding
import online.toolboox.plugin.templates.ot.BoxedDayCalendarCreator
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment
import java.time.LocalDate
import javax.inject.Inject

/**
 * Templates 'boxed week's calendar' fragment.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class BoxedDaysCalendarFragment @Inject constructor(
    private val presenter: BoxedDaysCalendarPresenter,
    private val router: Router
) : ScreenFragment() {

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

            presenter.export(this, binding)
        }

        binding.settingsWithNotes.isChecked = true
        binding.settingsWithTasks.isChecked = true
        binding.settingsWithHours.isChecked = true

        binding.preview.post {
            createPreview()
        }
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.templates_boxed_days_calendar_title))
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

        val localDate = LocalDate.now()
        BoxedDayCalendarCreator.drawPage(
            this.requireContext(),
            canvas,
            localDate.dayOfMonth.toLong(),
            binding.settingsWithNotes.isChecked,
            binding.settingsWithTasks.isChecked,
            binding.settingsWithHours.isChecked,
        )

        binding.preview.setImageBitmap(bitmap)
        binding.preview.invalidate()
    }
}

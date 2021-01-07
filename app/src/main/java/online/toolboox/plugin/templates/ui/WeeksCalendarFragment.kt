package online.toolboox.plugin.templates.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.View
import online.toolboox.R
import online.toolboox.databinding.FragmentTemplatesWeeksCalendarBinding
import online.toolboox.plugin.templates.ot.WeekCalendarCreator
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*
import javax.inject.Inject

/**
 * Templates 'week's calendar' fragment.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class WeeksCalendarFragment @Inject constructor(
    private val presenter: WeeksCalendarPresenter,
    private val router: Router
) : ScreenFragment() {

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_templates_weeks_calendar

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTemplatesWeeksCalendarBinding

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

        binding = FragmentTemplatesWeeksCalendarBinding.bind(view)

        binding.buttonExport.setOnClickListener {
            val permissionGranted = checkPermissionGranted(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                REQUEST_PERMISSION_READ_EXTERNAL_STORAGE,
                getString(R.string.main_read_external_storage_permission_title),
                getString(R.string.main_read_external_storage_permission_message)
            ) and checkPermissionGranted(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                getString(R.string.main_write_external_storage_permission_title),
                getString(R.string.main_write_external_storage_permission_message)
            )

            if (permissionGranted) {
                val doc = PdfDocument()
                for (p in 0L..52L) {
                    val pageInfo = PdfDocument.PageInfo.Builder(1404, 1872, p.toInt() + 1).create()
                    val page = doc.startPage(pageInfo)
                    WeekCalendarCreator.drawPage(this.requireContext(), page.canvas, p)
                    doc.finishPage(page)
                }

                val rootPath = Environment.getExternalStorageDirectory()
                val title = "weeks-calendar-${LocalDate.now().year}.pdf"
                val filename = "$rootPath/noteTemplate/$title"
                Timber.i("Save to $filename")
                try {
                    FileOutputStream(filename).use { out -> doc.writeTo(out) }
                    doc.close()
                } catch (e: IOException) {
                    Timber.e(e.toString(), e)
                }
                showMessage(
                    getString(R.string.templates_weeks_calendar_preview_export_message, title),
                    binding.root
                )
            }
        }

        binding.preview.post {
            bitmap = Bitmap.createBitmap(1404, 1872, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap)

            val localDate = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
            val weekOfYear = localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear())
            WeekCalendarCreator.drawPage(this.requireContext(), canvas, weekOfYear.toLong() - 1)

            binding.preview.setImageBitmap(bitmap)
            binding.preview.invalidate()
        }
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.templates_weeks_calendar_title))
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

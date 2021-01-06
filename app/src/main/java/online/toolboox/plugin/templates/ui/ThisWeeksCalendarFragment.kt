package online.toolboox.plugin.templates.ui

import android.Manifest
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.view.View
import online.toolboox.R
import online.toolboox.databinding.FragmentTemplatesThisWeeksCalendarBinding
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.*
import javax.inject.Inject

/**
 * Templates 'this week's calendar' fragment.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class ThisWeeksCalendarFragment @Inject constructor(
    private val presenter: ThisWeeksCalendarPresenter,
    private val router: Router
) : ScreenFragment() {

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_templates_this_weeks_calendar

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTemplatesThisWeeksCalendarBinding

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

        binding = FragmentTemplatesThisWeeksCalendarBinding.bind(view)

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
                val localDate = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                val weekOfYear = localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear())

                val rootPath = Environment.getExternalStorageDirectory()
                val title = "calendar-${localDate.year}-$weekOfYear.png"
                val filename = "$rootPath/noteTemplate/$title"
                Timber.i("Save to $filename")
                try {
                    FileOutputStream(filename).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                } catch (e: IOException) {
                    Timber.e(e.toString(), e)
                }
                showMessage(
                    getString(R.string.templates_this_weeks_calendar_preview_export_message, title),
                    binding.root
                )
            }
        }

        binding.preview.post {
            bitmap = Bitmap.createBitmap(1404, 1872, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap)

            val fillPaint = Paint()
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = Color.WHITE
            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, fillPaint)

            drawCalendarRect(0, 0, 0)
            drawCalendarRect(0, 1, 1)
            drawCalendarRect(0, 2, 2)
            drawCalendarRect(0, 3, 3)
            drawCalendarRect(1, 0, 4)
            drawCalendarRect(1, 1, 5)
            drawCalendarRect(1, 2, 6)
            drawCalendarRect(1, 3, 7)

            binding.preview.setImageBitmap(bitmap)
            binding.preview.invalidate()
        }
    }

    /**
     * Draw one day of week.
     *
     * @param x the X offset
     * @param y the Y offset
     * @param d the day offset
     */
    private fun drawCalendarRect(x: Int, y: Int, d: Long) {
        val l = 50.0f  // Left offset
        val t = 100.0f // Top offset
        val w = 630.0f // Width
        val h = 400.0f // Height
        val hg = 44.0f // Horizontal gap
        val vg = 43.0f // Vertical gap

        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)

        val linePaint = Paint()
        linePaint.style = Paint.Style.STROKE
        linePaint.color = Color.BLACK

        val textPaint = Paint()
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.color = Color.BLACK
        textPaint.textSize = 40.0f

        canvas.drawRect(
            l + x * (w + hg), t + y * (h + vg),
            l + x * (w + hg) + w, t + y * (h + vg) + 50.0f,
            fillPaint
        )
        canvas.drawRect(
            l + x * (w + hg), t + y * (h + vg),
            l + x * (w + hg) + w, t + y * (h + vg) + h,
            linePaint
        )

        linePaint.color = Color.argb(0.2f, 0.5f, 0.5f, 0.5f)
        linePaint.strokeWidth = 2.0f
        for (i in 1..7) {
            canvas.drawLine(
                l + x * (w + hg), t + y * (h + vg) + i * 50.0f,
                l + x * (w + hg) + w, t + y * (h + vg) + i * 50.0f,
                linePaint
            )
        }

        val localDate = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), (d % 7L) + 1)
        val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

        if (d == 7L) {
            val year = localDate.year
            val weekOfYear = localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear())

            textPaint.textSize = 80.0f
            canvas.drawText(
                getString(R.string.templates_this_weeks_calendar_preview_title, year, weekOfYear),
                100.0f, 80.0f,
                textPaint
            )

            textPaint.textSize = 40.0f
            canvas.drawText(
                getString(R.string.templates_this_weeks_calendar_preview_weekly_notes),
                l + x * (w + hg) + 10.0f, t + y * (h + vg) + 40.0f,
                textPaint
            )
        } else {
            val dayFormat = SimpleDateFormat("EEEE").format(date)
            val dateFormat = SimpleDateFormat("YYYY-MM-dd").format(date)

            textPaint.textSize = 40.0f
            canvas.drawText(
                dayFormat,
                l + x * (w + hg) + 10.0f, t + y * (h + vg) + 40.0f,
                textPaint
            )

            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                dateFormat,
                l + x * (w + hg) + w - 10.0f, t + y * (h + vg) + 40.0f,
                textPaint
            )
        }
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.templates_this_weeks_calendar_title))
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

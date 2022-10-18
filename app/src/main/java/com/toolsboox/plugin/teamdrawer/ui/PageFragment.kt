package com.toolsboox.plugin.teamdrawer.ui

import android.Manifest
import android.app.AlertDialog
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.view.SurfaceView
import android.view.View
import com.onyx.android.sdk.pen.TouchHelper
import com.toolsboox.BuildConfig
import com.toolsboox.R
import com.toolsboox.databinding.FragmentTeamdrawerPageBinding
import com.toolsboox.plugin.teamdrawer.nw.NoteRepository
import com.toolsboox.plugin.teamdrawer.nw.domain.Note
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import com.toolsboox.plugin.teamdrawer.nw.domain.StrokePoint
import com.toolsboox.plugin.teamdrawer.nw.dto.NotePageComplex
import com.toolsboox.ui.plugin.SurfaceFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.Instant
import java.util.*
import javax.inject.Inject

/**
 * Team drawer main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class PageFragment @Inject constructor() : SurfaceFragment() {

    @Inject
    lateinit var presenter: PagePresenter

    @Inject
    lateinit var noteRepository: NoteRepository

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_teamdrawer_page

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTeamdrawerPageBinding

    /**
     * The timer job.
     */
    private lateinit var timer: Job

    /**
     * The room ID.
     */
    private lateinit var roomId: UUID

    /**
     * The note ID.
     */
    private lateinit var noteId: UUID

    /**
     * The page ID.
     */
    private lateinit var pageId: UUID

    /**
     * Timestamp of the last stroke.
     */
    private var last: Long = 0

    /**
     * The cached note.
     */
    private lateinit var note: Note

    /**
     * The actual page number and the number of pages.
     */
    private var pageNumber: Int = 1
    private var pageNumbers: Int = 1

    /**
     * SurfaceView provide method.
     *
     * @return the actual surfaceView
     */
    override fun provideSurfaceView(): SurfaceView = binding.surfaceView

    /**
     * Add stroke callback.
     *
     * @param stroke list of stroke points
     */
    override fun addStroke(stroke: List<StrokePoint>) {
        presenter.add(this@PageFragment, roomId, noteId, pageId, stroke)
    }

    /**
     * Delete stroke callback.
     *
     * @param strokeId the UUID of the stroke
     */
    override fun delStroke(strokeId: UUID) {
        presenter.del(this@PageFragment, roomId, noteId, pageId, strokeId)
    }

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTeamdrawerPageBinding.bind(view)

        if (parameters["roomId"] == null) {
            somethingHappened()
            return
        }
        roomId = UUID.fromString(parameters["roomId"])

        if (parameters["noteId"] == null) {
            somethingHappened()
            return
        }
        noteId = UUID.fromString(parameters["noteId"])

        if (parameters["pageId"] == null) {
            somethingHappened()
            return
        }
        pageId = UUID.fromString(parameters["pageId"])

        binding.buttonExport.setOnClickListener {
            val permissionGranted = checkPermissionGranted(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                getString(R.string.main_write_external_storage_permission_title),
                getString(R.string.main_write_external_storage_permission_message)
            )

            if (permissionGranted) {
                val title = "export-${Instant.now().epochSecond}"
                MediaStore.Images.Media.insertImage(
                    this@PageFragment.requireActivity().contentResolver,
                    bitmap,
                    title,
                    title
                )
                showMessage(getString(R.string.team_drawer_page_export_message).format(title), binding.root)
            }
        }

        binding.buttonErase.setOnClickListener {
            presenter.del(this, roomId, noteId, pageId)
        }

        note = noteRepository.getNote(roomId, noteId)!!
        pageNumbers = note.pages.size

        toolBar.toolbarNext.setOnClickListener {
            if (pageNumber < pageNumbers) {
                pageNumber++
            } else {
                createNewPageDialog()
            }
            renderPage()
        }
        toolBar.toolbarPrevious.setOnClickListener {
            if (pageNumber > 1) {
                pageNumber--
            }
            renderPage()
        }

        touchHelper = TouchHelper.create(binding.surfaceView, callback)
        binding.surfaceView.setZOrderOnTop(true)
        binding.surfaceView.holder.setFormat(PixelFormat.TRANSPARENT)
        initializeSurface()
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        val note = noteRepository.getNote(roomId, noteId)!!
        val pageTitle = getString(R.string.team_drawer_page_title).format(note.title)
        toolBar.root.title = getString(R.string.drawer_title).format(getString(R.string.team_drawer_title), pageTitle)

        toolBar.toolbarPages.text = getString(R.string.toolbar_pages_template).format(1, pageNumbers)
        toolBar.toolbarPager.visibility = View.VISIBLE

        initializeSurface()
        touchHelper.setRawDrawingEnabled(true)

        timer = GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                presenter.last(this@PageFragment, roomId, noteId, pageId, false)
                delay(if (BuildConfig.DEBUG) 10000L else 1000L)
            }
        }
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        toolBar.toolbarPager.visibility = View.GONE

        touchHelper.setRawDrawingEnabled(false)
        last = 0

        timer.cancel()
    }

    /**
     * OnDestroy hook.
     */
    override fun onDestroy() {
        super.onDestroy()

        touchHelper.closeRawDrawing()
        bitmap.recycle()
    }

    /**
     * Render the result of 'add' service call.
     *
     * @param stroke the saved stroke
     */
    fun addResult(stroke: Stroke) {
        presenter.last(this, roomId, noteId, pageId, false)
    }

    /**
     * Render the result of 'addPage' service call.
     *
     * @param notePageComplex the note-page complex response
     */
    fun addPageResult(notePageComplex: NotePageComplex) {
        val page = notePageComplex.page
        note = notePageComplex.note
        pageNumbers = note.pages.size
        pageNumber = note.pages.indexOf(page.pageId) + 1
        pageId = page.pageId

        noteRepository.updateNote(roomId, note)

        renderPage()
    }

    /**
     * Render the result of 'del' service call.
     *
     * @param strokes the strokes on the erased page
     */
    fun delResult(strokes: List<Stroke>) {
        listResult(strokes, true)
    }

    /**
     * Render the result of 'last' service call.
     *
     * @param last the last update timestamp
     * @param clearPage clear page flag
     */
    fun lastResult(last: Long, clearPage: Boolean) {
        if (this.last != last) {
            this.last = last
            presenter.list(this, roomId, noteId, pageId, clearPage)
        }
    }

    /**
     * Render the result of 'list' service call.
     *
     * @param strokes the list of strokes
     * @param clearPage clear the page
     */
    fun listResult(strokes: List<Stroke>, clearPage: Boolean) {
        if (clearPage) {
            touchHelper.isRawDrawingRenderEnabled = false
        }

        this.strokes = strokes
        val lockCanvas = binding.surfaceView.holder.lockCanvas()

        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.TRANSPARENT
        val rect = Rect(0, 0, binding.surfaceView.width, binding.surfaceView.height)
        lockCanvas.drawRect(rect, fillPaint)
        if (clearPage) canvas.drawRect(rect, fillPaint)

        for (stroke in strokes) {
            val points = stroke.strokePoints
            if (points.isNotEmpty()) {
                val path = Path()
                val prePoint = PointF(points[0].x, points[0].y)
                if (points.size == 1) {
                    path.moveTo(prePoint.x - 1f, prePoint.y - 1f)
                } else {
                    path.moveTo(prePoint.x, prePoint.y)
                }
                for (point in points) {
                    path.quadTo(prePoint.x, prePoint.y, point.x, point.y)
                    prePoint.x = point.x
                    prePoint.y = point.y
                }

                lockCanvas.drawPath(path, paint)
                canvas.drawPath(path, paint)
            }
        }
        binding.surfaceView.holder.unlockCanvasAndPost(lockCanvas)

        if (clearPage) {
            touchHelper.isRawDrawingRenderEnabled = true
        }
        touchHelper.setRawDrawingEnabled(true)
    }

    /**
     * Reload the current page.
     */
    private fun renderPage() {
        val note = noteRepository.getNote(roomId, noteId)!!
        pageId = note.pages[pageNumber - 1]
        last = 0
        presenter.last(this, roomId, noteId, pageId, true)
        toolBar.toolbarPages.text = getString(R.string.toolbar_pages_template).format(pageNumber, pageNumbers)
    }

    /**
     * Show the add page dialog
     */
    private fun createNewPageDialog() {
        val builder = AlertDialog.Builder(this.requireContext())

        builder.setTitle(R.string.team_drawer_page_add_new_page_dialog_title)
        builder.setMessage(R.string.team_drawer_page_add_new_page_dialog_message)
        builder.setPositiveButton(android.R.string.yes) { _, _ ->
            presenter.addPage(this@PageFragment, roomId, noteId)
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }
        builder.create().show()
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

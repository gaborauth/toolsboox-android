package online.toolboox.plugin.teamdrawer.ui

import online.toolboox.plugin.teamdrawer.nw.PageService
import online.toolboox.plugin.teamdrawer.nw.StrokeService
import online.toolboox.plugin.teamdrawer.nw.domain.StrokePoint
import online.toolboox.ui.plugin.FragmentPresenter
import java.util.*
import javax.inject.Inject

/**
 * Team drawer presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class PagePresenter @Inject constructor(
    private val pageService: PageService,
    private val strokeService: StrokeService
) : FragmentPresenter() {

    /**
     * Add stroke.
     *
     * @param fragment the fragment
     * @param roomId the room ID
     * @param noteId the note ID
     * @param pageId the page ID
     * @param stroke the stroke
     */
    fun add(fragment: PageFragment, roomId: UUID, noteId: UUID, pageId: UUID, stroke: List<StrokePoint>) {
        coroutinesCallHelper(
            fragment,
            { strokeService.addAsync(roomId, noteId, pageId, stroke) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.addResult(body)
                        }
                    }
                    else -> fragment.somethingHappened()
                }
            }
        )
    }

    /**
     * Add new page.
     *
     * @param fragment the fragment
     * @param roomId the room ID
     * @param noteId the note ID
     */
    fun addPage(fragment: PageFragment, roomId: UUID, noteId: UUID) {
        coroutinesCallHelper(
            fragment,
            { pageService.addAsync(roomId, noteId) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.addPageResult(body)
                        }
                    }
                    else -> fragment.somethingHappened()
                }
            }
        )
    }

    /**
     * Delete all strokes of the page.
     *
     * @param fragment the fragment
     * @param roomId the room ID
     * @param noteId the note ID
     * @param pageId the page ID
     */
    fun del(fragment: PageFragment, roomId: UUID, noteId: UUID, pageId: UUID) {
        coroutinesCallHelper(
            fragment,
            { strokeService.delAsync(roomId, noteId, pageId) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.delResult(body)
                        }
                    }
                    else -> fragment.somethingHappened()
                }
            }
        )
    }

    /**
     * Delete a stroke of the page.
     *
     * @param fragment the fragment
     * @param roomId the room ID
     * @param noteId the note ID
     * @param pageId the page ID
     * @param strokeId the stroke ID
     */
    fun del(fragment: PageFragment, roomId: UUID, noteId: UUID, pageId: UUID, strokeId: UUID) {
        coroutinesCallHelper(
            fragment,
            { strokeService.delAsync(roomId, noteId, pageId, strokeId) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.delResult(body)
                        }
                    }
                    else -> fragment.somethingHappened()
                }
            }
        )
    }

    /**
     * Get the timestamp of the last stroke on the page.
     *
     * @param fragment the fragment
     * @param roomId the room ID
     * @param noteId the note ID
     * @param pageId the page ID
     * @param clearPage clear page flag
     */
    fun last(fragment: PageFragment, roomId: UUID, noteId: UUID, pageId: UUID, clearPage: Boolean) {
        coroutinesCallHelper(
            fragment,
            { strokeService.lastAsync(roomId, noteId, pageId) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.lastResult(body, clearPage)
                        }
                    }
                    else -> fragment.somethingHappened()
                }
            }
        )
    }

    /**
     * List the strokes by page ID.
     *
     * @param fragment the fragment
     * @param roomId the room ID
     * @param noteId the note ID
     * @param pageId the page ID
     * @param clearPage clear the page flag
     */
    fun list(fragment: PageFragment, roomId: UUID, noteId: UUID, pageId: UUID, clearPage: Boolean) {
        coroutinesCallHelper(
            fragment,
            { strokeService.listAsync(roomId, noteId, pageId) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.listResult(body, clearPage)
                        }
                    }
                    else -> fragment.somethingHappened()
                }
            }
        )
    }
}

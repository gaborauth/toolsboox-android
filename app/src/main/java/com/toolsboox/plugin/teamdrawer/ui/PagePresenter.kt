package com.toolsboox.plugin.teamdrawer.ui

import com.toolsboox.plugin.teamdrawer.nw.PageService
import com.toolsboox.plugin.teamdrawer.nw.StrokeService
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import com.toolsboox.ui.plugin.FragmentPresenter
import java.util.*
import javax.inject.Inject

/**
 * Team drawer presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class PagePresenter @Inject constructor() : FragmentPresenter() {

    @Inject
    lateinit var pageService: PageService

    @Inject
    lateinit var strokeService: StrokeService

    /**
     * Add stroke.
     *
     * @param fragment the fragment
     * @param roomId the room ID
     * @param noteId the note ID
     * @param pageId the page ID
     * @param strokes the list of strokes
     */
    fun add(fragment: PageFragment, roomId: UUID, noteId: UUID, pageId: UUID, strokes: List<Stroke>) {
        coroutinesCallHelper(
            fragment,
            { strokeService.addAsync(roomId, noteId, pageId, strokes) },
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
     * Delete strokes on the page.
     *
     * @param fragment the fragment
     * @param roomId the room ID
     * @param noteId the note ID
     * @param pageId the page ID
     * @param strokeIds the list of stroke ID
     */
    fun del(fragment: PageFragment, roomId: UUID, noteId: UUID, pageId: UUID, strokeIds: List<UUID>) {
        coroutinesCallHelper(
            fragment,
            { strokeService.delAsync(roomId, noteId, pageId, strokeIds) },
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

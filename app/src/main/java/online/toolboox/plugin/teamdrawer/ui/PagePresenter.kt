package online.toolboox.plugin.teamdrawer.ui

import online.toolboox.plugin.teamdrawer.nw.StrokeService
import online.toolboox.plugin.teamdrawer.nw.domain.StrokePoint
import online.toolboox.ui.plugin.FragmentPresenter
import java.util.*
import javax.inject.Inject

/**
 * Team drawer presenter.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class PagePresenter @Inject constructor(
    private val strokeService: StrokeService
) : FragmentPresenter() {

    /**
     * Add stroke.
     *
     * @param fragment the fragment
     * @param pageId the page ID
     * @param stroke the stroke
     */
    fun add(fragment: PageFragment, pageId: UUID, stroke: List<StrokePoint>) {
        coroutinesCallHelper(
            fragment,
            { strokeService.addAsync(pageId, stroke) },
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
     * Delete all strokes of the page.
     *
     * @param fragment the fragment
     * @param pageId the page ID
     */
    fun del(fragment: PageFragment, pageId: UUID) {
        coroutinesCallHelper(
            fragment,
            { strokeService.delAsync(pageId) },
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
     * @param pageId the page ID
     */
    fun last(fragment: PageFragment, pageId: UUID) {
        coroutinesCallHelper(
            fragment,
            { strokeService.lastAsync(pageId) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.lastResult(body)
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
     * @param pageId the page ID
     */
    fun list(fragment: PageFragment, pageId: UUID) {
        coroutinesCallHelper(
            fragment,
            { strokeService.listAsync(pageId) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.listResult(body)
                        }
                    }
                    else -> fragment.somethingHappened()
                }
            }
        )
    }
}

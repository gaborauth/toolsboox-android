package online.toolboox.main.ui

import online.toolboox.main.nw.domain.StrokePoint
import online.toolboox.ui.BasePresenter
import java.util.*

/**
 * Presenter of main.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class MainPresenter(mainView: MainView) : BasePresenter<MainView>(mainView) {

    /**
     * The static UUID of the page.
     */
    private val pageId = UUID.fromString("178e0a77-d9d2-4a88-b29c-b09007972b53")

    /**
     * Add stroke.
     *
     * @return the response
     */
    fun add(stroke: List<StrokePoint>) {
        coroutinesCallHelper(
            { strokeService.addAsync(pageId, stroke) },
            { response -> view.addResult(response) }
        )
    }

    /**
     * Delete all strokes of the page.
     *
     * @return the empty page
     */
    fun del() {
        coroutinesCallHelper(
            { strokeService.delAsync(pageId) },
            { response -> view.delResult(response) }
        )
    }

    /**
     * Timestamp of the last stroke.
     *
     * @return the response
     */
    fun last() {
        coroutinesCallHelper(
            { strokeService.lastAsync(pageId) },
            { response -> view.lastResult(response) }
        )
    }

    /**
     * List of strokes.
     *
     * @return the response
     */
    fun list() {
        coroutinesCallHelper(
            { strokeService.listAsync(pageId) },
            { response -> view.listResult(response) }
        )
    }
}

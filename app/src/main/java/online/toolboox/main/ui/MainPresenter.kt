package online.toolboox.main.ui

import online.toolboox.main.nw.domain.StrokePoint
import online.toolboox.ui.BasePresenter

/**
 * Presenter of main.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class MainPresenter(mainView: MainView) : BasePresenter<MainView>(mainView) {

    /**
     * Add stroke.
     *
     * @return the response
     */
    fun add(stroke: List<StrokePoint>) {
        coroutinesCallHelper(
            { mainService.addAsync(stroke) },
            { response -> view.addResult(response) }
        )
    }

    /**
     * Timestamp of the last stroke.
     *
     * @return the response
     */
    fun last() {
        coroutinesCallHelper(
            { mainService.lastAsync() },
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
            { mainService.listAsync() },
            { response -> view.listResult(response) }
        )
    }
}

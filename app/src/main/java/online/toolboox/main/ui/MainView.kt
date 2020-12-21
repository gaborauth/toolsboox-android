package online.toolboox.main.ui

import online.toolboox.main.nw.domain.StrokePoint
import online.toolboox.ui.BaseView
import retrofit2.Response

/**
 * Interface providing methods for the view of the main activity.
 *
 * @author <a href="mailto:auth.gabor@gmail.com">Gábor AUTH</a>
 */
interface MainView : BaseView {
    /**
     * Process the result of the 'add' call.
     *
     * @param response the response
     */
    fun addResult(response: Response<List<StrokePoint>>)

    /**
     * Process the result of the 'last' call.
     *
     * @param response the response
     */
    fun lastResult(response: Response<Long>)

    /**
     * Process the result of the 'list' call.
     *
     * @param response the response
     */
    fun listResult(response: Response<List<List<StrokePoint>>>)
}

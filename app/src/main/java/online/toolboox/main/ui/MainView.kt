package online.toolboox.main.ui

import online.toolboox.plugin.teamdrawer.nw.domain.Stroke
import online.toolboox.ui.BaseView
import retrofit2.Response

/**
 * Interface providing methods for the view of the main activity.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
interface MainView : BaseView {
    /**
     * Process the result of the 'add' call.
     *
     * @param response the response
     */
    fun addResult(response: Response<Stroke>)

    /**
     * Process the result of the 'del' call.
     *
     * @param response the response
     */
    fun delResult(response: Response<List<Stroke>>)

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
    fun listResult(response: Response<List<Stroke>>)
}

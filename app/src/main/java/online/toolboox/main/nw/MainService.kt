package online.toolboox.main.nw

import kotlinx.coroutines.Deferred
import online.toolboox.main.nw.domain.StrokePoint
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Main service interface.
 *
 * @author <a href="mailto:auth.gabor@gmail.com">Gábor AUTH</a>
 */
interface MainService {

    /**
     * Add a new stroke to the list of strokes.
     *
     * @param stroke the stroke
     * @return the response
     */
    @POST(value = "toolBoox/add")
    fun addAsync(
        @Body stroke: List<StrokePoint>
    ): Deferred<Response<List<StrokePoint>>>

    /**
     * Timestamp of the last stroke.
     *
     * @return the response
     */
    @GET(value = "toolBoox/last")
    fun lastAsync(): Deferred<Response<Long>>

    /**
     * List of the strokes.
     *
     * @return the response
     */
    @GET(value = "toolBoox/list")
    fun listAsync(): Deferred<Response<List<List<StrokePoint>>>>
}

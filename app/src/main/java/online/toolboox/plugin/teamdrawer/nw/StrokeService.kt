package online.toolboox.plugin.teamdrawer.nw

import kotlinx.coroutines.Deferred
import online.toolboox.plugin.teamdrawer.nw.domain.Stroke
import online.toolboox.plugin.teamdrawer.nw.domain.StrokePoint
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.*

/**
 * Stroke service interface.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
interface StrokeService {

    /**
     * Add a new stroke to the list.
     *
     * @param roomId       the room ID
     * @param noteId       the note ID
     * @param pageId       the page ID
     * @param strokePoints the list of stroke points
     * @return the saved stroke
     */
    @POST(value = "stroke/add/{roomId}/{noteId}/{pageId}")
    fun addAsync(
        @Path("roomId") roomId: UUID,
        @Path("noteId") noteId: UUID,
        @Path("pageId") pageId: UUID,
        @Body strokePoints: List<StrokePoint>
    ): Deferred<Response<Stroke>>

    /**
     * Delete all strokes of the page.
     *
     * @param roomId       the room ID
     * @param noteId       the note ID
     * @param pageId the page ID
     * @return the empty page
     */
    @GET(value = "stroke/del/{roomId}/{noteId}/{pageId}")
    fun delAsync(
        @Path("roomId") roomId: UUID,
        @Path("noteId") noteId: UUID,
        @Path("pageId") pageId: UUID
    ): Deferred<Response<List<Stroke>>>

    /**
     * Delete a stroke by ID of the page.
     *
     * @param roomId       the room ID
     * @param noteId       the note ID
     * @param pageId   the page ID
     * @param strokeId the stroke ID
     * @return the removed stroke
     */
    @GET(value = "stroke/del/{roomId}/{noteId}/{pageId}/{strokeId}")
    fun delAsync(
        @Path("roomId") roomId: UUID,
        @Path("noteId") noteId: UUID,
        @Path("pageId") pageId: UUID,
        @Path("strokeId") strokeId: UUID
    ): Deferred<Response<Stroke>>

    /**
     * Get the timestamp of the last stroke on the page.
     *
     * @param roomId       the room ID
     * @param noteId       the note ID
     * @param pageId the page ID
     * @return the value
     */
    @GET(value = "stroke/last/{roomId}/{noteId}/{pageId}")
    fun lastAsync(
        @Path("roomId") roomId: UUID,
        @Path("noteId") noteId: UUID,
        @Path("pageId") pageId: UUID
    ): Deferred<Response<Long>>

    /**
     * List the strokes by page ID.
     *
     * @param roomId       the room ID
     * @param noteId       the note ID
     * @param pageId the page ID
     * @return the list of strokes
     */
    @GET(value = "stroke/list/{roomId}/{noteId}/{pageId}")
    fun listAsync(
        @Path("roomId") roomId: UUID,
        @Path("noteId") noteId: UUID,
        @Path("pageId") pageId: UUID
    ): Deferred<Response<List<Stroke>>>
}

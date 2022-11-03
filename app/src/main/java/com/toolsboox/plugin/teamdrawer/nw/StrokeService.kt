package com.toolsboox.plugin.teamdrawer.nw

import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.*

/**
 * Stroke service interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface StrokeService {

    /**
     * Add a new stroke to the list.
     *
     * @param roomId       the room ID
     * @param noteId       the note ID
     * @param pageId       the page ID
     * @param strokes      the list of strokes
     * @return the saved stroke
     */
    @POST(value = "stroke/addStrokes/{roomId}/{noteId}/{pageId}")
    fun addAsync(
        @Path("roomId") roomId: UUID,
        @Path("noteId") noteId: UUID,
        @Path("pageId") pageId: UUID,
        @Body strokes: List<Stroke>
    ): Deferred<Response<List<Stroke>>>

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
     * @param strokeIds the list of stroke ID
     * @return the strokes on the page
     */
    @POST(value = "stroke/delStrokes/{roomId}/{noteId}/{pageId}")
    fun delAsync(
        @Path("roomId") roomId: UUID,
        @Path("noteId") noteId: UUID,
        @Path("pageId") pageId: UUID,
        @Body strokeIds: List<UUID>
    ): Deferred<Response<List<Stroke>>>

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

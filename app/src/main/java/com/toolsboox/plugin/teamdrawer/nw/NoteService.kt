package com.toolsboox.plugin.teamdrawer.nw

import com.toolsboox.plugin.teamdrawer.nw.domain.Note
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.*

/**
 * Note service interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface NoteService {
    /**
     * Add a new note.
     *
     * @param roomId the room ID
     * @param title the title of the note
     * @return the added note
     */
    @POST(value = "note/add/{roomId}")
    fun addAsync(
        @Path("roomId") roomId: UUID,
        @Body title: String
    ): Deferred<Response<Note>>

    /**
     * List the notes.
     *
     * @return the list of notes
     */
    @GET(value = "note/list/{roomId}")
    fun listAsync(
        @Path("roomId") roomId: UUID
    ): Deferred<Response<List<Note>>>
}

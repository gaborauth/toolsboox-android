package com.toolsboox.plugin.teamdrawer.nw

import kotlinx.coroutines.Deferred
import com.toolsboox.plugin.teamdrawer.nw.dto.NotePageComplex
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.*

/**
 * Page service interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface PageService {
    /**
     * Add a new page.
     *
     * @param roomId the room ID
     * @param noteId the note ID
     * @return the added page
     */
    @POST(value = "page/add/{roomId}/{noteId}")
    fun addAsync(
        @Path("roomId") roomId: UUID,
        @Path("noteId") noteId: UUID
    ): Deferred<Response<NotePageComplex>>
}

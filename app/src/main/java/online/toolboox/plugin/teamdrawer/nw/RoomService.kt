package online.toolboox.plugin.teamdrawer.nw

import kotlinx.coroutines.Deferred
import online.toolboox.plugin.teamdrawer.nw.domain.Room
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Room service interface.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
interface RoomService {
    /**
     * Add a new room.
     *
     * @param name the name of the room
     * @return the added room
     */
    @POST(value = "room/add")
    fun addAsync(
        @Body name: String
    ): Deferred<Response<Room>>

    /**
     * List the rooms.
     *
     * @return the list of rooms
     */
    @GET(value = "room/list")
    fun listAsync(): Deferred<Response<List<Room>>>
}
package online.toolboox.plugin.teamdrawer.nw

import online.toolboox.plugin.teamdrawer.nw.domain.Room
import java.util.*

/**
 * The 'room' repository.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class RoomRepository {

    companion object {
        /**
         * Cached room map.
         */
        private val rooms: MutableMap<UUID, Room> = mutableMapOf()
    }


    /**
     * Get the cached room.
     *
     * @param roomId the room ID
     * @return the room
     */
    fun getRoom(roomId: UUID): Room? {
        return rooms[roomId]
    }

    /**
     * Get the cached room list.
     *
     * @return the room list
     */
    fun getRoomList(): List<Room> {
        return rooms.values.toList()
    }

    /**
     * Update the cached room.
     *
     * @param newRoom the new rooms
     */
    fun updateRoom(newRoom: Room) {
        rooms[newRoom.roomId] = newRoom
    }

    /**
     * Update the cached room list.
     *
     * @param newRooms the new rooms
     */
    fun updateRoomList(newRooms: List<Room>) {
        rooms.clear()
        newRooms.forEach {
            rooms[it.roomId] = it
        }
    }
}

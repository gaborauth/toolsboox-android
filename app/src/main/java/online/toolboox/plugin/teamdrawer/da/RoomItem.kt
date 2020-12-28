package online.toolboox.plugin.teamdrawer.da

import java.util.*

/**
 * Room item data class.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
data class RoomItem(
    val roomId: UUID,
    val created: Date,
    val lastUpdated: Date,
    val name: String,
    val imageRes: Int
)

package online.toolboox.plugin.teamdrawer.nw.domain

import java.util.*

/**
 * Room data class.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
data class Room(
    val roomId: UUID,
    val created: Date,
    val lastUpdated: Date,
    val name: String
)

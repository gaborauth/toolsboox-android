package online.toolboox.plugin.teamdrawer.nw.domain

import java.util.*

/**
 * Note data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class Note(
    val roomId: UUID,
    val noteId: UUID,
    val created: Date,
    val lastUpdated: Date,
    val pages: List<UUID>,
    val title: String
)

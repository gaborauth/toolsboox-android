package online.toolboox.plugin.teamdrawer.da

import java.util.*

/**
 * Note item data class.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
data class NoteItem(
    val roomId: UUID,
    val noteId: UUID,
    val created: Date,
    val lastUpdated: Date,
    val pages: List<UUID>,
    val title: String,
    val imageRes: Int
)

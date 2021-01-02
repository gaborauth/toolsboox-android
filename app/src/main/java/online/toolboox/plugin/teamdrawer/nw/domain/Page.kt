package online.toolboox.plugin.teamdrawer.nw.domain

import java.util.*

/**
 * Page data class.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
data class Page(
    val noteId: UUID,
    val pageId: UUID,
    val created: Date,
    val lastUpdated: Date,
    val pageNumber: Int
)

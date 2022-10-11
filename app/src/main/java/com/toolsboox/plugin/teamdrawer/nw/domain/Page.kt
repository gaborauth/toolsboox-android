package com.toolsboox.plugin.teamdrawer.nw.domain

import java.util.*

/**
 * Page data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class Page(
    val noteId: UUID,
    val pageId: UUID,
    val created: Date,
    val lastUpdated: Date,
    val pageNumber: Int
)

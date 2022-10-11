package com.toolsboox.plugin.teamdrawer.nw.dto

import com.toolsboox.plugin.teamdrawer.nw.domain.Note
import com.toolsboox.plugin.teamdrawer.nw.domain.Page

/**
 * Note-page complex response data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class NotePageComplex(
    val note: Note,
    val page: Page
)

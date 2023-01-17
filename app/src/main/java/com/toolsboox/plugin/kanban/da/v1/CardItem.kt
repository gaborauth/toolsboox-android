package com.toolsboox.plugin.kanban.da.v1

import com.squareup.moshi.JsonClass
import com.toolsboox.da.Stroke
import java.util.*

/**
 * Card item data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class CardItem(
    var id: UUID,
    var version: Int,

    var lane: Int,
    var dueDate: Date,
    var doneDate: Date?,
    val strokes: MutableList<Stroke>,
)

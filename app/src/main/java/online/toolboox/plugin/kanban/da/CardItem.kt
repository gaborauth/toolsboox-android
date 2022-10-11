package online.toolboox.plugin.kanban.da

import online.toolboox.plugin.teamdrawer.nw.domain.Stroke
import java.util.*

/**
 * Card item data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class CardItem(
    var id: UUID,
    var version: Int,

    var lane: Int,
    var dueDate: Date,
    var doneDate: Date?,
    val strokes: MutableList<Stroke>,
)

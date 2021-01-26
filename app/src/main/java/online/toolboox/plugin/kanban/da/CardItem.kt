package online.toolboox.plugin.kanban.da

/**
 * Card item data class.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
data class CardItem(
    var id: String,
    var version: Int,
    val content: String
)

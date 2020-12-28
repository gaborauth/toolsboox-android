package online.toolboox.plugin.teamdrawer.nw.domain

import java.util.*

/**
 * Stroke data class.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
data class Stroke(
    var pageId: UUID,
    var strokeId: UUID,
    var strokePoints: List<StrokePoint>
)

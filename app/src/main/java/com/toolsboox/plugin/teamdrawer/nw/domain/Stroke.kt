package com.toolsboox.plugin.teamdrawer.nw.domain

import com.squareup.moshi.JsonClass
import java.util.*

/**
 * Stroke data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class Stroke(
    var pageId: UUID,
    var strokeId: UUID,
    var strokePoints: List<StrokePoint>
)

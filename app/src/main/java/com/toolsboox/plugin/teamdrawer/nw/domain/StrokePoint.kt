package com.toolsboox.plugin.teamdrawer.nw.domain

import com.squareup.moshi.JsonClass

/**
 * Stroke point data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class StrokePoint(
    var x: Float,
    var y: Float,
    var p: Float
)

package com.toolsboox.da

import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * Text element data class for on-canvas text annotations.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class TextElement(
    var elementId: UUID = UUID.randomUUID(),
    var timestamp: Long = System.currentTimeMillis(),
    var x: Float,
    var y: Float,
    var width: Float = 300f,
    var height: Float = 60f,
    var text: String,
    var fontFamily: String = "atkinson_hyperlegible",
    var fontSize: Float = 24f,
    var color: Int = -16777216  // Color.BLACK as ARGB int
)

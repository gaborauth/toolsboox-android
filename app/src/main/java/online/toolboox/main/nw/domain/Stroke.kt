package online.toolboox.main.nw.domain

import java.util.*

data class Stroke(
    var id: UUID,
    var strokePoints: List<StrokePoint>
)

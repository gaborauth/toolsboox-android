package com.toolsboox.plugin.calendar.da

import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import java.util.*

/**
 * Calendar week data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class CalendarWeek(
    var year: Int,
    var weekOfYear: Int,
    var locale: Locale?,

    override val strokes: List<Stroke>
) : Calendar

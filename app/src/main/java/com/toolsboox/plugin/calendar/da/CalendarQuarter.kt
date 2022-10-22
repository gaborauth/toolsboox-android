package com.toolsboox.plugin.calendar.da

import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke

/**
 * Calendar quarter data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class CalendarQuarter(
    var year: Int,
    var quarter: Int,

    override val strokes: List<Stroke>
) : Calendar

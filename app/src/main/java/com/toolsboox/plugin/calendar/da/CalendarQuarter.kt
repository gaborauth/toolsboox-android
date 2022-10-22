package com.toolsboox.plugin.calendar.da

import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import java.util.*

/**
 * Calendar quarter data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class CalendarQuarter(
    val year: Int,
    val quarter: Int,
    val locale: Locale?,

    override val strokes: List<Stroke>
) : Calendar

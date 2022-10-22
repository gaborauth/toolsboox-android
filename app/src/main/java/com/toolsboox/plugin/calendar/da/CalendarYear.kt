package com.toolsboox.plugin.calendar.da

import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import java.util.*

/**
 * Calendar year data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class CalendarYear(
    val year: Int,
    val locale: Locale?,

    override val strokes: List<Stroke>
) : Calendar

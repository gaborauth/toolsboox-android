package com.toolsboox.plugin.calendar.da

import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import java.util.*

/**
 * Calendar month data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class CalendarMonth(
    val year: Int,
    val month: Int,
    val locale: Locale?,

    override val strokes: List<Stroke>
) : Calendar

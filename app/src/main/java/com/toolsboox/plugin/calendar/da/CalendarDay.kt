package com.toolsboox.plugin.calendar.da

import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import java.util.*

/**
 * Calendar day data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class CalendarDay(
    val year: Int,
    val month: Int,
    val day: Int,
    val locale: Locale?,

    override val strokes: List<Stroke>
) : Calendar

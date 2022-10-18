package com.toolsboox.plugin.calendar.da

import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke

/**
 * Calendar day data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class CalendarDay(
    val withTasks: Boolean,
    val withNotes: Boolean,
    val withHours: Boolean,
    val startHours: Int,
    val strokes: List<Stroke>
)

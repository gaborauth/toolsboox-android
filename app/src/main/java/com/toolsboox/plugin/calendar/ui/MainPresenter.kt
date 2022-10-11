package com.toolsboox.plugin.calendar.ui

import com.toolsboox.plugin.calendar.nw.CalendarService
import com.toolsboox.ui.plugin.FragmentPresenter
import javax.inject.Inject

/**
 * Calendar main presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class MainPresenter @Inject constructor(
    private val calendarService: CalendarService
) : FragmentPresenter()

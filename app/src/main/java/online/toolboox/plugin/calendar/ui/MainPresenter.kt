package online.toolboox.plugin.calendar.ui

import online.toolboox.plugin.calendar.nw.CalendarService
import online.toolboox.ui.plugin.FragmentPresenter
import javax.inject.Inject

/**
 * Calendar main presenter.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class MainPresenter @Inject constructor(
    private val calendarService: CalendarService
) : FragmentPresenter()

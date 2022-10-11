package online.toolboox.plugin.templates.ui

import online.toolboox.plugin.templates.nw.TemplatesService
import online.toolboox.ui.plugin.FragmentPresenter
import javax.inject.Inject

/**
 * Templates 'this week's calendar' presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class ThisWeeksCalendarPresenter @Inject constructor(
    private val templatesService: TemplatesService
) : FragmentPresenter()

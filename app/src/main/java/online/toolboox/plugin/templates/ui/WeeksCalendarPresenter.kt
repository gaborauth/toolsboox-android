package online.toolboox.plugin.templates.ui

import online.toolboox.plugin.templates.nw.TemplatesService
import online.toolboox.ui.plugin.FragmentPresenter
import javax.inject.Inject

/**
 * Templates 'week's calendar' presenter.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class WeeksCalendarPresenter @Inject constructor(
    private val templatesService: TemplatesService
) : FragmentPresenter()

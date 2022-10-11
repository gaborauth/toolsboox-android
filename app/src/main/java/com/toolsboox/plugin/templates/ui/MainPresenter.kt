package com.toolsboox.plugin.templates.ui

import com.toolsboox.plugin.templates.nw.TemplatesService
import com.toolsboox.ui.plugin.FragmentPresenter
import javax.inject.Inject

/**
 * Templates main presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class MainPresenter @Inject constructor(
    private val templatesService: TemplatesService
) : FragmentPresenter()

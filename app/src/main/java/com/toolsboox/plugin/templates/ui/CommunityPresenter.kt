package com.toolsboox.plugin.templates.ui

import com.toolsboox.databinding.FragmentTemplatesCommunityBinding
import com.toolsboox.plugin.templates.nw.TemplatesService
import com.toolsboox.ui.plugin.FragmentPresenter
import javax.inject.Inject

/**
 * Templates 'community' presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CommunityPresenter @Inject constructor(
    private val templatesService: TemplatesService
) : FragmentPresenter() {
    /**
     * Export the community template.
     *
     * @param fragment the fragment
     * @param binding the data binding
     */
    fun export(fragment: CommunityFragment, binding: FragmentTemplatesCommunityBinding) {
    }

    /**
     * List the community templates.
     *
     * @param fragment the fragment
     * @param binding the data binding
     */
    fun list(fragment: CommunityFragment, binding: FragmentTemplatesCommunityBinding) {
        coroutinesCallHelper(
            fragment,
            { templatesService.listAsync() },
            { response ->
                when (response.code()) {
                    200 -> {
                        val list = response.body()
                        if (list == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.renderList(list)
                        }
                    }
                    else -> {
                        fragment.somethingHappened()
                    }
                }
            }
        )
    }
}

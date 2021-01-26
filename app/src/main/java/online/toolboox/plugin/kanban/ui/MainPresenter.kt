package online.toolboox.plugin.kanban.ui

import online.toolboox.ui.plugin.FragmentPresenter
import javax.inject.Inject

/**
 * Kanban planner main presenter.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class MainPresenter @Inject constructor(
) : FragmentPresenter() {

    /**
     * List the cards.
     *
     * @param fragment the fragment
     */
    fun list(fragment: MainFragment) {
        fragment.listResult(listOf())
    }
}

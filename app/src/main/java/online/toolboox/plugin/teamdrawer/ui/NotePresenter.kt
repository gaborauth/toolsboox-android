package online.toolboox.plugin.teamdrawer.ui

import online.toolboox.plugin.teamdrawer.nw.NoteService
import online.toolboox.ui.plugin.FragmentPresenter
import java.util.*
import javax.inject.Inject

/**
 * Team drawer note presenter.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class NotePresenter @Inject constructor(
    private val noteService: NoteService
) : FragmentPresenter() {

    /**
     * List the notes.
     *
     * @param fragment the fragment
     */
    fun list(fragment: NoteFragment, roomId: UUID) {
        coroutinesCallHelper(
            fragment,
            { noteService.listAsync(roomId) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.listResult(body)
                        }
                    }
                    else -> fragment.somethingHappened()
                }
            }
        )
    }
}

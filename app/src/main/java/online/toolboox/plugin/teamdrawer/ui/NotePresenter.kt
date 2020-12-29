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
     * Add a new note.
     *
     * @param fragment the fragment
     * @param roomId the room ID
     * @param title the title of the note
     */
    fun add(fragment: NoteFragment, roomId: UUID, title: String) {
        coroutinesCallHelper(
            fragment,
            { noteService.addAsync(roomId, title) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.addResult(body)
                        }
                    }
                    else -> fragment.somethingHappened()
                }
            }
        )
    }

    /**
     * List the notes.
     *
     * @param fragment the fragment
     * @param roomId the room ID
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

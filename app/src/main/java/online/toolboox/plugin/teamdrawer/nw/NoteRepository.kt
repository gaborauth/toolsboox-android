package online.toolboox.plugin.teamdrawer.nw

import online.toolboox.plugin.teamdrawer.nw.domain.Note
import java.util.*

/**
 * The 'note' repository.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class NoteRepository {

    companion object {
        /**
         * Cached notes map.
         */
        private val notes: MutableMap<UUID, MutableMap<UUID, Note>> = mutableMapOf()
    }


    /**
     * Get the cached note.
     *
     * @param roomId the room ID
     * @param noteId the note ID
     * @return the note
     */
    fun getNote(roomId: UUID, noteId: UUID): Note? {
        return (notes[roomId] ?: return null)[noteId]
    }

    /**
     * Get the cached notes list.
     *
     * @param roomId the room ID
     * @return the notes list
     */
    fun getNotesList(roomId: UUID): List<Note> {
        return (notes[roomId] ?: mutableMapOf()).values.toList()
    }

    /**
     * Update the cached note.
     *
     * @param roomId the room ID
     * @param newNote the new note
     */
    fun updateNote(roomId: UUID, newNote: Note) {
        notes[roomId] = notes[roomId] ?: mutableMapOf()
        notes[roomId]!![newNote.noteId] = newNote
    }

    /**
     * Update the cached notes list.
     *
     * @param roomId the room ID
     * @param newNotes the new notes
     */
    fun updateNotesList(roomId: UUID, newNotes: List<Note>) {
        notes[roomId] = notes[roomId] ?: mutableMapOf()
        notes[roomId]!!.clear()

        newNotes.forEach {
            notes[roomId]!![it.noteId] = it
        }
    }
}

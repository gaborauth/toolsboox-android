package online.toolboox.plugin.teamdrawer.ui

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.*
import online.toolboox.R
import online.toolboox.databinding.FragmentTeamdrawerNoteBinding
import online.toolboox.plugin.teamdrawer.da.NoteItem
import online.toolboox.plugin.teamdrawer.nw.domain.Note
import online.toolboox.plugin.teamdrawer.ot.NoteItemAdapter
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Team drawer note fragment.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
class NoteFragment @Inject constructor(
    private val presenter: NotePresenter,
    private val router: Router
) : ScreenFragment() {

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_teamdrawer_note

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTeamdrawerNoteBinding

    /**
     * The room ID.
     */
    private lateinit var roomId: UUID

    /**
     * The note item adapter.
     */
    private lateinit var adapter: NoteItemAdapter

    /**
     * The note items.
     */
    private val noteItems = mutableListOf<NoteItem>()

    /**
     * The timer job.
     */
    private lateinit var timer: Job

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTeamdrawerNoteBinding.bind(view)

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@NoteFragment.requireContext(), 4)
        }

        if (parameters["roomId"] == null) {
            somethingHappened()
            return
        }
        roomId = UUID.fromString(parameters["roomId"])

        val clickListener = object : NoteItemAdapter.OnItemClickListener {
            override fun onItemClicked(noteItem: NoteItem) {
                Timber.i("Route to ${noteItem.noteId} and ${noteItem.pages[0]}")
                router.dispatch("/teamDrawer/$roomId/${noteItem.noteId}/${noteItem.pages[0]}", false)
            }
        }

        adapter = NoteItemAdapter(this.requireContext(), noteItems, clickListener)
        binding.recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.team_drawer_note_title))

        timer = GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                presenter.list(this@NoteFragment, roomId)
                delay(30000L)
            }
        }
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        timer.cancel()
    }

    /**
     * Render the result of 'list' service call.
     *
     * @param notes the list of notes
     */
    fun listResult(notes: List<Note>) {
        noteItems.clear()
        notes.forEach {
            noteItems.add(
                NoteItem(
                    it.noteId,
                    it.roomId,
                    it.created,
                    it.lastUpdated,
                    it.pages,
                    it.title,
                    R.drawable.ic_teamdrawer_note
                )
            )
        }
        adapter.notifyDataSetChanged()
    }

    /**
     * Show the progress bar.
     */
    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    /**
     * Hide the progress bar.
     */
    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }
}

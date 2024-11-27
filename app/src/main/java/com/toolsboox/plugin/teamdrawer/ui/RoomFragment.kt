package com.toolsboox.plugin.teamdrawer.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.toolsboox.R
import com.toolsboox.databinding.FragmentTeamdrawerRoomBinding
import com.toolsboox.plugin.teamdrawer.da.RoomItem
import com.toolsboox.plugin.teamdrawer.nw.RoomRepository
import com.toolsboox.plugin.teamdrawer.nw.domain.Room
import com.toolsboox.plugin.teamdrawer.ot.RoomItemAdapter
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Team drawer room fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class RoomFragment @Inject constructor() : ScreenFragment() {

    /**
     * The Firebase analytics.
     */
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var presenter: RoomPresenter

    @Inject
    lateinit var roomRepository: RoomRepository

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_teamdrawer_room

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTeamdrawerRoomBinding

    /**
     * The room item adapter.
     */
    private lateinit var adapter: RoomItemAdapter

    /**
     * The room items.
     */
    private val roomItems = mutableListOf<RoomItem>()

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

        binding = FragmentTeamdrawerRoomBinding.bind(view)

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@RoomFragment.requireContext(), 4)
        }

        binding.fabAddItem.setOnClickListener {
            val builder = AlertDialog.Builder(this.requireContext())

            builder.setTitle(R.string.team_drawer_room_add_dialog_title)

            val input = EditText(this.requireContext())
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                presenter.add(this@RoomFragment, input.text.toString())
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            builder.create().show()
            input.requestFocus()
        }

        val clickListener = object : RoomItemAdapter.OnItemClickListener {
            override fun onItemClicked(roomItem: RoomItem) {
                val routeUrl = "/teamDrawer/${roomItem.roomId}"
                Timber.i("Route to $routeUrl")
                val bundle = bundleOf()
                bundle.putString("roomId", roomItem.roomId.toString())
                findNavController().navigate(R.id.action_to_teamdrawer_note, bundle)
            }
        }

        adapter = RoomItemAdapter(this.requireContext(), roomItems, clickListener)
        binding.recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.team_drawer_room_title))

        listResult(roomRepository.getRoomList())
        timer = GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                presenter.list(this@RoomFragment)
                delay(30000L)
            }
        }

        firebaseAnalytics.logEvent("teamdrawer") {
            param("view", "room")
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
     * Render the result of 'add' service call.
     *
     * @param roomItem the saved room
     */
    fun addResult(roomItem: Room) {
        roomRepository.updateRoom(roomItem)
        val routeUrl = "/teamDrawer/${roomItem.roomId}"
        Timber.i("Route to $routeUrl")
        val bundle = bundleOf()
        bundle.putString("roomId", roomItem.roomId.toString())
        findNavController().navigate(R.id.action_to_teamdrawer_note, bundle)
    }

    /**
     * Render the result of 'list' service call.
     *
     * @param rooms the list of rooms
     */
    fun listResult(rooms: List<Room>) {
        roomItems.clear()
        rooms.forEach {
            roomItems.add(RoomItem(it.roomId, it.created, it.lastUpdated, it.name, R.drawable.ic_teamdrawer_room))
        }
        adapter.notifyDataSetChanged()
        roomRepository.updateRoomList(rooms)
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

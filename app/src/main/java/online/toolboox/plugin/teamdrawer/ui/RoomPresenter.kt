package online.toolboox.plugin.teamdrawer.ui

import online.toolboox.plugin.teamdrawer.nw.RoomService
import online.toolboox.ui.plugin.FragmentPresenter
import javax.inject.Inject

/**
 * Team drawer room presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class RoomPresenter @Inject constructor(
    private val roomService: RoomService
) : FragmentPresenter() {

    /**
     * Add room.
     *
     * @param fragment the fragment
     * @param name the name of the room
     */
    fun add(fragment: RoomFragment, name: String) {
        coroutinesCallHelper(
            fragment,
            { roomService.addAsync(name) },
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
     * List the rooms.
     *
     * @param fragment the fragment
     */
    fun list(fragment: RoomFragment) {
        coroutinesCallHelper(
            fragment,
            { roomService.listAsync() },
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

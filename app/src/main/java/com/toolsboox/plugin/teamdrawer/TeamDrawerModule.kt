package com.toolsboox.plugin.teamdrawer

import com.toolsboox.plugin.teamdrawer.ui.NotePresenter
import com.toolsboox.plugin.teamdrawer.ui.PagePresenter
import com.toolsboox.plugin.teamdrawer.ui.RoomPresenter
import com.toolsboox.ui.plugin.FragmentPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * Team drawer plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class TeamDrawerModule {

    @Binds
    abstract fun bindNotePresenter(notePresenter: NotePresenter): FragmentPresenter

    @Binds
    abstract fun bindPagePresenter(pagePresenter: PagePresenter): FragmentPresenter

    @Binds
    abstract fun bindRoomPresenter(roomPresenter: RoomPresenter): FragmentPresenter
}

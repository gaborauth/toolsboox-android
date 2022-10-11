package com.toolsboox.plugin.teamdrawer

import dagger.Component
import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.teamdrawer.di.TeamDrawerServiceModule
import com.toolsboox.plugin.teamdrawer.ui.NoteFragment
import com.toolsboox.plugin.teamdrawer.ui.PageFragment
import com.toolsboox.plugin.teamdrawer.ui.RoomFragment
import javax.inject.Singleton

/**
 * Team drawer plugin component.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Singleton
@Component(
    modules = [
        NetworkModule::class,

        TeamDrawerModule::class,
        TeamDrawerServiceModule::class
    ]
)
interface TeamDrawerComponent {
    fun inject(plugin: TeamDrawerPlugin)

    fun noteFragment(): NoteFragment
    fun pageFragment(): PageFragment
    fun roomFragment(): RoomFragment
}

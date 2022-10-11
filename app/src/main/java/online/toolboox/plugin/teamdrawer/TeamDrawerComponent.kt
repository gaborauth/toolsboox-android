package online.toolboox.plugin.teamdrawer

import dagger.Component
import online.toolboox.di.NetworkModule
import online.toolboox.plugin.teamdrawer.di.TeamDrawerServiceModule
import online.toolboox.plugin.teamdrawer.ui.NoteFragment
import online.toolboox.plugin.teamdrawer.ui.PageFragment
import online.toolboox.plugin.teamdrawer.ui.RoomFragment
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

package online.toolboox.plugin.teamdrawer

import dagger.Component
import online.toolboox.di.NetworkModule
import online.toolboox.plugin.teamdrawer.di.TeamDrawerServiceModule
import online.toolboox.plugin.teamdrawer.ui.PageFragment
import javax.inject.Singleton

/**
 * Team drawer plugin component.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
@Singleton
@Component(
    modules = [
        TeamDrawerServiceModule::class,
        NetworkModule::class
    ]
)
interface TeamDrawerComponent {
    fun inject(plugin: TeamDrawerPlugin)
    fun fragment(): PageFragment
}

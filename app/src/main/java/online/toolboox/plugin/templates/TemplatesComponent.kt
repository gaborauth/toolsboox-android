package online.toolboox.plugin.templates

import dagger.Component
import online.toolboox.di.NetworkModule
import online.toolboox.plugin.templates.di.TemplatesServiceModule
import online.toolboox.plugin.templates.ui.MainFragment
import javax.inject.Singleton

/**
 * Templates plugin component.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
@Singleton
@Component(
    modules = [
        TemplatesModule::class,
        TemplatesServiceModule::class,
        NetworkModule::class
    ]
)
interface TemplatesComponent {
    fun inject(plugin: TemplatesPlugin)
    fun fragment(): MainFragment
}
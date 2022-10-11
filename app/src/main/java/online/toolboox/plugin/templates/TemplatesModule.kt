package online.toolboox.plugin.templates

import dagger.Module
import dagger.Provides
import online.toolboox.ui.plugin.Router
import javax.inject.Singleton

/**
 * Templates plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
class TemplatesModule(private val plugin: TemplatesPlugin, private val router: Router) {
    @Provides
    @Singleton
    fun providePlugin() = plugin

    @Provides
    @Singleton
    fun provideRouter(): Router = router
}

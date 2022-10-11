package online.toolboox.plugin.calendar

import dagger.Module
import dagger.Provides
import online.toolboox.ui.plugin.Router
import javax.inject.Singleton

/**
 * Calendar plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
class CalendarModule(private val plugin: CalendarPlugin, private val router: Router) {
    @Provides
    @Singleton
    fun providePlugin() = plugin

    @Provides
    @Singleton
    fun provideRouter(): Router = router
}

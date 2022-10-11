package com.toolsboox.plugin.templates

import dagger.Component
import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.templates.di.TemplatesServiceModule
import com.toolsboox.plugin.templates.ui.*
import javax.inject.Singleton

/**
 * Templates plugin component.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
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
    fun mainFragment(): MainFragment

    fun boxedDaysCalendarFragment(): BoxedDaysCalendarFragment
    fun boxedWeeksCalendarFragment(): BoxedWeeksCalendarFragment
    fun communityFragment(): CommunityFragment
    fun flatWeeksCalendarFragment(): FlatWeeksCalendarFragment
    fun thisWeeksCalendarFragment(): ThisWeeksCalendarFragment
}

package com.toolsboox.plugin.calendar

import com.toolsboox.plugin.calendar.ui.MainPresenter
import com.toolsboox.ui.plugin.FragmentPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * Calendar plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class CalendarModule {

    @Binds
    abstract fun bindMainPresenter(mainPresenter: MainPresenter): FragmentPresenter
}

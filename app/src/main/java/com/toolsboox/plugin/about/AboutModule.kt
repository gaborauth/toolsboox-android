package com.toolsboox.plugin.about

import com.toolsboox.plugin.about.ui.AboutPresenter
import com.toolsboox.ui.plugin.FragmentPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * About plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class AboutModule {

    @Binds
    abstract fun bindAboutPresenter(aboutPresenter: AboutPresenter): FragmentPresenter
}

package com.toolsboox.di

import com.toolsboox.ui.DefaultRouter
import com.toolsboox.ui.plugin.Router
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * Router module of routing services.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class RouterModule {

    @Binds
    abstract fun bindRouter(defaultRouter: DefaultRouter): Router
}

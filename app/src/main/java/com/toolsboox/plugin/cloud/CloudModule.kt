package com.toolsboox.plugin.cloud

import com.toolsboox.plugin.cloud.ui.CloudPresenter
import com.toolsboox.ui.plugin.FragmentPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * Cloud plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class CloudModule {

    @Binds
    abstract fun bindCloudPresenter(cloudPresenter: CloudPresenter): FragmentPresenter
}

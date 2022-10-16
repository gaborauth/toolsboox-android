package com.toolsboox.plugin.kanban

import com.toolsboox.plugin.kanban.ui.MainPresenter
import com.toolsboox.ui.plugin.FragmentPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * Kanban plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class KanbanModule {

    @Binds
    abstract fun bindMainPresenter(mainPresenter: MainPresenter): FragmentPresenter
}

package com.toolsboox.plugin.templates

import com.toolsboox.plugin.templates.ui.*
import com.toolsboox.ui.plugin.FragmentPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * Templates plugin module.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class TemplatesModule {

    @Binds
    abstract fun bindBoxedDaysCalendarPresenter(boxedDaysCalendarPresenter: BoxedDaysCalendarPresenter): FragmentPresenter

    @Binds
    abstract fun bindBoxedWeeksCalendarPresenter(boxedWeeksCalendarPresenter: BoxedWeeksCalendarPresenter): FragmentPresenter

    @Binds
    abstract fun bindCommunityPresenter(communityPresenter: CommunityPresenter): FragmentPresenter

    @Binds
    abstract fun bindFlatWeeksCalendarPresenter(flatWeeksCalendarPresenter: FlatWeeksCalendarPresenter): FragmentPresenter

    @Binds
    abstract fun bindTemplatesMainPresenter(templatesMainPresenter: TemplatesMainPresenter): FragmentPresenter

    @Binds
    abstract fun bindThisWeeksCalendarPresenter(thisWeeksCalendarPresenter: ThisWeeksCalendarPresenter): FragmentPresenter
}

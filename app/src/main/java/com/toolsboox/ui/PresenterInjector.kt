package com.toolsboox.ui

import dagger.BindsInstance
import dagger.Component
import com.toolsboox.di.NetworkModule
import com.toolsboox.di.MainSharedPreferencesModule
import com.toolsboox.plugin.teamdrawer.di.TeamDrawerServiceModule
import com.toolsboox.ui.main.MainPresenter
import javax.inject.Singleton

/**
 * Component providing inject() methods for presenters.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Singleton
@Component(
    modules = [
        (NetworkModule::class),
        (MainSharedPreferencesModule::class),
        (TeamDrawerServiceModule::class)
    ]
)
interface PresenterInjector {

    /**
     * Injects required dependencies into the specified MainPresenter.
     *
     * @param mainPresenter MainPresenter in which to inject the dependencies
     */
    fun inject(mainPresenter: MainPresenter)

    /**
     * Builder component.
     */
    @Component.Builder
    interface Builder {
        fun build(): PresenterInjector

        fun networkModule(networkModule: NetworkModule): Builder
        fun mainSharedPreferencesModule(mainSharedPreferencesModule: MainSharedPreferencesModule): Builder

        @BindsInstance
        fun baseView(baseView: BaseView): Builder
    }
}

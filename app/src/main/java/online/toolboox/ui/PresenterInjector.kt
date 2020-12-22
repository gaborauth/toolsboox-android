package online.toolboox.ui

import dagger.BindsInstance
import dagger.Component
import online.toolboox.di.NetworkModule
import online.toolboox.main.di.MainServiceModule
import online.toolboox.main.di.MainSharedPreferencesModule
import online.toolboox.main.ui.MainPresenter
import javax.inject.Singleton

/**
 * Component providing inject() methods for presenters.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
@Singleton
@Component(
    modules = [
        (NetworkModule::class),
        (MainSharedPreferencesModule::class),
        (MainServiceModule::class)
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
        fun mainServiceModule(mainService: MainServiceModule): Builder

        @BindsInstance
        fun baseView(baseView: BaseView): Builder
    }
}

package online.toolboox.ui

import kotlinx.coroutines.*
import online.toolboox.di.NetworkModule
import online.toolboox.plugin.teamdrawer.di.TeamDrawerServiceModule
import online.toolboox.main.di.MainSharedPreferencesModule
import online.toolboox.plugin.teamdrawer.nw.StrokeService
import online.toolboox.main.ui.MainPresenter
import javax.inject.Inject

/**
 * Base presenter, provides initial injections and required methods.
 *
 * @param V the type of the View the presenter is based on
 * @property view the view the presenter is based on
 * @constructor Injects the required dependencies
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
abstract class BasePresenter<out V : BaseView>(protected val view: V) {

    /**
     * Inject the stroke service.
     */
    @Inject
    lateinit var strokeService: StrokeService

    /**
     * The injector used to inject required dependencies.
     */
    private val injector: PresenterInjector = DaggerPresenterInjector
        .builder()
        .baseView(view)
        .networkModule(NetworkModule)
        .mainSharedPreferencesModule(MainSharedPreferencesModule)
        .teamDrawerServiceModule(TeamDrawerServiceModule)
        .build()

    /**
     * Inject.
     */
    init {
        inject()
    }

    /**
     * Coroutines based call helper with default onError.
     *
     * @param call the backend call
     * @param onSuccess the success path function
     */
    protected fun <T> coroutinesCallHelper(
        call: () -> Deferred<T>,
        onSuccess: (response: T) -> Unit
    ) = coroutinesCallHelper(call, onSuccess,
        { t -> view.somethingHappened(t) })


    /**
     * Coroutines based call helper.
     *
     * @param call the backend call
     * @param onSuccess the success path function
     * @param onError the custom onError function
     */
    protected fun <T> coroutinesCallHelper(
        call: () -> Deferred<T>,
        onSuccess: (response: T) -> Unit,
        onError: (t: Throwable) -> Unit
    ) = GlobalScope.launch(Dispatchers.Main) {
        try {
            view.showLoading()
            onSuccess(call().await())
        } catch (t: Throwable) {
            onError(t)
        } finally {
            view.hideLoading()
        }
    }

    /**
     * This method may be called when the presenter view is created.
     */
    open fun onViewCreated() {}

    /**
     * This method may be called when the presenter view is destroyed.
     */
    open fun onViewDestroyed() {}

    /**
     * Injects the required dependencies.
     */
    private fun inject() {
        when (this) {
            is MainPresenter -> injector.inject(this)
        }
    }
}

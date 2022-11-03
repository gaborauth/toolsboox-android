package com.toolsboox.ui

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Base presenter, provides initial injections and required methods.
 *
 * @param V the type of the View the presenter is based on
 * @property view the view the presenter is based on
 * @constructor Injects the required dependencies
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
abstract class BasePresenter<out V : BaseView>(private val view: V) {
    /**
     * Coroutines based call helper with default onError.
     *
     * @param call the backend call
     * @param onSuccess the success path function
     */
    protected fun <T> coroutinesCallHelper(
        call: () -> Deferred<T>,
        onSuccess: (response: T) -> Unit
    ) = coroutinesCallHelper(call, onSuccess
    ) { t -> view.somethingHappened(t) }


    /**
     * Coroutines based call helper.
     *
     * @param call the backend call
     * @param onSuccess the success path function
     * @param onError the custom onError function
     */
    private fun <T> coroutinesCallHelper(
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
}

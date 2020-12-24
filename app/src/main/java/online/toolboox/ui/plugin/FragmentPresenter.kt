package online.toolboox.ui.plugin

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Response

/**
 * Base class of fragment presenters.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
abstract class FragmentPresenter {

    /**
     * Coroutines based call helper with default onError.
     *
     * @param fragment the fragment
     * @param call the backend call
     * @param onSuccess the success path function
     */
    protected fun <T> coroutinesCallHelper(
        fragment: ScreenFragment,
        call: () -> Deferred<Response<T>>,
        onSuccess: (response: Response<T>) -> Unit
    ) = coroutinesCallHelper(fragment, call, onSuccess, { t -> fragment.somethingHappened(t) })

    /**
     * Coroutines based call helper.
     *
     * @param fragment the fragment
     * @param call the backend call
     * @param onSuccess the success path function
     * @param onError the custom onError function
     */
    protected fun <T> coroutinesCallHelper(
        fragment: ScreenFragment,
        call: () -> Deferred<Response<T>>,
        onSuccess: (response: Response<T>) -> Unit,
        onError: (t: Throwable) -> Unit
    ) = GlobalScope.launch(Dispatchers.Main) {
        try {
            fragment.runOnActivity {
                fragment.showLoading()
            }

            onSuccess(call().await())
        } catch (t: Throwable) {
            onError(t)
        } finally {
            fragment.runOnActivity {
                fragment.hideLoading()
            }
        }
    }
}

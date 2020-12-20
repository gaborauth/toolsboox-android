package online.toolboox.ui

import android.content.Context
import androidx.annotation.StringRes
import online.toolboox.R

/**
 * Base view interface.
 *
 * @author <a href="mailto:auth.gabor@gmail.com">Gábor AUTH</a>
 */
interface BaseView {

    /**
     * Returns the context in which the application is running.
     *
     * @return the context
     */
    fun getContext(): Context

    /**
     * Show the 'something happened' error...
     */
    fun somethingHappened(t: Throwable? = null) {
        showError(t, R.string.something_happened_error)
    }

    /**
     * Displays an error in the view.
     *
     * @param t the optional throwable
     * @param errorResId the resource id of the error
     */
    fun showError(t: Throwable?, @StringRes errorResId: Int)

    /**
     * Displays an error in the view.
     *
     * @param messageResId the resource id of the error
     */
    fun showMessage(@StringRes messageResId: Int)

    /**
     * Displays the loading indicator of the view.
     */
    fun showLoading()

    /**
     * Hides the loading indicator of the view.
     */
    fun hideLoading()
}

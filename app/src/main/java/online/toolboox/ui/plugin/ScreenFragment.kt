package online.toolboox.ui.plugin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import online.toolboox.R
import timber.log.Timber

/**
 * The fragment base class.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
abstract class ScreenFragment : Fragment() {
    init {
        retainInstance = true
    }

    /**
     * The progress bar of the parent activity
     */
    lateinit var toolBar: Toolbar

    /**
     * The view resource.
     */
    protected open val view: Int? = null

    /**
     * Map of parameters.
     */
    protected var parameters: Map<String, String> = mapOf()

    /**
     * OnCreateView hook.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return view?.let { inflater.inflate(it, container, false) }
    }

    /**
     * Consume parameters from the URL.
     *
     * @param parameters the parameter map
     * @return returns with the fragment
     */
    open fun setParameters(parameters: Map<String, String>): ScreenFragment {
        this.parameters = parameters

        return this
    }

    /**
     * Show the 'something happened' error...
     */
    fun somethingHappened(t: Throwable? = null) {
        runOnActivity {
            showError(t, R.string.something_happened_error)
        }
    }

    /**
     * Displays an error in the view.
     *
     * @param t the optional throwable
     * @param errorResId the resource id of the error
     */
    open fun showError(t: Throwable?, @StringRes errorResId: Int) {
        t?.let { Timber.e(it, getString(errorResId)) }

        Snackbar.make(toolBar, errorResId, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.something_happened_action) {}
            .show()
    }

    /**
     * Displays an error in the view.
     *
     * @param messageResId the resource id of the error
     */
    open fun showMessage(@StringRes messageResId: Int) {
        Snackbar.make(toolBar, messageResId, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Call the function on the foreground activity only.
     *
     * @param call the function
     */
    fun runOnActivity(call: () -> Unit) {
        activity?.let {
            if (isAdded) call()
        }
    }

    /**
     * Displays the loading indicator of the view.
     */
    abstract fun showLoading()

    /**
     * Hides the loading indicator of the view.
     */
    abstract fun hideLoading()
}

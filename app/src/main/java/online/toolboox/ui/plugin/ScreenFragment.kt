package online.toolboox.ui.plugin

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    /**
     * Result code of WRITE_EXTERNAL_STORAGE permission.
     */
    protected val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 12345

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

    /**
     * Result of request permission.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this.context, "Permission Granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this.context, "Permission Denied!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Check if permission granted or start the request process.
     *
     * @param permissionName the name of the permission (Manifest.permission)
     * @param permissionCode the result code of the request Activity
     * @param title the title of the request dialog
     * @param message the message of the request dialog
     * @return true, if the permission is granted
     */
    protected fun checkPermissionGranted(
        permissionName: String,
        permissionRequestCode: Int,
        title: String,
        message: String
    ): Boolean {
        val permission = ContextCompat.checkSelfPermission(this.requireContext(), permissionName)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this.requireActivity(), permissionName)) {
                showExplanation(title, message, permissionName, permissionRequestCode)
            } else {
                requestPermission(permissionName, permissionRequestCode)
            }

            return false
        }

        return true
    }

    /**
     * Show explanation of the permission request.
     *
     * @param title the title
     * @param message the message
     * @param permission the permission
     * @param permissionRequestCode the request code of the dialog
     */
    private fun showExplanation(title: String, message: String, permission: String, permissionRequestCode: Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                android.R.string.ok,
                { dialog, id -> requestPermission(permission, permissionRequestCode) }
            )
        builder.create().show()
    }

    /**
     * Request a permission.
     *
     * @param permissionName the name of the permission
     * @param permissionRequestCode the request code of the dialog
     */
    private fun requestPermission(permissionName: String, permissionRequestCode: Int) {
        ActivityCompat.requestPermissions(this.requireActivity(), arrayOf(permissionName), permissionRequestCode)
    }
}

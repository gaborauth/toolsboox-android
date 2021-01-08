package online.toolboox.ui.plugin

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import online.toolboox.R
import online.toolboox.databinding.ToolbarBinding
import timber.log.Timber

/**
 * The fragment base class.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
abstract class ScreenFragment : Fragment() {
    companion object {

        /**
         * Result code of READ_EXTERNAL_STORAGE permission.
         */
        const val REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 12345

        /**
         * Result code of WRITE_EXTERNAL_STORAGE permission.
         */
        const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 12346
    }

    init {
        retainInstance = true
    }

    /**
     * The toolbar of the parent activity
     */
    lateinit var toolBar: ToolbarBinding

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
     *
     * @param t the optional throwable
     * @param parentView the optional parent view of the snackbar
     */
    fun somethingHappened(t: Throwable? = null, parentView: View? = null) {
        runOnActivity {
            showError(t, R.string.something_happened_error, parentView)
        }
    }

    /**
     * Displays an error in the view.
     *
     * @param t the optional throwable
     * @param errorResId the resource id of the error
     * @param parentView the optional parent view of the snackbar
     */
    open fun showError(t: Throwable?, @StringRes errorResId: Int, parentView: View? = null) {
        t?.let { Timber.e(it, getString(errorResId)) }

        val snackbar =
            Snackbar.make(parentView?.let { parentView } ?: toolBar.root, errorResId, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(R.string.something_happened_action) {}
        snackbar.show()
    }

    /**
     * Displays an error in the view.
     *
     * @param message the message
     * @param parentView the optional parent view of the snackbar
     */
    open fun showMessage(message: String, parentView: View? = null) {
        Snackbar.make(parentView?.let { parentView } ?: toolBar.root, message, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Displays an error in the view.
     *
     * @param messageResId the resource id of the error
     * @param parentView the optional parent view of the snackbar
     */
    open fun showMessage(@StringRes messageResId: Int, parentView: View? = null) {
        showMessage(getString(messageResId), parentView)
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
     * @param permissionRequestCode the result code of the request Activity
     * @param title the title of the request dialog
     * @param message the message of the request dialog
     * @return true, if the permission is granted
     */
    fun checkPermissionGranted(
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
     * @param permissionName the name of the permission (Manifest.permission)
     * @param permissionRequestCode the request code of the dialog
     */
    private fun showExplanation(title: String, message: String, permissionName: String, permissionRequestCode: Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ -> requestPermission(permissionName, permissionRequestCode) }
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

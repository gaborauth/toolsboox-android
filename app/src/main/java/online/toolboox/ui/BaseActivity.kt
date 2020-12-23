package online.toolboox.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import online.toolboox.BuildConfig

/**
 * Base activity, it provides required methods and presenter instantiation and calls.
 * @param P the type of the presenter the Activity is based on
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
 */
abstract class BaseActivity<P : BasePresenter<BaseView>> : BaseView, AppCompatActivity() {

    /**
     * The presenter instance.
     */
    protected lateinit var presenter: P

    /**
     * Result of WRITE_EXTERNAL permission.
     */
    protected val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 12345

    /**
     * OnCreate hook.
     *
     * @param savedInstanceState the saved state of the instance
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        presenter = presenter()
    }

    /**
     * Instantiates the presenter the Activity is based on.
     */
    protected abstract fun presenter(): P

    /**
     * Get the context.
     *
     * @return the context
     */
    override fun getContext(): Context {
        return this
    }

    /**
     * Result of request permission.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Show explanation of the permission request.
     *
     * @param title the title
     * @param message the message
     * @param permission the permission
     * @param permissionRequestCode the request code of the dialog
     */
    protected fun showExplanation(
        title: String,
        message: String,
        permission: String,
        permissionRequestCode: Int
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
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
    protected fun requestPermission(permissionName: String, permissionRequestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permissionName), permissionRequestCode)
    }
}

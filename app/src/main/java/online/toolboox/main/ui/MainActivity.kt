package online.toolboox.main.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.onyx.android.sdk.scribble.provider.BaseNoteProvider
import com.onyx.android.sdk.scribble.provider.RemoteNoteProvider
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.config.ShapeGeneratedDatabaseHolder
import kotlinx.android.synthetic.main.activity_main.*
import online.toolboox.BuildConfig
import online.toolboox.R
import online.toolboox.main.di.MainSharedPreferencesModule
import online.toolboox.ui.BaseActivity
import online.toolboox.utils.ReleaseTree
import timber.log.Timber
import java.util.*

/**
 * A dashboard screen that offers the main menu.
 *
 * @author <a href="mailto:auth.gabor@gmail.com">Gábor AUTH</a>
 */
class MainActivity : BaseActivity<MainPresenter>(), MainView {

    /**
     * The Firebase analytics.
     */
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The back URL from intent extra.
     */
    private var backUrl: String? = null

    /**
     * The note provider.
     */
    private lateinit var noteProvider: BaseNoteProvider

    /**
     * OnCreate hook.
     *
     * @param savedInstanceState the saved state of the instance
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)

        title = getString(R.string.drawer_title).format(getString(R.string.app_name), getString(R.string.main_title))

        backUrl = intent.getStringExtra("backUrl")

        val preferences = MainSharedPreferencesModule.provideSharedPreferences(this)
        preferences.edit().putLong("lastTimestamp", Date().time).apply()

        FlowManager.init(FlowConfig.builder(this).addDatabaseHolder(ShapeGeneratedDatabaseHolder::class.java).build())
        noteProvider = RemoteNoteProvider()
        val notes = noteProvider.loadAllNoteList()

        notes.stream().forEach {
            Timber.i(
                "uniqueId=%s, parentUniqueId=%s, isLibrary=%s, title=%s",
                it.uniqueId, it.parentUniqueId, it.isLibrary, it.title
            )
        }

        resetForm()
        mainMessage.visibility = View.VISIBLE
    }

    /**
     * Reset the form.
     */
    private fun resetForm() {
        mainLogo.visibility = View.VISIBLE
    }

    /**
     * Displays an error in the view.
     *
     * @param t the optional throwable
     * @param errorResId the resource id of the error
     */
    override fun showError(t: Throwable?, @StringRes errorResId: Int) {
        t?.let { Timber.e(it, getString(errorResId)) }
    }

    /**
     * Displays an error in the view.
     *
     * @param messageResId the resource id of the error
     */
    override fun showMessage(@StringRes messageResId: Int) {
        Snackbar.make(mainToolbar, messageResId, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Show progress and hide login form.
     */
    override fun showLoading() {
        mainProgress.visibility = View.VISIBLE
    }

    /**
     * Hide progress and show login form.
     */
    override fun hideLoading() {
        mainProgress.visibility = View.INVISIBLE
    }

    /**
     * Instantiate the presenter.
     */
    override fun presenter(): MainPresenter {
        return MainPresenter(this)
    }
}

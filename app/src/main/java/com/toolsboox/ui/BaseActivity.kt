package com.toolsboox.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.toolsboox.BuildConfig

/**
 * Base activity, it provides required methods and presenter instantiation and calls.
 * @param P the type of the presenter the Activity is based on
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
abstract class BaseActivity<P : BasePresenter<BaseView>> : BaseView, AppCompatActivity() {

    /**
     * The presenter instance.
     */
    protected lateinit var presenter: P

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
}

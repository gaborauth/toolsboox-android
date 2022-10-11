package online.toolboox.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Timber release tree implementation.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class ReleaseTree : Timber.Tree() {

    /**
     * Overrode log method to send reports to the Crashlytics.
     *
     * @param priority the priority
     * @param tag the tag
     * @param message the message
     * @param t the throwable
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t == null) {
            if (priority == Log.ERROR || priority == Log.WARN) {
                FirebaseCrashlytics.getInstance().log("$tag: $message")
            }
        } else {
            FirebaseCrashlytics.getInstance().recordException(t)
        }
    }
}

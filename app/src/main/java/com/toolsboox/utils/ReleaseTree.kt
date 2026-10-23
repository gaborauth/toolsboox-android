package com.toolsboox.utils

import android.util.Log
import timber.log.Timber

class ReleaseTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.WARN) {
            Log.println(priority, tag ?: "ToolsBoox", message)
        }
    }
}

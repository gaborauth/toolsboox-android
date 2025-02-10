package com.toolsboox.plugin.dashboard.ui

import androidx.lifecycle.lifecycleScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.toolsboox.BuildConfig
import com.toolsboox.plugin.dashboard.nw.DashboardService
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Dashboard presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class DashboardPresenter @Inject constructor() : FragmentPresenter() {

    /**
     * The dashboard service.
     */
    @Inject
    lateinit var dashboardService: DashboardService

    /**
     * Get a value of the parameter by key.
     *
     * @param fragment the fragment
     * @return the value of the parameters
     */
    fun parameters(fragment: DashboardFragment) {
        coroutinesCallHelper(
            fragment,
            { dashboardService.parametersAsync() },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(true)
                        } else {
                            fragment.parameterResult(body)
                        }
                    }

                    else -> fragment.somethingHappened(true)
                }
            },
            true
        )
    }

    /**
     * Get the server API version.
     *
     * @param fragment the fragment
     * @return the server API version
     */
    fun version(fragment: DashboardFragment) {
        coroutinesCallHelper(
            fragment,
            { dashboardService.versionAsync("${BuildConfig.VERSION_CODE}") },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened(true)
                        } else {
                            fragment.versionResult(body)
                        }
                    }

                    else -> fragment.somethingHappened(true)
                }
            },
            true
        )
    }

    /**
     * Download the ink recognition model.
     *
     * @param fragment the fragment
     * @param languageTag the language tag
     */
    fun downloadInkRecognition(fragment: DashboardFragment, languageTag: String) {
        val remoteModelManager = RemoteModelManager.getInstance()
        DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)?.let { mi ->
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val model = DigitalInkRecognitionModel.builder(mi).build()
                if (!remoteModelManager.isModelDownloaded(model).await()) {
                    remoteModelManager.download(model, DownloadConditions.Builder().build())
                        .addOnSuccessListener {
                            Timber.i("Model downloaded")
                        }
                        .addOnFailureListener { e: Exception ->
                            Timber.e(e, "Error while downloading a model")
                        }
                } else {
                    Timber.i("Model already downloaded")
                }
            }
        }
    }
}

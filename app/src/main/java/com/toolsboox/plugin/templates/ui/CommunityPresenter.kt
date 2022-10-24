package com.toolsboox.plugin.templates.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.toolsboox.R
import com.toolsboox.databinding.FragmentTemplatesCommunityBinding
import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.templates.da.CommunityTemplate
import com.toolsboox.plugin.templates.nw.TemplatesService
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.inject.Inject

/**
 * Templates 'community' presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CommunityPresenter @Inject constructor() : FragmentPresenter() {

    @Inject
    lateinit var templatesService: TemplatesService

    /**
     * Export the community template.
     *
     * @param fragment the fragment
     * @param binding the data binding
     */
    fun export(fragment: CommunityFragment, binding: FragmentTemplatesCommunityBinding, item: CommunityTemplate) {
        if (!fragment.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            fragment.showError(null, R.string.main_read_external_storage_permission_missing, binding.root)
            return
        }

        if (!fragment.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            fragment.showError(null, R.string.main_write_external_storage_permission_missing, binding.root)
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val rootPath = Environment.getExternalStorageDirectory()
                val filename = "$rootPath/noteTemplate/${item.templateUri}"
                val url = URL(NetworkModule.GITHUB_BASE_URL + "communityTemplates/" + item.templateUri)
                val bitmap = BitmapFactory.decodeStream(url.content as InputStream)
                FileOutputStream(filename).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    withContext(Dispatchers.Main) {
                        binding.exportMessage.text = fragment.getString(R.string.export_message, item.templateUri)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) { binding.exportMessage.text = e.toString() }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * List the community templates.
     *
     * @param fragment the fragment
     */
    fun list(fragment: CommunityFragment) {
        coroutinesCallHelper(
            fragment,
            { templatesService.listAsync() },
            { response ->
                when (response.code()) {
                    200 -> {
                        val list = response.body()
                        if (list == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.renderList(list)
                        }
                    }

                    else -> {
                        fragment.somethingHappened()
                    }
                }
            }
        )
    }
}

package com.toolsboox.plugin.templates.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.toolsboox.R
import com.toolsboox.databinding.FragmentTemplatesCommunityBinding
import com.toolsboox.di.NetworkModule
import com.toolsboox.plugin.templates.da.CommunityTemplate
import com.toolsboox.plugin.templates.nw.CommunityTemplatesRepository
import com.toolsboox.plugin.templates.ot.CommunityTemplatesItemAdapter
import com.toolsboox.ui.plugin.ScreenFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.net.URL
import javax.inject.Inject

/**
 * Templates 'community' fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CommunityFragment @Inject constructor() : ScreenFragment() {

    @Inject
    lateinit var presenter: CommunityPresenter

    @Inject
    lateinit var communityTemplatesRepository: CommunityTemplatesRepository

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_templates_community

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTemplatesCommunityBinding

    /**
     * The selected item.
     */
    private var selectedItem: CommunityTemplate? = null

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTemplatesCommunityBinding.bind(view)

        binding.buttonList.isChecked = true
        binding.buttonList.setOnClickListener {
            binding.listPane.visibility = View.VISIBLE
            binding.previewPane.visibility = View.GONE
            binding.exportPane.visibility = View.GONE

            presenter.list(this)
            renderList(communityTemplatesRepository.list())
        }
        binding.buttonPreview.setOnClickListener {
            binding.listPane.visibility = View.GONE
            binding.previewPane.visibility = View.VISIBLE
            binding.exportPane.visibility = View.GONE
        }
        binding.buttonExport.setOnClickListener {
            binding.listPane.visibility = View.GONE
            binding.previewPane.visibility = View.GONE
            binding.exportPane.visibility = View.VISIBLE

            if (selectedItem != null) presenter.export(this, binding, selectedItem!!)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(view.context)
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.HORIZONTAL))

        binding.recyclerView.adapter = CommunityTemplatesItemAdapter(context, listOf()) {
            selectedItem = it
            binding.preview.setImageResource(R.mipmap.image_not_found)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(NetworkModule.GITHUB_BASE_URL + "communityTemplates/" + it.templateUri)
                    val bitmap = BitmapFactory.decodeStream(url.content as InputStream)
                    withContext(Dispatchers.Main) {
                        binding.preview.setImageBitmap(bitmap)
                    }
                } catch (t: Throwable) {
                    Timber.e(t)
                }
            }
            binding.buttonPreview.performClick()
        }

        binding.buttonList.performClick()
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolBar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.templates_community_title))
    }

    /**
     * Render the list of community templates.
     *
     * @param list list of community templates
     */
    fun renderList(list: List<CommunityTemplate>) {
        Timber.i("List: $list")
        if (list.isEmpty()) {
            binding.fragmentSwipeContainer.visibility = View.GONE
            binding.fragmentEmptyPlaceholder.visibility = View.VISIBLE

            binding.fragmentEmptyMessage.text = getString(R.string.list_empty)
        } else {
            binding.fragmentSwipeContainer.visibility = View.VISIBLE
            binding.fragmentEmptyPlaceholder.visibility = View.GONE
        }
        (binding.recyclerView.adapter as CommunityTemplatesItemAdapter).update(list.sortedBy { item -> item.name })

        communityTemplatesRepository.updateList(list)
    }

    /**
     * Show the progress bar.
     */
    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    /**
     * Hide the progress bar.
     */
    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }
}

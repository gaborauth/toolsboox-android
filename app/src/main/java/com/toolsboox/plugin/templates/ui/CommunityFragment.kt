package com.toolsboox.plugin.templates.ui

import android.os.Bundle
import android.view.View
import com.toolsboox.R
import com.toolsboox.databinding.FragmentTemplatesCommunityBinding
import com.toolsboox.plugin.templates.da.CommunityTemplate
import com.toolsboox.ui.plugin.Router
import com.toolsboox.ui.plugin.ScreenFragment
import timber.log.Timber
import javax.inject.Inject

/**
 * Templates 'community' fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CommunityFragment @Inject constructor(
    private val presenter: CommunityPresenter,
    private val router: Router
) : ScreenFragment() {

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_templates_community

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTemplatesCommunityBinding

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

            presenter.list(this, binding)
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

            presenter.export(this, binding)
        }

        binding.preview.post {
            presenter.list(this, binding)
        }
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
        Timber.i("$list")
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

package online.toolboox.plugin.templates.ui

import android.os.Bundle
import android.view.View
import online.toolboox.R
import online.toolboox.databinding.FragmentTemplatesCommunityBinding
import online.toolboox.plugin.templates.da.CommunityTemplate
import online.toolboox.ui.plugin.Router
import online.toolboox.ui.plugin.ScreenFragment
import timber.log.Timber
import javax.inject.Inject

/**
 * Templates 'community' fragment.
 *
 * @author <a href="mailto:gabor.auth@toolboox.online">Gábor AUTH</a>
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

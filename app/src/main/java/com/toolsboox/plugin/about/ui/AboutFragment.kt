package com.toolsboox.plugin.about.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import com.toolsboox.R
import com.toolsboox.databinding.FragmentAboutBinding
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * About main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class AboutFragment @Inject constructor() : ScreenFragment() {

    /**
     * The injected presenter.
     */
    @Inject
    lateinit var presenter: AboutPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_about

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentAboutBinding

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAboutBinding.bind(view)

        val aboutMeLink = "https://github.com/gaborauth/"
        val aboutMeLinkHtml = "<a href=\"$aboutMeLink\">$aboutMeLink</a>"
        binding.aboutMeLink.text = Html.fromHtml(aboutMeLinkHtml, Html.FROM_HTML_MODE_COMPACT)
        binding.aboutMeLink.setOnClickListener {
            this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(aboutMeLink)))
        }

        val projectHomeLink = "https://github.com/gaborauth/toolsboox-android/"
        val projectHomeLinkHtml = "<a href=\"$projectHomeLink\">$projectHomeLink</a>"
        binding.projectHomeLink.text = Html.fromHtml(projectHomeLinkHtml, Html.FROM_HTML_MODE_COMPACT)
        binding.projectHomeLink.setOnClickListener {
            this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(projectHomeLink)))
        }

        val sponsorshipLink = "https://patreon.com/toolsboox"
        val sponsorshipLinkHtml = "<a href=\"$sponsorshipLink\">$sponsorshipLink</a>"
        binding.sponsorshipLink.text = Html.fromHtml(sponsorshipLinkHtml, Html.FROM_HTML_MODE_COMPACT)
        binding.sponsorshipLink.setOnClickListener {
            this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sponsorshipLink)))
        }

        val translateLink = "https://poeditor.com/join/project?hash=dbYOuWr2UB"
        val translateText = getString(R.string.about_other_links_translate_link)
        val translateLinkHtml = "$translateText <a href=\"$translateLink\">$translateLink</a>"
        binding.otherLinksTranslate.text = Html.fromHtml(translateLinkHtml, Html.FROM_HTML_MODE_COMPACT)
        binding.otherLinksTranslate.setOnClickListener {
            this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(translateLink)))
        }

        val facebookLink = "https://www.facebook.com/toolsboox"
        val facebookText = getString(R.string.about_other_links_facebook_link)
        val facebookLinkHtml = "$facebookText <a href=\"$facebookLink\">$facebookLink</a>"
        binding.otherLinksFacebook.text = Html.fromHtml(facebookLinkHtml, Html.FROM_HTML_MODE_COMPACT)
        binding.otherLinksFacebook.setOnClickListener {
            this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(facebookLink)))
        }
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.about_title))
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

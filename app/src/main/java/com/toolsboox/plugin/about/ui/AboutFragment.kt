package com.toolsboox.plugin.about.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.TextView
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
     * Update textView content to clickable link.
     *
     * @param linkView the link holder textView
     * @param messageResId the message resource id
     * @param link the link
     */
    private fun htmlLinks(linkView: TextView, messageResId: Int, link: String) {
        val linkMessage = getString(messageResId)
        val linkHtml = "$linkMessage <a href=\"$link\">$link</a>"
        linkView.text = Html.fromHtml(linkHtml, Html.FROM_HTML_MODE_COMPACT)
        linkView.setOnClickListener {
            this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }
    }

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAboutBinding.bind(view)

        htmlLinks(
            binding.aboutMeLink, R.string.about_about_me_link,
            "https://github.com/gaborauth/"
        )
        htmlLinks(
            binding.projectHomeLink, R.string.about_project_home_link,
            "https://github.com/gaborauth/toolsboox-android/"
        )
        htmlLinks(
            binding.otherLinksWebpage, R.string.about_other_links_webpage_link,
            "https://toolsboox.com"
        )
        htmlLinks(
            binding.otherLinksTranslate, R.string.about_other_links_translate_link,
            "https://poeditor.com/join/project?hash=dbYOuWr2UB"
        )
        htmlLinks(
            binding.otherLinksFacebook, R.string.about_other_links_facebook_link,
            "https://www.facebook.com/toolsboox"
        )
        htmlLinks(
            binding.otherLinksDiscord, R.string.about_other_links_discord_link,
            "https://discord.gg/S3sKsbmaSk"
        )
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

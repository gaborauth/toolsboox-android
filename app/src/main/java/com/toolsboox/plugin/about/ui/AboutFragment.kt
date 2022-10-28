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
     * @param link the link
     */
    private fun htmlLinks(linkView: TextView, link: String) {
        val linkHtml = "<a href=\"$link\">$link</a>"
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

        htmlLinks(binding.aboutMeLink, "https://github.com/gaborauth/")
        htmlLinks(binding.projectHomeLink, "https://github.com/gaborauth/toolsboox-android/")
        htmlLinks(binding.sponsorshipLink, "https://patreon.com/toolsboox")
        htmlLinks(binding.otherLinksTranslate, "https://poeditor.com/join/project?hash=dbYOuWr2UB")
        htmlLinks(binding.otherLinksFacebook, "https://www.facebook.com/toolsboox")
        htmlLinks(binding.otherLinksDiscord, "https://discord.gg/S3sKsbmaSk")
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

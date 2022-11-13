package com.toolsboox.plugin.templates.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.toolsboox.R
import com.toolsboox.da.SquareItem
import com.toolsboox.databinding.FragmentTemplatesMainBinding
import com.toolsboox.ot.SquareItemAdapter
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Templates main fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class TemplatesMainFragment @Inject constructor() : ScreenFragment() {

    @Inject
    lateinit var presenter: TemplatesMainPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_templates_main

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentTemplatesMainBinding

    /**
     * The dashboard item adapter.
     */
    private lateinit var adapter: SquareItemAdapter

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTemplatesMainBinding.bind(view)

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@TemplatesMainFragment.requireContext(), 4)
        }

        val squareItems = mutableListOf<SquareItem>()
        squareItems.add(
            SquareItem(
                getString(R.string.templates_item_boxed_days_title), R.drawable.ic_dashboard_item_calendar,
                R.id.action_to_templates_boxed_days_calendar, bundleOf() //"/templates/boxedDaysCalendar"
            )
        )
        squareItems.add(
            SquareItem(
                getString(R.string.templates_item_boxed_weeks_title), R.drawable.ic_dashboard_item_calendar,
                R.id.action_to_templates_boxed_weeks_calendar, bundleOf()
            )
        )
        squareItems.add(
            SquareItem(
                getString(R.string.templates_item_community_title), R.drawable.ic_community_templates,
                R.id.action_to_templates_community, bundleOf()
            )
        )
        squareItems.add(
            SquareItem(
                getString(R.string.templates_item_flat_weeks_title), R.drawable.ic_dashboard_item_calendar,
                R.id.action_to_templates_flat_weeks_calendar, bundleOf()
            )
        )
        squareItems.add(
            SquareItem(
                getString(R.string.templates_item_this_weeks_title), R.drawable.ic_dashboard_item_calendar,
                R.id.action_to_templates_this_weeks_calendar, bundleOf()
            )
        )

        val clickListener = object : SquareItemAdapter.OnItemClickListener {
            override fun onItemClicked(squareItem: SquareItem) {
                Timber.i("Route to ${squareItem.title}")
                findNavController().navigate(squareItem.actionId, squareItem.bundle)
            }
        }

        adapter = SquareItemAdapter(this.requireContext(), squareItems, clickListener)
        binding.recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.drawer_title)
            .format(getString(R.string.app_name), getString(R.string.templates_main_title))
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

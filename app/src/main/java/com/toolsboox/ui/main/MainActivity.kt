package com.toolsboox.ui.main

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.toolsboox.BuildConfig
import com.toolsboox.R
import com.toolsboox.databinding.ActivityMainBinding
import com.toolsboox.databinding.ToolbarBinding
import com.toolsboox.di.MainSharedPreferencesModule
import com.toolsboox.ui.BaseActivity
import com.toolsboox.utils.ReleaseTree
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*

/**
 * A dashboard screen that offers the main menu.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */

@AndroidEntryPoint
class MainActivity : BaseActivity<MainPresenter>(), MainView {

    /**
     * The view model.
     */
    private val viewModel by viewModels<MainViewModel>()

    /**
     * The Firebase analytics.
     */
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The view binding.
     */
    private lateinit var binding: ActivityMainBinding

    /**
     * OnCreate hook.
     *
     * @param savedInstanceState the saved state of the instance
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAnalytics = Firebase.analytics

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        setSupportActionBar(binding.mainToolbar.root)

        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        val headerEmail = binding.navigationView.getHeaderView(0).findViewById<TextView>(R.id.navigation_header_email)
        headerEmail.text = "unknown@unknown"

        val headerVersion = binding.navigationView.getHeaderView(0)
            .findViewById<TextView>(R.id.navigation_header_version)
        headerVersion.text = getString(R.string.main_version)
            .format(BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE)

        val preferences = MainSharedPreferencesModule.provideSharedPreferences(this)
        preferences.edit().putLong("lastTimestamp", Date().time).apply()

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.drawer_item_dashboard -> {
                    val bundle = bundleOf()
                    binding.fragmentContent.findNavController().navigate(R.id.action_to_dashboard, bundle)
                }

                R.id.drawer_item_website -> {
                    intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://toolsboox.com"))
                    this.startActivity(intent)
                }

                R.id.drawer_item_forum -> {
                    intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://forum.toolsboox.com"))
                    this.startActivity(intent)
                }

                R.id.drawer_item_facebook -> {
                    Timber.i("Not implemented yet...")
                }

                R.id.drawer_item_twitter -> {
                    Timber.i("Not implemented yet...")
                }

                R.id.drawer_item_logout -> {
                    Timber.i("Not implemented yet...")
                }
            }

            menuItem.isChecked = true
            binding.drawerLayout.closeDrawers()
            true
        }

        binding.mainToolbar.toolbarBack.setOnClickListener {
            onBackPressed()
        }

        presenter.onViewCreated()
    }

    /**
     * Get the main toolbar.
     *
     * @return the toolbar
     */
    fun getToolbar(): ToolbarBinding = binding.mainToolbar

    /**
     * Displays an error in the view.
     *
     * @param t the optional throwable
     * @param errorResId the resource id of the error
     */
    override fun showError(t: Throwable?, @StringRes errorResId: Int) {
        t?.let { Timber.e(it, getString(errorResId)) }
    }

    /**
     * Displays an error in the view.
     *
     * @param messageResId the resource id of the error
     */
    override fun showMessage(@StringRes messageResId: Int) {
        Snackbar.make(binding.mainToolbar.root, messageResId, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Show progress and hide login form.
     */
    override fun showLoading() {
    }

    /**
     * Hide progress and show login form.
     */
    override fun hideLoading() {
    }

    /**
     * Instantiate the presenter.
     */
    override fun presenter(): MainPresenter {
        return MainPresenter(this)
    }

    /**
     * Close the drawer menu.
     *
     * @param item the selected menu item
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * OnBackPressed hook.
     */
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            orientateFragment(null)
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Orientate the fragment by name.
     *
     * @param fragment the fragment
     */
    private fun orientateFragment(fragment: Fragment?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        Timber.i("Sensor portrait: ${fragment?.javaClass?.name}")
    }
}

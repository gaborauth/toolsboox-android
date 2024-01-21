package com.toolsboox.ui.main

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.toolsboox.BuildConfig
import com.toolsboox.R
import com.toolsboox.databinding.ActivityMainBinding
import com.toolsboox.databinding.ToolbarBinding
import com.toolsboox.di.MainSharedPreferencesModule
import com.toolsboox.nw.CredentialService
import com.toolsboox.ui.BaseActivity
import com.toolsboox.utils.ReleaseTree
import dagger.hilt.android.AndroidEntryPoint
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber
import java.time.Instant
import java.util.*
import javax.inject.Inject

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
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The injected shared preferences.
     */
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    /**
     * The credential service.
     */
    @Inject
    lateinit var credentialService: CredentialService

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

        MobileAds.initialize(this) {}
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

        // Temporary (?) fix for https://github.com/gaborauth/toolsboox-android/issues/305
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }

        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        sharedPreferences.edit().putString("androidId", androidId).apply()

        val headerUserId = binding.navigationView.getHeaderView(0).findViewById<TextView>(R.id.navigation_header_user_id)

        val userId = sharedPreferences.getString("userId", null)
        if (userId == null) {
            headerUserId.text = getString(R.string.main_not_logged_in)
        } else {
            headerUserId.text = userId
        }

        val headerAndroidId = binding.navigationView.getHeaderView(0).findViewById<TextView>(R.id.navigation_header_android_id)
        headerAndroidId.text = androidId

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
     * Activity onResume.
     */
    override fun onResume() {
        super.onResume()

        val host = intent?.data?.host
        val path = intent?.data?.path
        if (host == "app") {
            if (path?.startsWith("/calendar") == true) {
                val defaultCalendarStartActionId = when (sharedPreferences.getInt("calendarStartView", 0)) {
                    0 -> R.id.action_to_calendar_day
                    1 -> R.id.action_to_calendar_week
                    2 -> R.id.action_to_calendar_month
                    3 -> R.id.action_to_calendar_quarter
                    4 -> R.id.action_to_calendar_year
                    else -> R.id.action_to_calendar_day
                }

                val calendarStartActionId = when (path) {
                    "/calendar/day" -> R.id.action_to_calendar_day
                    "/calendar/week" -> R.id.action_to_calendar_week
                    "/calendar/month" -> R.id.action_to_calendar_month
                    "/calendar/quarter" -> R.id.action_to_calendar_quarter
                    "/calendar/year" -> R.id.action_to_calendar_year
                    else -> defaultCalendarStartActionId
                }

                val bundle = Bundle()
                binding.fragmentContent.findNavController().navigate(calendarStartActionId, bundle)
            }
        }

        val refreshToken = sharedPreferences.getString("refreshToken", null)
        val refreshTokenLastUpdate = sharedPreferences.getLong("refreshTokenLastUpdate", 0L)
        val accessTokenLastUpdate = sharedPreferences.getLong("accessTokenLastUpdate", 0L)
        val now = Date.from(Instant.now()).time

        if (refreshToken != null) {
            // Request new access token after 8 hours.
            if (accessTokenLastUpdate + 60 * 60 * 8L * 1000L < now) {
                presenter.accessToken(this, refreshToken)
            }
            // Request new refresh token after 7 days.
            if (refreshTokenLastUpdate + 60 * 60 * 24 * 7L * 1000L < now) {
                presenter.refreshToken(this, refreshToken)
            }

            // Remove all tokens after 30 days.
            if (refreshTokenLastUpdate + 60 * 60 * 24 * 30L * 1000L < now) {
                sharedPreferences.edit().remove("refreshToken").apply()
                sharedPreferences.edit().remove("refreshTokenLastUpdate").apply()
                sharedPreferences.edit().remove("accessToken").apply()
                sharedPreferences.edit().remove("accessTokenLastUpdate").apply()
            }
        }
    }

    /**
     * Process access token result.
     *
     * @param accessToken the access token
     */
    fun accessTokenResult(accessToken: String) {
        sharedPreferences.edit().putString("accessToken", accessToken).apply()
        sharedPreferences.edit().putLong("accessTokenLastUpdate", Date.from(Instant.now()).time).apply()
        Timber.i("Store the new access token in shared preferences: $accessToken")
    }

    /**
     * Process refresh token result.
     *
     * @param refreshToken the refresh token
     */
    fun refreshTokenResult(refreshToken: String) {
        sharedPreferences.edit().putString("refreshToken", refreshToken).apply()
        sharedPreferences.edit().putLong("refreshTokenLastUpdate", Date.from(Instant.now()).time).apply()
        Timber.i("Store the new refresh token in shared preferences: $refreshToken")
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
        return MainPresenter(this, credentialService)
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

package com.toolsboox.plugin.calendar.ui

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarSettingsBinding
import com.toolsboox.ot.ZipManager
import com.toolsboox.plugin.calendar.fi.*
import com.toolsboox.ui.main.MainActivity
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.inject.Inject

/**
 * Calendar settings presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarSettingsPresenter @Inject constructor() : FragmentPresenter() {

    /**
     * The calendar day service.
     */
    @Inject
    lateinit var calendarDayService: CalendarDayService

    /**
     * The calendar month service.
     */
    @Inject
    lateinit var calendarMonthService: CalendarMonthService

    /**
     * The calendar quarter service.
     */
    @Inject
    lateinit var calendarQuarterService: CalendarQuarterService

    /**
     * The calendar week service.
     */
    @Inject
    lateinit var calendarWeekService: CalendarWeekService

    /**
     * The calendar year service.
     */
    @Inject
    lateinit var calendarYearService: CalendarYearService

    /**
     * The calendar pattern service.
     */
    @Inject
    lateinit var calendarPatternService: CalendarPatternService

    /**
     * Export the calendar to the storage.
     *
     * @param fragment the fragment
     * @param binding the data binding
     */
    fun export(fragment: CalendarSettingsFragment, binding: FragmentCalendarSettingsBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Instant.now())
                    ZipManager.zip(File(rootPath, "calendar"), File(downloads, "toolsBoox-calendar-backup-$timestamp.zip"))

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            fragment.requireContext(), R.string.calendar_settings_backup_done, Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Create shortcut of calendar.
     *
     * @param fragment the fragment
     * @param binding the data binding
     */
    fun createShortcut(fragment: CalendarSettingsFragment, binding: FragmentCalendarSettingsBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        fragment.runOnActivity { fragment.showLoading() }

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(fragment.requireContext())) {
            val shortcutIntent = Intent(fragment.requireContext(), MainActivity::class.java)
            shortcutIntent.action = Intent.ACTION_VIEW
            shortcutIntent.data = Uri.parse("toolsboox://app/calendar")

            val shortcutInfo = ShortcutInfoCompat.Builder(fragment.requireContext(), "calendar")
                .setIntent(shortcutIntent)
                .setShortLabel(fragment.getString(R.string.calendar_settings_shortcut_short_label))
                .setLongLabel(fragment.getString(R.string.calendar_settings_shortcut_long_label))
                .setIcon(IconCompat.createWithResource(fragment.requireContext(), R.mipmap.ic_launcher_calendar))
                .build()

            val pinnedShortcutCallbackIntent = Intent()
            val successCallback = PendingIntent.getBroadcast(fragment.requireContext(), 12345, pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE)
            ShortcutManagerCompat.requestPinShortcut(fragment.requireContext(), shortcutInfo, successCallback.intentSender)

            Toast.makeText(fragment.requireContext(), R.string.calendar_settings_shortcut_done, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(fragment.requireContext(), R.string.calendar_settings_shortcut_failed, Toast.LENGTH_LONG).show()
        }

        fragment.runOnActivity { fragment.hideLoading() }
    }

    /**
     * Sync the calendar patterns.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param locale the locale
     */
    fun patternSync(fragment: CalendarSettingsFragment, binding: FragmentCalendarSettingsBinding, locale: Locale) {
        if (!checkPermissions(fragment, binding.root)) return

        // Sync only the 2022.
        val year = 2022

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                    val path = File(rootPath, "calendar/$year/")
                    if (!path.exists()) return@launch

                    val calendarPattern = calendarPatternService.load(rootPath, LocalDate.of(year, 1, 1), locale)

                    Files.walk(Paths.get(path.toURI())).use { stream ->
                        stream.map(Path::toFile).filter(File::isFile).filter { it.name.endsWith(".json") }.forEach { item ->
                            if (item.name.startsWith("pattern-")) return@forEach

                            if (item.name.startsWith("year-")) {
                                calendarYearService.load(item)?.let { calendarYear ->
                                    if (calendarYear.year == year) {
                                        calendarPattern.updateYear(calendarYear)
                                    }
                                }
                            }

                            if (item.name.startsWith("quarter-")) {
                                calendarQuarterService.load(item)?.let { calendarQuarter ->
                                    if (calendarQuarter.year == year) {
                                        calendarPattern.updateQuarter(calendarQuarter)
                                    }
                                }
                            }

                            if (item.name.startsWith("month-")) {
                                calendarMonthService.load(item)?.let { calendarMonth ->
                                    if (calendarMonth.year == year) {
                                        calendarPattern.updateMonth(calendarMonth)
                                    }
                                }
                            }

                            if (item.name.startsWith("week-")) {
                                calendarWeekService.load(item)?.let { calendarWeek ->
                                    if (calendarWeek.year == year) {
                                        calendarPattern.updateWeek(calendarWeek)
                                    }
                                }
                            }

                            if (item.name.startsWith("day-")) {
                                calendarDayService.load(item)?.let { calendarDay ->
                                    if (calendarDay.year == year) {
                                        calendarPattern.updateDay(calendarDay)
                                    }
                                }
                            }
                        }
                    }

                    calendarPatternService.save(rootPath, LocalDate.of(year, 1, 1), calendarPattern)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            fragment.requireContext(), R.string.calendar_settings_pattern_sync_done, Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }
}
package com.toolsboox.plugin.calendar.ui

import android.os.Environment
import android.widget.Toast
import com.squareup.moshi.Moshi
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarSettingsBinding
import com.toolsboox.ot.ZipManager
import com.toolsboox.plugin.calendar.da.*
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

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
                    ZipManager.zip(File(rootPath, "calendar"), File(downloads, "toolsBoox-calendar-backup.zip"))

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
     * Sync the calendar patterns.
     *
     * @param fragment the fragment
     * @param binding the data binding
     */
    fun patternSync(fragment: CalendarSettingsFragment, binding: FragmentCalendarSettingsBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                    val path = File(rootPath, "calendar/2022/")
                    if (!path.exists()) return@launch

                    val calendarPatternFile = File(path, "pattern-2022.json")
                    val calendarPattern = CalendarPattern(2022, Locale.getDefault()).fill()

                    Files.walk(Paths.get(path.toURI())).use { stream ->
                        stream.map(Path::toFile).filter(File::isFile).forEach { item ->
                            if (item.name.startsWith("pattern-")) return@forEach

                            if (item.name.startsWith("year-")) {
                                val adapter = moshi.adapter(CalendarYear::class.java)
                                FileReader(item).use { fileReader ->
                                    adapter.fromJson(fileReader.readText())?.let {
                                        if (it.year == 2022) {
                                            val pages = if (it.strokes.isEmpty()) 0 else 1
                                            val notes = if (it.notesStrokes.isEmpty()) 0 else 1
                                            calendarPattern.updateYear(pages, notes)
                                        }
                                    }
                                }
                            }
                            if (item.name.startsWith("quarter-")) {
                                val adapter = moshi.adapter(CalendarQuarter::class.java)
                                FileReader(item).use { fileReader ->
                                    adapter.fromJson(fileReader.readText())?.let {
                                        if (it.year == 2022) {
                                            val pages = if (it.strokes.isEmpty()) 0 else 1
                                            val notes = if (it.notesStrokes.isEmpty()) 0 else 1
                                            calendarPattern.updateQuarter(it.quarter, pages, notes)
                                        }
                                    }
                                }
                            }
                            if (item.name.startsWith("month-")) {
                                val adapter = moshi.adapter(CalendarMonth::class.java)
                                FileReader(item).use { fileReader ->
                                    adapter.fromJson(fileReader.readText())?.let {
                                        if (it.year == 2022) {
                                            val pages = if (it.strokes.isEmpty()) 0 else 1
                                            val notes = if (it.notesStrokes.isEmpty()) 0 else 1
                                            calendarPattern.updateMonth(it.month, pages, notes)
                                        }
                                    }
                                }
                            }
                            if (item.name.startsWith("week-")) {
                                val adapter = moshi.adapter(CalendarWeek::class.java)
                                FileReader(item).use { fileReader ->
                                    adapter.fromJson(fileReader.readText())?.let {
                                        if (it.year == 2022) {
                                            val pages = if (it.strokes.isEmpty()) 0 else 1
                                            val notes = if (it.notesStrokes.isEmpty()) 0 else 1
                                            calendarPattern.updateWeek(it.weekOfYear, pages, notes)
                                        }
                                    }
                                }
                            }
                            if (item.name.startsWith("day-")) {
                                val adapter = moshi.adapter(CalendarDay::class.java)
                                FileReader(item).use { fileReader ->
                                    adapter.fromJson(fileReader.readText())?.let {
                                        if (it.year == 2022) {
                                            val pages = if (it.strokes.isEmpty()) 0 else 1
                                            val notes = if (it.notesStrokes.isEmpty()) 0 else 1
                                            val localDate = LocalDate.of(it.year, it.month, it.day)
                                            calendarPattern.updateDay(localDate.dayOfYear, pages, notes)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val adapter = moshi.adapter(CalendarPattern::class.java)
                    PrintWriter(FileWriter(calendarPatternFile)).use {
                        it.write(adapter.toJson(calendarPattern))
                    }

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
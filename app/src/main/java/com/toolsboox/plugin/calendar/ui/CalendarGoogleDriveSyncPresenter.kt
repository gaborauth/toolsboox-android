package com.toolsboox.plugin.calendar.ui

import android.os.Environment
import androidx.lifecycle.lifecycleScope
import com.google.api.services.drive.Drive
import com.toolsboox.databinding.FragmentCalendarGoogleDriveSyncBinding
import com.toolsboox.plugin.calendar.da.v1.CalendarItem
import com.toolsboox.plugin.calendar.fi.*
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.inject.Inject


/**
 * Calendar Google Drive sync presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarGoogleDriveSyncPresenter @Inject constructor() : FragmentPresenter() {
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
     * Load the .json files of the Calendar plugin.
     *
     * @param fragment the fragment
     * @param userId the user ID
     * @param binding the binding on the fragment
     * @return the list of files
     */
    fun fileList(fragment: CalendarGoogleDriveSyncFragment, userId: UUID, binding: FragmentCalendarGoogleDriveSyncBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        val calendarItems: MutableList<CalendarItem> = mutableListOf()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                    val path = File(rootPath, "calendar/")
                    if (!path.exists()) return@launch

                    Files.walk(Paths.get(path.toURI())).use { stream ->
                        stream.map(Path::toFile).filter(File::isFile).filter { it.name.endsWith(".json") }.forEach { item ->
                            if (item.name.startsWith("pattern-")) return@forEach

                            calendarYearService.load(item)?.let { calendarYear ->
                                calendarItems.add(calendarYearService.getItem(userId, calendarYear))
                            }
                            calendarQuarterService.load(item)?.let { calendarQuarter ->
                                calendarItems.add(calendarQuarterService.getItem(userId, calendarQuarter))
                            }
                            calendarMonthService.load(item)?.let { calendarMonth ->
                                calendarItems.add(calendarMonthService.getItem(userId, calendarMonth))
                            }
                            calendarWeekService.load(item)?.let { calendarWeek ->
                                calendarItems.add(calendarWeekService.getItem(userId, calendarWeek))
                            }
                            calendarDayService.load(item)?.let { calendarDay ->
                                calendarItems.add(calendarDayService.getItem(userId, calendarDay))
                            }
                        }
                    }

                    fragment.fileListResult(calendarItems)
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Load the specified item from file to JSON.
     *
     * @param fragment the fragment
     * @param calendarItem the calendar item
     * @param binding the binding on the fragment
     * @return the loaded JSON
     */
    fun fileLoadJson(fragment: CalendarGoogleDriveSyncFragment, calendarItem: CalendarItem, binding: FragmentCalendarGoogleDriveSyncBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)

                    calendarYearService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                        fragment.fileLoadJsonResult(calendarItem, calendarYearService.json(it))
                    }
                    calendarQuarterService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                        fragment.fileLoadJsonResult(calendarItem, calendarQuarterService.json(it))
                    }
                    calendarMonthService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                        fragment.fileLoadJsonResult(calendarItem, calendarMonthService.json(it))
                    }
                    calendarWeekService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                        fragment.fileLoadJsonResult(calendarItem, calendarWeekService.json(it))
                    }
                    calendarDayService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                        fragment.fileLoadJsonResult(calendarItem, calendarDayService.json(it))
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
     * Update the cloudUpdate field of the specified item.
     *
     * @param fragment the fragment
     * @param calendarItem the calendar item
     * @param binding the binding on the fragment
     * @return the loaded JSON
     */
    fun fileUpdate(fragment: CalendarGoogleDriveSyncFragment, calendarItem: CalendarItem, binding: FragmentCalendarGoogleDriveSyncBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)

                    calendarYearService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                        it.created = calendarItem.created
                        it.updated = calendarItem.updated
                        calendarYearService.save(rootPath, calendarItem.path, calendarItem.baseName, it)
                        fragment.fileUpdateResult(calendarItem, it)
                    }
                    calendarQuarterService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                        it.created = calendarItem.created
                        it.updated = calendarItem.updated
                        calendarQuarterService.save(rootPath, calendarItem.path, calendarItem.baseName, it)
                        fragment.fileUpdateResult(calendarItem, it)
                    }
                    calendarMonthService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                        it.created = calendarItem.created
                        it.updated = calendarItem.updated
                        calendarMonthService.save(rootPath, calendarItem.path, calendarItem.baseName, it)
                        fragment.fileUpdateResult(calendarItem, it)
                    }
                    calendarWeekService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                        it.created = calendarItem.created
                        it.updated = calendarItem.updated
                        calendarWeekService.save(rootPath, calendarItem.path, calendarItem.baseName, it)
                        fragment.fileUpdateResult(calendarItem, it)
                    }
                    calendarDayService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                        it.created = calendarItem.created
                        it.updated = calendarItem.updated
                        calendarDayService.save(rootPath, calendarItem.path, calendarItem.baseName, it)
                        fragment.fileUpdateResult(calendarItem, it)
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    fun cloudList(fragment: CalendarGoogleDriveSyncFragment, driveService: Drive) {
        fragment.lifecycleScope.launch {
            fragment.showLoading()
            withContext(Dispatchers.IO) {
                val files = driveService.files().list().setQ("mimeType='application/vnd.google-apps.folder' and trashed=false").execute().files
                Timber.i("Files: ${files.size}")
                val calendarItems = listOf<CalendarItem>()
                withContext(Dispatchers.Main) {
                    fragment.cloudListResult(calendarItems)
                    fragment.hideLoading()
                }
            }
        }
    }

    /**
     * Update the specified calendar item on Google Drive.
     *
     * @param fragment the fragment
     * @param driveService the Google Drive service
     * @param calendarItem the calendar item
     */
    fun cloudUpdate(fragment: CalendarGoogleDriveSyncFragment, driveService: Drive, calendarItem: CalendarItem) {
        val folders = calendarItem.path.split("/")
        val baseName = calendarItem.baseName
        Timber.i("Folders: $folders, baseName: $baseName")

        fragment.lifecycleScope.launch {
            fragment.showLoading()
            withContext(Dispatchers.IO) {
                val folder = com.google.api.services.drive.model.File()
                    .setParents(Collections.singletonList("root"))
                    .setMimeType("application/vnd.google-apps.folder")
                    .setName(folders[0])
                val result = driveService.files().create(folder).execute()
                Timber.i("Files: ${result.name}")
                withContext(Dispatchers.Main) {
                    fragment.hideLoading()
                }
            }
        }
    }
}
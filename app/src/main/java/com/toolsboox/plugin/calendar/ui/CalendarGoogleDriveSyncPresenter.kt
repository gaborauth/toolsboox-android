package com.toolsboox.plugin.calendar.ui

import android.os.Environment
import androidx.lifecycle.lifecycleScope
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.toolsboox.databinding.FragmentCalendarGoogleDriveSyncBinding
import com.toolsboox.fi.GoogleDriveService
import com.toolsboox.plugin.calendar.da.v1.CalendarSyncItem
import com.toolsboox.plugin.calendar.fi.*
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
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
     * The calendar pattern service.
     */
    @Inject
    lateinit var calendarPatternService: CalendarPatternService

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

        fragment.showLoading()
        val calendarSyncItems: MutableList<CalendarSyncItem> = mutableListOf()
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                val path = File(rootPath, "calendar/")
                if (!path.exists()) return@launch

                Files.walk(Paths.get(path.toURI())).use { stream ->
                    stream.map(Path::toFile).filter(File::isFile).filter { it.name.endsWith(".json") }.forEach { item ->
                        if (item.name.startsWith("pattern-")) return@forEach

                        calendarYearService.load(item)?.let { calendarYear ->
                            calendarSyncItems.add(calendarYearService.getItem(userId, calendarYear))
                        }
                        calendarQuarterService.load(item)?.let { calendarQuarter ->
                            calendarSyncItems.add(calendarQuarterService.getItem(userId, calendarQuarter))
                        }
                        calendarMonthService.load(item)?.let { calendarMonth ->
                            calendarSyncItems.add(calendarMonthService.getItem(userId, calendarMonth))
                        }
                        calendarWeekService.load(item)?.let { calendarWeek ->
                            calendarSyncItems.add(calendarWeekService.getItem(userId, calendarWeek))
                        }
                        calendarDayService.load(item)?.let { calendarDay ->
                            calendarSyncItems.add(calendarDayService.getItem(userId, calendarDay))
                        }
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    fragment.somethingHappened(e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    fragment.hideLoading()
                    fragment.fileListResult(calendarSyncItems)
                }
            }
        }
    }

    /**
     * Load the list of files from Google Drive.
     *
     * @param fragment the fragment
     * @param driveService the Google Drive service
     */
    fun cloudList(fragment: CalendarGoogleDriveSyncFragment, driveService: Drive) {
        fragment.showLoading()
        val calendarSyncItems = mutableListOf<CalendarSyncItem>()
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val appRoot = GoogleDriveService.getOrCreateRootFolder(driveService, "calendar")
                val files = GoogleDriveService.walk(driveService, appRoot!!, "")
                files.forEach {
                    if (it.properties == null) return@forEach
                    val path = it.properties["path"] ?: return@forEach
                    val baseName = it.properties["baseName"] ?: return@forEach
                    val version = it.properties["version"] ?: return@forEach
                    val created = it.properties["created"]?.toLong() ?: return@forEach
                    val modified = it.properties["modified"]?.toLong() ?: return@forEach
                    calendarSyncItems.add(CalendarSyncItem(UUID.randomUUID(), path, baseName, version, Date(created), Date(modified), null))
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    fragment.somethingHappened(e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    fragment.hideLoading()
                    fragment.cloudListResult(calendarSyncItems)
                }
            }
        }
    }

    /**
     * Load the specified item from file.
     *
     * @param fragment the fragment
     * @param calendarSyncItem the calendar sync item
     * @param binding the binding on the fragment
     * @return the loaded content
     */
    fun fileLoad(fragment: CalendarGoogleDriveSyncFragment, calendarSyncItem: CalendarSyncItem, binding: FragmentCalendarGoogleDriveSyncBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        fragment.showLoading()
        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                calendarYearService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                    calendarSyncItem.json = calendarYearService.json(it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileLoadResult(calendarSyncItem)
                    }
                }
                calendarQuarterService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                    calendarSyncItem.json = calendarQuarterService.json(it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileLoadResult(calendarSyncItem)
                    }
                }
                calendarMonthService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                    calendarSyncItem.json = calendarMonthService.json(it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileLoadResult(calendarSyncItem)
                    }
                }
                calendarWeekService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                    calendarSyncItem.json = calendarWeekService.json(it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileLoadResult(calendarSyncItem)
                    }
                }
                calendarDayService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                    calendarSyncItem.json = calendarDayService.json(it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileLoadResult(calendarSyncItem)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    fragment.somethingHappened(e)
                }
            }
        }
    }

    /**
     * Load the specified item from Google Drive.
     *
     * @param fragment the fragment
     * @param driveService the Google Drive service
     * @param calendarSyncItem the calendar sync item
     */
    fun cloudLoad(fragment: CalendarGoogleDriveSyncFragment, driveService: Drive, calendarSyncItem: CalendarSyncItem) {
        fragment.showLoading()
        val fileName = "${calendarSyncItem.baseName}-${calendarSyncItem.version}.json"
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val appRoot = GoogleDriveService.getOrCreateRootFolder(driveService, "calendar")
                val folder = GoogleDriveService.getOrCreatePath(driveService, appRoot!!, calendarSyncItem.path)
                val file = GoogleDriveService.getFile(driveService, folder!!, fileName)

                val byteArrayOutputStream = ByteArrayOutputStream()
                GoogleDriveService.downloadFile(driveService, file!!, byteArrayOutputStream)
                calendarSyncItem.json = byteArrayOutputStream.toString("UTF-8")

                withContext(Dispatchers.Main) {
                    fragment.hideLoading()
                    fragment.cloudLoadResult(calendarSyncItem)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    fragment.somethingHappened(e)
                }
            }
        }
    }

    /**
     * Update the cloudUpdate field of the specified item.
     *
     * @param fragment the fragment
     * @param calendarSyncItem the calendar sync item
     * @param binding the binding on the fragment
     * @return the loaded JSON
     */
    fun fileUpdate(fragment: CalendarGoogleDriveSyncFragment, calendarSyncItem: CalendarSyncItem, binding: FragmentCalendarGoogleDriveSyncBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        fragment.showLoading()
        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                calendarYearService.fromSyncItem(calendarSyncItem)?.let {
                    calendarYearService.save(rootPath, calendarSyncItem.path, calendarSyncItem.baseName, it)

                    val currentDate = LocalDate.ofYearDay(it.year, 1)
                    val calendarPattern = calendarPatternService.load(rootPath, currentDate, it.locale)
                    calendarPattern.updateYear(it)
                    calendarPatternService.save(rootPath, currentDate, calendarPattern)

                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileUpdateResult(calendarSyncItem)
                    }
                }
                calendarQuarterService.fromSyncItem(calendarSyncItem)?.let {
                    calendarQuarterService.save(rootPath, calendarSyncItem.path, calendarSyncItem.baseName, it)

                    val currentDate = LocalDate.ofYearDay(it.year, 1)
                    val calendarPattern = calendarPatternService.load(rootPath, currentDate, it.locale)
                    calendarPattern.updateQuarter(it)
                    calendarPatternService.save(rootPath, currentDate, calendarPattern)

                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileUpdateResult(calendarSyncItem)
                    }
                }
                calendarMonthService.fromSyncItem(calendarSyncItem)?.let {
                    calendarMonthService.save(rootPath, calendarSyncItem.path, calendarSyncItem.baseName, it)

                    val currentDate = LocalDate.ofYearDay(it.year, 1)
                    val calendarPattern = calendarPatternService.load(rootPath, currentDate, it.locale)
                    calendarPattern.updateMonth(it)
                    calendarPatternService.save(rootPath, currentDate, calendarPattern)

                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileUpdateResult(calendarSyncItem)
                    }
                }
                calendarWeekService.fromSyncItem(calendarSyncItem)?.let {
                    calendarWeekService.save(rootPath, calendarSyncItem.path, calendarSyncItem.baseName, it)

                    val currentDate = LocalDate.ofYearDay(it.year, 1)
                    val calendarPattern = calendarPatternService.load(rootPath, currentDate, it.locale)
                    calendarPattern.updateWeek(it)
                    calendarPatternService.save(rootPath, currentDate, calendarPattern)

                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileUpdateResult(calendarSyncItem)
                    }
                }
                calendarDayService.fromSyncItem(calendarSyncItem)?.let {
                    calendarDayService.save(rootPath, calendarSyncItem.path, calendarSyncItem.baseName, it)

                    val currentDate = LocalDate.ofYearDay(it.year, 1)
                    val calendarPattern = calendarPatternService.load(rootPath, currentDate, it.locale)
                    calendarPattern.updateDay(it)
                    calendarPatternService.save(rootPath, currentDate, calendarPattern)

                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileUpdateResult(calendarSyncItem)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    fragment.somethingHappened(e)
                }
            } finally {
                withContext(Dispatchers.Main) {
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
     * @param calendarSyncItem the calendar sync item
     */
    fun cloudUpdate(fragment: CalendarGoogleDriveSyncFragment, driveService: Drive, calendarSyncItem: CalendarSyncItem) {
        fragment.showLoading()
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val appRoot = GoogleDriveService.getOrCreateRootFolder(driveService, "calendar")
                val folder = GoogleDriveService.getOrCreatePath(driveService, appRoot!!, calendarSyncItem.path)

                val properties = mutableMapOf<String, String>()
                properties["path"] = calendarSyncItem.path
                properties["baseName"] = calendarSyncItem.baseName
                properties["version"] = calendarSyncItem.version
                properties["created"] = (calendarSyncItem.created?.time ?: Instant.now().toEpochMilli()).toString()
                properties["modified"] = (calendarSyncItem.updated?.time ?: Instant.now().toEpochMilli()).toString()

                val fileName = "${calendarSyncItem.baseName}-${calendarSyncItem.version}.json"
                val content = ByteArrayContent.fromString("application/json", calendarSyncItem.json!!)

                val uploaded = GoogleDriveService.uploadFile(driveService, folder!!, fileName, content, properties)
                Timber.i("App root: $appRoot, folder: $folder, uploaded: $uploaded")
                withContext(Dispatchers.Main) {
                    fragment.hideLoading()
                    fragment.cloudUpdateResult(calendarSyncItem)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    fragment.somethingHappened(e)
                }
            }
        }
    }
}
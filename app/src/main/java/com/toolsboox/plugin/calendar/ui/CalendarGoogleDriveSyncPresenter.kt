package com.toolsboox.plugin.calendar.ui

import android.os.Environment
import androidx.lifecycle.lifecycleScope
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.toolsboox.databinding.FragmentCalendarGoogleDriveSyncBinding
import com.toolsboox.fi.GoogleDriveService
import com.toolsboox.plugin.calendar.da.v1.CalendarItem
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

        fragment.showLoading()
        val calendarItems: MutableList<CalendarItem> = mutableListOf()
        fragment.lifecycleScope.launch(Dispatchers.IO) {
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
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    fragment.somethingHappened(e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    fragment.hideLoading()
                    fragment.fileListResult(calendarItems)
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
        val calendarItems = mutableListOf<CalendarItem>()
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
                    calendarItems.add(CalendarItem(UUID.randomUUID(), path, baseName, version, Date(created), Date(modified), null))
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    fragment.somethingHappened(e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    fragment.hideLoading()
                    fragment.cloudListResult(calendarItems)
                }
            }
        }
    }

    /**
     * Load the specified item from file.
     *
     * @param fragment the fragment
     * @param calendarItem the calendar item
     * @param binding the binding on the fragment
     * @return the loaded content
     */
    fun fileLoad(fragment: CalendarGoogleDriveSyncFragment, calendarItem: CalendarItem, binding: FragmentCalendarGoogleDriveSyncBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        fragment.showLoading()
        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                calendarYearService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                    calendarItem.json = calendarYearService.json(it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileLoadResult(calendarItem)
                    }
                }
                calendarQuarterService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                    calendarItem.json = calendarQuarterService.json(it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileLoadResult(calendarItem)
                    }
                }
                calendarMonthService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                    calendarItem.json = calendarMonthService.json(it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileLoadResult(calendarItem)
                    }
                }
                calendarWeekService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                    calendarItem.json = calendarWeekService.json(it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileLoadResult(calendarItem)
                    }
                }
                calendarDayService.load(rootPath, calendarItem.path, calendarItem.baseName)?.let {
                    calendarItem.json = calendarDayService.json(it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileLoadResult(calendarItem)
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
     * @param calendarItem the calendar item
     */
    fun cloudLoad(fragment: CalendarGoogleDriveSyncFragment, driveService: Drive, calendarItem: CalendarItem) {
        fragment.showLoading()
        val fileName = "${calendarItem.baseName}-${calendarItem.version}.json"
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val appRoot = GoogleDriveService.getOrCreateRootFolder(driveService, "calendar")
                val folder = GoogleDriveService.getOrCreatePath(driveService, appRoot!!, calendarItem.path)
                val file = GoogleDriveService.getFile(driveService, folder!!, fileName)

                val byteArrayOutputStream = ByteArrayOutputStream()
                GoogleDriveService.downloadFile(driveService, file!!, byteArrayOutputStream)
                calendarItem.json = byteArrayOutputStream.toString("UTF-8")

                withContext(Dispatchers.Main) {
                    fragment.hideLoading()
                    fragment.cloudLoadResult(calendarItem)
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
     * @param calendarItem the calendar item
     * @param binding the binding on the fragment
     * @return the loaded JSON
     */
    fun fileUpdate(fragment: CalendarGoogleDriveSyncFragment, calendarItem: CalendarItem, binding: FragmentCalendarGoogleDriveSyncBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        fragment.showLoading()
        val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                calendarYearService.fromItem(calendarItem)?.let {
                    calendarYearService.save(rootPath, calendarItem.path, calendarItem.baseName, it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileUpdateResult(calendarItem)
                    }
                }
                calendarQuarterService.fromItem(calendarItem)?.let {
                    calendarQuarterService.save(rootPath, calendarItem.path, calendarItem.baseName, it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileUpdateResult(calendarItem)
                    }
                }
                calendarMonthService.fromItem(calendarItem)?.let {
                    calendarMonthService.save(rootPath, calendarItem.path, calendarItem.baseName, it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileUpdateResult(calendarItem)
                    }
                }
                calendarWeekService.fromItem(calendarItem)?.let {
                    calendarWeekService.save(rootPath, calendarItem.path, calendarItem.baseName, it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileUpdateResult(calendarItem)
                    }
                }
                calendarDayService.fromItem(calendarItem)?.let {
                    calendarDayService.save(rootPath, calendarItem.path, calendarItem.baseName, it)
                    withContext(Dispatchers.Main) {
                        fragment.hideLoading()
                        fragment.fileUpdateResult(calendarItem)
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
     * @param calendarItem the calendar item
     */
    fun cloudUpdate(fragment: CalendarGoogleDriveSyncFragment, driveService: Drive, calendarItem: CalendarItem) {
        fragment.showLoading()
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val appRoot = GoogleDriveService.getOrCreateRootFolder(driveService, "calendar")
                val folder = GoogleDriveService.getOrCreatePath(driveService, appRoot!!, calendarItem.path)

                val properties = mutableMapOf<String, String>()
                properties["path"] = calendarItem.path
                properties["baseName"] = calendarItem.baseName
                properties["version"] = calendarItem.version
                properties["created"] = (calendarItem.created?.time ?: Instant.now().toEpochMilli()).toString()
                properties["modified"] = (calendarItem.updated?.time ?: Instant.now().toEpochMilli()).toString()

                val fileName = "${calendarItem.baseName}-${calendarItem.version}.json"
                val content = ByteArrayContent.fromString("application/json", calendarItem.json!!)

                val uploaded = GoogleDriveService.uploadFile(driveService, folder!!, fileName, content, properties)
                Timber.i("App root: $appRoot, folder: $folder, uploaded: $uploaded")
                withContext(Dispatchers.Main) {
                    fragment.hideLoading()
                    fragment.cloudUpdateResult(calendarItem)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    fragment.somethingHappened(e)
                }
            }
        }
    }
}
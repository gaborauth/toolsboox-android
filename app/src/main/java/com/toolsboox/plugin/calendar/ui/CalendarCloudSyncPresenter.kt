package com.toolsboox.plugin.calendar.ui

import android.os.Environment
import com.toolsboox.databinding.FragmentCalendarCloudSyncBinding
import com.toolsboox.plugin.calendar.da.v1.CalendarSyncItem
import com.toolsboox.plugin.calendar.fi.*
import com.toolsboox.plugin.calendar.nw.CalendarService
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
import java.util.*
import javax.inject.Inject

/**
 * Calendar cloud sync presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarCloudSyncPresenter @Inject constructor() : FragmentPresenter() {
    /**
     * The calendar service.
     */
    @Inject
    lateinit var calendarService: CalendarService

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
    fun fileList(fragment: CalendarCloudSyncFragment, userId: UUID, binding: FragmentCalendarCloudSyncBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        val calendarSyncItems: MutableList<CalendarSyncItem> = mutableListOf()
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

                    fragment.fileListResult(calendarSyncItems)
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
     * @param calendarSyncItem the calendar sync item
     * @param binding the binding on the fragment
     * @return the loaded JSON
     */
    fun fileLoadJson(fragment: CalendarCloudSyncFragment, calendarSyncItem: CalendarSyncItem, binding: FragmentCalendarCloudSyncBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)

                    calendarYearService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                        fragment.fileLoadJsonResult(calendarSyncItem, calendarYearService.json(it))
                    }
                    calendarQuarterService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                        fragment.fileLoadJsonResult(calendarSyncItem, calendarQuarterService.json(it))
                    }
                    calendarMonthService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                        fragment.fileLoadJsonResult(calendarSyncItem, calendarMonthService.json(it))
                    }
                    calendarWeekService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                        fragment.fileLoadJsonResult(calendarSyncItem, calendarWeekService.json(it))
                    }
                    calendarDayService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                        fragment.fileLoadJsonResult(calendarSyncItem, calendarDayService.json(it))
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
     * @param calendarSyncItem the calendar sync item
     * @param binding the binding on the fragment
     * @return the loaded JSON
     */
    fun fileUpdate(fragment: CalendarCloudSyncFragment, calendarSyncItem: CalendarSyncItem, binding: FragmentCalendarCloudSyncBinding) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                try {
                    val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)

                    calendarYearService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                        it.created = calendarSyncItem.created
                        it.updated = calendarSyncItem.updated
                        calendarYearService.save(rootPath, calendarSyncItem.path, calendarSyncItem.baseName, it)
                        fragment.fileUpdateResult(calendarSyncItem, it)
                    }
                    calendarQuarterService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                        it.created = calendarSyncItem.created
                        it.updated = calendarSyncItem.updated
                        calendarQuarterService.save(rootPath, calendarSyncItem.path, calendarSyncItem.baseName, it)
                        fragment.fileUpdateResult(calendarSyncItem, it)
                    }
                    calendarMonthService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                        it.created = calendarSyncItem.created
                        it.updated = calendarSyncItem.updated
                        calendarMonthService.save(rootPath, calendarSyncItem.path, calendarSyncItem.baseName, it)
                        fragment.fileUpdateResult(calendarSyncItem, it)
                    }
                    calendarWeekService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                        it.created = calendarSyncItem.created
                        it.updated = calendarSyncItem.updated
                        calendarWeekService.save(rootPath, calendarSyncItem.path, calendarSyncItem.baseName, it)
                        fragment.fileUpdateResult(calendarSyncItem, it)
                    }
                    calendarDayService.load(rootPath, calendarSyncItem.path, calendarSyncItem.baseName)?.let {
                        it.created = calendarSyncItem.created
                        it.updated = calendarSyncItem.updated
                        calendarDayService.save(rootPath, calendarSyncItem.path, calendarSyncItem.baseName, it)
                        fragment.fileUpdateResult(calendarSyncItem, it)
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
     * Retrieve the encrypted credential content.
     *
     * @param fragment the fragment
     * @return the encrypted credential content
     */
    fun authenticateGet(fragment: CalendarCloudSyncFragment) {
        coroutinesCallHelper(
            fragment,
            { calendarService.authenticateGetAsync() },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.authenticateGetResult(body)
                        }
                    }

                    204 -> {
                        fragment.authenticateGetNoContent()
                    }

                    else -> fragment.somethingHappened()
                }
            },
            true
        )
    }

    /**
     * Store the encrypted credential content.
     *
     * @param fragment the fragment
     * @param data the encrypted credential content
     * @return the encrypted credential content
     */
    fun authenticatePost(fragment: CalendarCloudSyncFragment, data: String) {
        coroutinesCallHelper(
            fragment,
            { calendarService.authenticatePostAsync(data) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.authenticatePostResult()
                        }
                    }

                    else -> fragment.somethingHappened()
                }
            },
            true
        )
    }

    /**
     * Get a list of the cloud calendar items.
     *
     * @param fragment the fragment
     * @return the list of calendar items
     */
    fun cloudList(fragment: CalendarCloudSyncFragment) {
        coroutinesCallHelper(
            fragment,
            { calendarService.listAsync() },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.cloudListResult(body)
                        }
                    }

                    else -> fragment.somethingHappened()
                }
            },
            true
        )
    }

    /**
     * Update the specified calendar item.
     *
     * @param fragment the fragment
     * @param calendarSyncItem the calendar sync item
     * @param data the data
     * @return the list of calendar items
     */
    fun cloudUpdate(fragment: CalendarCloudSyncFragment, calendarSyncItem: CalendarSyncItem, data: String) {
        val path = calendarSyncItem.path.replace("/", "%2F")
        val baseName = calendarSyncItem.baseName.replace("/", "%2F")

        coroutinesCallHelper(
            fragment,
            { calendarService.updateAsync(path, baseName, calendarSyncItem.version, data) },
            { response ->
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body == null) {
                            fragment.somethingHappened()
                        } else {
                            fragment.cloudUpdateResult(body)
                        }
                    }

                    else -> fragment.somethingHappened()
                }
            },
            true
        )
    }
}
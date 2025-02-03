package com.toolsboox.plugin.calendar.fi

import com.squareup.moshi.Moshi
import com.toolsboox.plugin.calendar.da.v1.CalendarSyncItem
import com.toolsboox.plugin.calendar.da.v2.CalendarMonth
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

/**
 * Calendar month data service.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarMonthService @Inject constructor() {
    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

    /**
     * Returns with the sync item of the data class.
     *
     * @param userId the user ID
     * @param calendarMonth the data class
     * @return the calendar sync item data class
     */
    fun getItem(userId: UUID, calendarMonth: CalendarMonth): CalendarSyncItem {
        val year = "%04d".format(calendarMonth.year)
        val month = "%02d".format(calendarMonth.month)
        return CalendarSyncItem(userId, "$year/$month/", "month-$year-$month", "v2", calendarMonth.created, calendarMonth.updated)
    }

    /**
     * Load the data class from the sync item.
     *
     * @param calendarSyncItem the calendar sync item
     * @return the data class
     */
    fun fromSyncItem(calendarSyncItem: CalendarSyncItem): CalendarMonth? {
        if (!calendarSyncItem.baseName.startsWith("month-")) return null

        when (calendarSyncItem.version) {
            "v1" -> {
                moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarMonth::class.java)
                    .fromJson(calendarSyncItem.json!!)?.let { return CalendarMonth.convert(it) }
            }

            "v2" -> {
                moshi.adapter(CalendarMonth::class.java)
                    .fromJson(calendarSyncItem.json!!)?.let { return it }
            }
        }

        return null
    }

    /**
     * Load the data class from JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param currentDate the current date
     * @param locale the current locale
     */
    fun load(rootPath: File, currentDate: LocalDate, locale: Locale): CalendarMonth {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val month = currentDate.format(DateTimeFormatter.ofPattern("MM"))
        val calendarMonth = CalendarMonth(currentDate.year, currentDate.monthValue, locale)

        return load(rootPath, "$year/$month/", "month-$year-$month") ?: calendarMonth
    }

    /**
     * Load the data class from JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param path the path
     * @param baseName the base name
     * @return the data class
     */
    fun load(rootPath: File, path: String, baseName: String): CalendarMonth? {
        val fullPath = File(rootPath, "calendar/$path")

        load(File(fullPath, "$baseName-v2.json"))?.let { return it }
        load(File(fullPath, "$baseName.json"))?.let { return it }

        return null
    }

    /**
     * Load the data class from JSON file on the specified path.
     *
     * @param item the file
     * @return optional data class instance
     */
    fun load(item: File): CalendarMonth? {
        if (!item.exists()) return null
        if (!item.name.startsWith("month-")) return null

        FileReader(item).use { fileReader ->
            Timber.i("Try to load from ${item.name}")
            if (item.absolutePath.endsWith("-v2.json")) {
                moshi.adapter(CalendarMonth::class.java)
                    .fromJson(fileReader.readText())?.let { return it }
            } else {
                moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarMonth::class.java)
                    .fromJson(fileReader.readText())?.let { return CalendarMonth.convert(it) }
            }
        }

        return null
    }

    /**
     * Convert the data class to JSON.
     *
     * @param calendarMonth the calendar month
     * @return the JSON
     */
    fun json(calendarMonth: CalendarMonth): String {
        val adapter = moshi.adapter(CalendarMonth::class.java)
        return adapter.toJson(calendarMonth)
    }

    /**
     * Save the data class to JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param currentDate the current date
     * @param calendarMonth the data class
     */
    fun save(rootPath: File, currentDate: LocalDate, calendarMonth: CalendarMonth) {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val month = currentDate.format(DateTimeFormatter.ofPattern("MM"))

        calendarMonth.created = calendarMonth.created ?: Date.from(Instant.now())
        calendarMonth.updated = calendarMonth.updated ?: Date.from(Instant.now())
        save(rootPath, "$year/$month/", "month-$year-$month", calendarMonth)
    }

    fun save(rootPath: File, path: String, baseName: String, calendarMonth: CalendarMonth) {
        val fullPath = File(rootPath, "calendar/$path")
        fullPath.mkdirs()

        // Try to save to v2
        PrintWriter(FileWriter(File(fullPath, "$baseName-v2.json"))).use { it.write(json(calendarMonth)) }

        // Try to rename the old file to .backup
        val source = File(fullPath, "$baseName.json")
        if (source.exists()) {
            Files.move(source.toPath(), source.toPath().resolveSibling("$baseName.json.backup"))
        }
    }
}
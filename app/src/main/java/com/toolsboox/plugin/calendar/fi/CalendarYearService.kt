package com.toolsboox.plugin.calendar.fi

import com.squareup.moshi.Moshi
import com.toolsboox.plugin.calendar.da.v1.CalendarItem
import com.toolsboox.plugin.calendar.da.v2.CalendarYear
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
 * Calendar year data service.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarYearService @Inject constructor() {
    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

    /**
     * Returns with the item of the data class.
     *
     * @param userId the user ID
     * @param calendarYear the data class
     * @return the calendar item data class
     */
    fun getItem(userId: UUID, calendarYear: CalendarYear): CalendarItem {
        val year = "%04d".format(calendarYear.year)
        return CalendarItem(userId, "$year/", "year-$year", "v2", calendarYear.created, calendarYear.updated)
    }

    /**
     * Load the data class from JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param currentDate the current date
     * @param locale the current locale
     * @return the data class
     */
    fun load(rootPath: File, currentDate: LocalDate, locale: Locale): CalendarYear {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val calendarYear = CalendarYear(currentDate.year, locale)

        return load(rootPath, "$year/", "year-$year") ?: calendarYear
    }

    /**
     * Load the data class from JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param path the path
     * @param baseName the base name
     * @return the data class
     */
    fun load(rootPath: File, path: String, baseName: String): CalendarYear? {
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
    fun load(item: File): CalendarYear? {
        if (!item.exists()) return null
        if (!item.name.startsWith("year-")) return null

        FileReader(item).use { fileReader ->
            Timber.i("Try to load from ${item.name}")
            if (item.absolutePath.endsWith("-v2.json")) {
                moshi.adapter(CalendarYear::class.java)
                    .fromJson(fileReader.readText())?.let { return it }
            } else {
                moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarYear::class.java)
                    .fromJson(fileReader.readText())?.let { return CalendarYear.convert(it) }
            }
        }

        return null
    }

    /**
     * Convert the data class to JSON.
     *
     * @param calendarYear the calendar year
     * @return the JSON
     */
    fun json(calendarYear: CalendarYear): String {
        val adapter = moshi.adapter(CalendarYear::class.java)
        return adapter.toJson(calendarYear)
    }

    /**
     * Save the data class to JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param currentDate the current date
     * @param calendarYear the data class
     */
    fun save(rootPath: File, currentDate: LocalDate, calendarYear: CalendarYear) {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))

        calendarYear.created = calendarYear.created ?: Date.from(Instant.now())
        calendarYear.updated = Date.from(Instant.now())
        save(rootPath, "$year/", "year-$year", calendarYear)
    }

    fun save(rootPath: File, path: String, baseName: String, calendarYear: CalendarYear) {
        val fullPath = File(rootPath, "calendar/$path")
        fullPath.mkdirs()

        // Try to save to v2
        PrintWriter(FileWriter(File(fullPath, "$baseName-v2.json"))).use { it.write(json(calendarYear)) }

        // Try to rename the old file to .backup
        val source = File(fullPath, "$baseName.json")
        if (source.exists()) {
            Files.move(source.toPath(), source.toPath().resolveSibling("$baseName.json.backup"))
        }
    }
}
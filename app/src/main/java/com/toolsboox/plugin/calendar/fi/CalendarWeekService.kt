package com.toolsboox.plugin.calendar.fi

import com.squareup.moshi.Moshi
import com.toolsboox.plugin.calendar.da.v1.CalendarItem
import com.toolsboox.plugin.calendar.da.v2.CalendarWeek
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import javax.inject.Inject

/**
 * Calendar week data service.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarWeekService @Inject constructor() {
    /**
     * The Moshi instance.
     */
    @Inject
    lateinit var moshi: Moshi

    /**
     * Returns with the item of the data class.
     *
     * @param userId the user ID
     * @param calendarWeek the data class
     * @return the calendar item data class
     */
    fun getItem(userId: UUID, calendarWeek: CalendarWeek): CalendarItem {
        val year = "%04d".format(calendarWeek.year)
        val week = "%02d".format(calendarWeek.weekOfYear)
        return CalendarItem(userId, "$year/", "week-$year-$week", "v2", calendarWeek.created, calendarWeek.updated)
    }

    /**
     * Load the data class from JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param currentDate the current date
     * @param locale the current locale
     */
    fun load(rootPath: File, currentDate: LocalDate, locale: Locale): CalendarWeek {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val week = currentDate.format(DateTimeFormatter.ofPattern("ww", locale))
        val weekOfYearField = WeekFields.of(locale).weekOfWeekBasedYear()
        val weekOfYear = currentDate.plusWeeks(0L).get(weekOfYearField)
        val calendarWeek = CalendarWeek(currentDate.year, weekOfYear, locale)

        return load(rootPath, "$year/", "week-$year-$week") ?: calendarWeek
    }

    /**
     * Load the data class from JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param path the path
     * @param baseName the base name
     * @return the data class
     */
    fun load(rootPath: File, path: String, baseName: String): CalendarWeek? {
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
    fun load(item: File): CalendarWeek? {
        if (!item.exists()) return null
        if (!item.name.startsWith("week-")) return null

        FileReader(item).use { fileReader ->
            Timber.i("Try to load from ${item.name}")
            if (item.absolutePath.endsWith("-v2.json")) {
                moshi.adapter(CalendarWeek::class.java)
                    .fromJson(fileReader.readText())?.let { return it }
            } else {
                moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarWeek::class.java)
                    .fromJson(fileReader.readText())?.let { return CalendarWeek.convert(it) }
            }
        }

        return null
    }

    /**
     * Convert the data class to JSON.
     *
     * @param calendarWeek the calendar week
     * @return the JSON
     */
    fun json(calendarWeek: CalendarWeek): String {
        val adapter = moshi.adapter(CalendarWeek::class.java)
        return adapter.toJson(calendarWeek)
    }

    /**
     * Save the data class to JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param currentDate the current date
     * @param calendarWeek the data class
     */
    fun save(rootPath: File, currentDate: LocalDate, calendarWeek: CalendarWeek) {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val week = currentDate.format(DateTimeFormatter.ofPattern("ww", calendarWeek.locale))

        calendarWeek.created = calendarWeek.created ?: Date.from(Instant.now())
        calendarWeek.updated = Date.from(Instant.now())
        save(rootPath, "$year/", "week-$year-$week", calendarWeek)
    }

    fun save(rootPath: File, path: String, baseName: String, calendarWeek: CalendarWeek) {
        val fullPath = File(rootPath, "calendar/$path")
        fullPath.mkdirs()

        // Try to save to v2
        PrintWriter(FileWriter(File(fullPath, "$baseName-v2.json"))).use { it.write(json(calendarWeek)) }

        // Try to rename the old file to .backup
        val source = File(fullPath, "$baseName.json")
        if (source.exists()) {
            Files.move(source.toPath(), source.toPath().resolveSibling("$baseName.json.backup"))
        }
    }
}
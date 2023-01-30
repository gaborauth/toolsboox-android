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
     * Load the data class from JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param currentDate the current date
     * @param locale the current locale
     */
    fun load(rootPath: File, currentDate: LocalDate, locale: Locale): CalendarYear {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val calendarYear = CalendarYear(currentDate.year, locale)

        val path = File(rootPath, "calendar/$year/")
        val baseName = "year-$year"

        load(File(path, "$baseName-v2.json"))?.let { return it }
        load(File(path, "$baseName.json"))?.let { return it }

        return calendarYear
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
     * Save the data class to JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param currentDate the current date
     * @param calendarYear the data class
     */
    fun save(rootPath: File, currentDate: LocalDate, calendarYear: CalendarYear) {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))

        val path = File(rootPath, "calendar/$year/")
        path.mkdirs()

        val baseName = "year-$year"

        // Try to save to v2
        PrintWriter(FileWriter(File(path, "$baseName-v2.json"))).use {
            val adapter = moshi.adapter(CalendarYear::class.java)
            it.write(adapter.toJson(calendarYear))
        }

        // Try to rename the old file to .backup
        val source = File(path, "$baseName.json")
        if (source.exists()) {
            Files.move(source.toPath(), source.toPath().resolveSibling("$baseName.json.backup"))
        }
    }
}
package com.toolsboox.plugin.calendar.fi

import com.squareup.moshi.Moshi
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
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
 * Calendar pattern data service.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarPatternService @Inject constructor() {
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
    fun load(rootPath: File, currentDate: LocalDate, locale: Locale): CalendarPattern {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))

        val path = File(rootPath, "calendar/$year/")

        val baseName = "pattern-$year"

        val calendarPattern = CalendarPattern(currentDate.year, locale).fill()

        load(File(path, "$baseName-v1.json"))?.let { return it }
        load(File(path, "$baseName.json"))?.let { return it }

        return calendarPattern
    }

    /**
     * Load the data class from JSON file on the specified path.
     *
     * @param item the file
     * @return optional data class instance
     */
    fun load(item: File): CalendarPattern? {
        if (item.exists()) {
            FileReader(item).use { fileReader ->
                Timber.i("Try to load from ${item.name}")
                if (item.absolutePath.endsWith("-v1.json")) {
                    moshi.adapter(CalendarPattern::class.java)
                        .fromJson(fileReader.readText())?.let { return it }
                } else {
                    moshi.adapter(CalendarPattern::class.java)
                        .fromJson(fileReader.readText())?.let { return it }
                }
            }
        }

        return null
    }

    /**
     * Save the data class to JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param currentDate the current date
     * @param calendarPattern the data class
     */
    fun save(rootPath: File, currentDate: LocalDate, calendarPattern: CalendarPattern) {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))

        val path = File(rootPath, "calendar/$year/")
        path.mkdirs()

        val baseName = "pattern-$year"

        PrintWriter(FileWriter(File(path, "$baseName-v1.json"))).use {
            val adapter = moshi.adapter(CalendarPattern::class.java)
            it.write(adapter.toJson(calendarPattern))
        }

        // Try to rename the old file to .backup
        val source = File(path, "$baseName.json")
        if (source.exists()) {
            Files.move(source.toPath(), source.toPath().resolveSibling("$baseName.json.backup"))
        }
    }
}
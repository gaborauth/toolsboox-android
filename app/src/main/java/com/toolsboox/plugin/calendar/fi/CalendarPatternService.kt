package com.toolsboox.plugin.calendar.fi

import com.squareup.moshi.Moshi
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
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

        if (File(path, "$baseName-v1.json").exists()) {
            Timber.i("Load from $baseName-v1.json")
            FileReader(File(path, "$baseName-v1.json")).use { fileReader ->
                moshi.adapter(CalendarPattern::class.java)
                    .fromJson(fileReader.readText())?.let {
                        return it
                    }
            }
        }

        if (File(path, "$baseName.json").exists()) {
            Timber.i("Load from $baseName.json")
            FileReader(File(path, "$baseName.json")).use { fileReader ->
                moshi.adapter(CalendarPattern::class.java)
                    .fromJson(fileReader.readText())?.let {
                        return it
                    }
            }
        }

        return calendarPattern
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
    }
}
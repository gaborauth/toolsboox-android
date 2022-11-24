package com.toolsboox.plugin.calendar.fi

import com.squareup.moshi.Moshi
import com.toolsboox.plugin.calendar.da.v2.CalendarQuarter
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
 * Calendar quarter data service.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarQuarterService @Inject constructor() {
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
    fun load(rootPath: File, currentDate: LocalDate, locale: Locale): CalendarQuarter {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val quarter = currentDate.format(DateTimeFormatter.ofPattern("QQ"))

        val calendarQuarter = CalendarQuarter(currentDate.year, (currentDate.monthValue - 1) / 3 + 1, locale)

        val path = File(rootPath, "calendar/$year/")
        val baseName = "quarter-$year-$quarter"

        // Try to load from v2
        if (File(path, "$baseName-v2.json").exists()) {
            FileReader(File(path, "$baseName-v2.json")).use { fileReader ->
                Timber.i("Load from $baseName-v2.json")
                moshi.adapter(CalendarQuarter::class.java)
                    .fromJson(fileReader.readText())?.let { return it }
            }
        }

        // Try to load from (v1)
        if (File(path, "$baseName.json").exists()) {
            FileReader(File(path, "$baseName.json")).use { fileReader ->
                Timber.i("Load from $baseName.json")
                moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarQuarter::class.java)
                    .fromJson(fileReader.readText())?.let { return CalendarQuarter.convert(it) }
            }
        }

        return calendarQuarter
    }

    /**
     * Save the data class to JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param currentDate the current date
     * @param calendarQuarter the data class
     */
    fun save(rootPath: File, currentDate: LocalDate, calendarQuarter: CalendarQuarter) {
        val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        val quarter = currentDate.format(DateTimeFormatter.ofPattern("QQ"))

        val path = File(rootPath, "calendar/$year/")
        path.mkdirs()

        val baseName = "quarter-$year-$quarter"

        // Try to save to v2
        PrintWriter(FileWriter(File(path, "$baseName-v2.json"))).use {
            val adapter = moshi.adapter(CalendarQuarter::class.java)
            it.write(adapter.toJson(calendarQuarter))
        }
    }
}
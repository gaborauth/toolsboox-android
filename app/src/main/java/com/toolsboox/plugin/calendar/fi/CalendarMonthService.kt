package com.toolsboox.plugin.calendar.fi

import com.squareup.moshi.Moshi
import com.toolsboox.plugin.calendar.da.v2.CalendarMonth
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

        val path = File(rootPath, "calendar/$year/$month/")
        val baseName = "month-$year-$month"

        // Try to load from v2
        if (File(path, "$baseName-v2.json").exists()) {
            FileReader(File(path, "$baseName-v2.json")).use { fileReader ->
                Timber.i("Load from $baseName-v2.json")
                moshi.adapter(CalendarMonth::class.java)
                    .fromJson(fileReader.readText())?.let { return it }
            }
        }

        // Try to load from (v1)
        if (File(path, "$baseName.json").exists()) {
            FileReader(File(path, "$baseName.json")).use { fileReader ->
                Timber.i("Load from $baseName.json")
                moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarMonth::class.java)
                    .fromJson(fileReader.readText())?.let { return CalendarMonth.convert(it) }
            }
        }

        return calendarMonth
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

        val path = File(rootPath, "calendar/$year/$month/")
        path.mkdirs()

        val baseName = "month-$year-$month"

        // Try to save to v2
        PrintWriter(FileWriter(File(path, "$baseName-v2.json"))).use {
            val adapter = moshi.adapter(CalendarMonth::class.java)
            it.write(adapter.toJson(calendarMonth))
        }
    }
}
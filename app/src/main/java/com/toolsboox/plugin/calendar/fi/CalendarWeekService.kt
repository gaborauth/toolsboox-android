package com.toolsboox.plugin.calendar.fi

import com.squareup.moshi.Moshi
import com.toolsboox.plugin.calendar.da.v2.CalendarWeek
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
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

        val path = File(rootPath, "calendar/$year/")
        val baseName = "week-$year-$week"

        // Try to load from v2
        if (File(path, "$baseName-v2.json").exists()) {
            FileReader(File(path, "$baseName-v2.json")).use { fileReader ->
                Timber.i("Load from $baseName-v2.json")
                moshi.adapter(CalendarWeek::class.java)
                    .fromJson(fileReader.readText())?.let { return it }
            }
        }

        // Try to load from (v1)
        if (File(path, "$baseName.json").exists()) {
            FileReader(File(path, "$baseName.json")).use { fileReader ->
                Timber.i("Load from $baseName.json")
                moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarWeek::class.java)
                    .fromJson(fileReader.readText())?.let { return CalendarWeek.convert(it) }
            }
        }

        return calendarWeek
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

        val path = File(rootPath, "calendar/$year/")
        path.mkdirs()

        val baseName = "week-$year-$week"

        // Try to save to v2
        PrintWriter(FileWriter(File(path, "$baseName-v2.json"))).use {
            val adapter = moshi.adapter(CalendarWeek::class.java)
            it.write(adapter.toJson(calendarWeek))
        }
    }
}
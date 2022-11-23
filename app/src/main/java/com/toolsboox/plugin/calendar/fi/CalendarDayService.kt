package com.toolsboox.plugin.calendar.fi

import com.squareup.moshi.Moshi
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDate
import java.util.*
import javax.inject.Inject

/**
 * Calendar day data service.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarDayService @Inject constructor() {
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
    fun load(rootPath: File, currentDate: LocalDate, locale: Locale): CalendarDay {
        val year = currentDate.year
        val month = currentDate.monthValue
        val day = currentDate.dayOfMonth
        val calendarDay = CalendarDay(year, month, day, locale)

        val path = File(rootPath, "calendar/$year/$month/")
        val baseName = "day-$year-$month-$day"

        // Try to load from v2
        if (File(path, "$baseName-v2.json").exists()) {
            FileReader(File(path, "$baseName-v2.json")).use { fileReader ->
                Timber.i("Load from $baseName-v2.json")
                moshi.adapter(CalendarDay::class.java)
                    .fromJson(fileReader.readText())?.let { return it }
            }
        }

        // Try to load from (v1)
        if (File(path, "$baseName.json").exists()) {
            FileReader(File(path, "$baseName.json")).use { fileReader ->
                Timber.i("Load from $baseName.json")
                moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarDay::class.java)
                    .fromJson(fileReader.readText())?.let { return CalendarDay.convert(it) }
            }
        }

        return calendarDay
    }

    /**
     * Save the data class to JSON file on the specified path.
     *
     * @param rootPath the root path
     * @param currentDate the current date
     * @param calendarDay the data class
     */
    fun save(rootPath: File, currentDate: LocalDate, calendarDay: CalendarDay) {
        val year = currentDate.year
        val month = currentDate.monthValue
        val day = currentDate.dayOfMonth

        val path = File(rootPath, "calendar/$year/$month/")
        path.mkdirs()

        val baseName = "day-$year-$month-$day"

        // Try to save to v2
        PrintWriter(FileWriter(File(path, "$baseName-v2.json"))).use {
            val adapter = moshi.adapter(CalendarDay::class.java)
            it.write(adapter.toJson(calendarDay))
        }
    }
}
package com.toolsboox.plugin.calendar.da

import com.squareup.moshi.JsonClass
import java.util.*

/**
 * Calendar pattern data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class CalendarPattern(
    val year: Int,
    val locale: Locale,

    var yearPage: String = "",
    var yearNote: String = "",
    var quarterPage: String = "",
    var quarterNote: String = "",
    var monthPage: String = "",
    var monthNote: String = "",
    var weekPage: String = "",
    var weekNote: String = "",
    var dayPage: String = "",
    var dayNote: String = ""
) {
    /**
     * Fill the patterns with empty zeros.
     *
     * @return the data class
     */
    fun fill(): CalendarPattern {
        yearPage = "0"
        yearNote = "0"

        quarterPage = "0000"
        quarterNote = "0000"

        monthPage = "000000000000"
        monthNote = "000000000000"

        weekPage = ""
        for (i in 1..54) weekPage += "0"
        weekNote = ""
        for (i in 1..54) weekNote += "0"

        dayPage = ""
        for (i in 1..366) dayPage += "0"
        dayNote = ""
        for (i in 1..366) dayNote += "0"

        return this
    }

    /**
     * Update year pattern.
     *
     * @param pages the number of pages
     * @param notes the number of notes
     */
    fun updateYear(pages: Int, notes: Int) {
        yearPage = setIndex(yearPage, 0, (pages + 48).toChar())
        yearNote = setIndex(yearNote, 0, (notes + 48).toChar())
    }

    /**
     * Update quarter pattern.
     *
     * @param quarter the quarter of year
     * @param pages the number of pages
     * @param notes the number of notes
     */
    fun updateQuarter(quarter: Int, pages: Int, notes: Int) {
        quarterPage = setIndex(quarterPage, quarter - 1, (pages + 48).toChar())
        quarterNote = setIndex(quarterNote, quarter - 1, (notes + 48).toChar())
    }

    /**
     * Update month pattern.
     *
     * @param month the month of year
     * @param pages the number of pages
     * @param notes the number of notes
     */
    fun updateMonth(month: Int, pages: Int, notes: Int) {
        monthPage = setIndex(monthPage, month - 1, (pages + 48).toChar())
        monthNote = setIndex(monthNote, month - 1, (notes + 48).toChar())
    }

    /**
     * Update week pattern.
     *
     * @param weekOfYear the week of year
     * @param pages the number of pages
     * @param notes the number of notes
     */
    fun updateWeek(weekOfYear: Int, pages: Int, notes: Int) {
        weekPage = setIndex(weekPage, weekOfYear - 1, (pages + 48).toChar())
        weekNote = setIndex(weekNote, weekOfYear - 1, (notes + 48).toChar())
    }

    /**
     * Update day pattern.
     *
     * @param dayOfYear the day of year
     * @param pages the number of pages
     * @param notes the number of notes
     */
    fun updateDay(dayOfYear: Int, pages: Int, notes: Int) {
        dayPage = setIndex(dayPage, dayOfYear - 1, (pages + 48).toChar())
        dayNote = setIndex(dayNote, dayOfYear - 1, (notes + 48).toChar())
    }

    /**
     * Set the index to the 'number'.
     *
     * @param item the string
     * @param index the index
     * @param char the char
     */
    private fun setIndex(item: String, index: Int, char: Char): String {
        if (item.length <= index) return item
        val list = item.toMutableList()
        list[index] = char
        return list.joinToString(separator = "")
    }
}
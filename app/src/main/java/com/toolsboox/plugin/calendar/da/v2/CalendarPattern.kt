package com.toolsboox.plugin.calendar.da.v2

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
     * Get number of pages.
     *
     * @return number of pages
     */
    fun getYearPages(): Int {
        return yearPage[0].digitToInt()
    }

    /**
     * Get number of notes.
     *
     * @return number of notes
     */
    fun getYearNotes(): Int {
        return yearNote[0].digitToInt()
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
     * Get number of pages of the quarter.
     *
     * @param quarterOfYear quarter of the year
     * @return number of pages
     */
    fun getQuarterPages(quarterOfYear: Int): Int {
        if (quarterPage.length <= quarterOfYear - 1) return 0

        return quarterPage[quarterOfYear - 1].digitToInt()
    }

    /**
     * Get number of notes of the quarter.
     *
     * @param quarterOfYear quarter of the year
     * @return number of notes
     */
    fun getQuarterNotes(quarterOfYear: Int): Int {
        if (quarterNote.length <= quarterOfYear - 1) return 0

        return quarterNote[quarterOfYear - 1].digitToInt()
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
     * Get number of pages of the month.
     *
     * @param monthOfYear month of the year
     * @return number of pages
     */
    fun getMonthPages(monthOfYear: Int): Int {
        if (monthPage.length <= monthOfYear - 1) return 0

        return monthPage[monthOfYear - 1].digitToInt()
    }

    /**
     * Get number of notes of the month.
     *
     * @param monthOfYear month of the year
     * @return number of notes
     */
    fun getMonthNotes(monthOfYear: Int): Int {
        if (monthNote.length <= monthOfYear - 1) return 0

        return monthNote[monthOfYear - 1].digitToInt()
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
     * Get number of pages of the week.
     *
     * @param weekOfYear week of the year
     * @return number of pages
     */
    fun getWeekPages(weekOfYear: Int): Int {
        if (weekPage.length <= weekOfYear - 1) return 0

        return weekPage[weekOfYear - 1].digitToInt()
    }

    /**
     * Get number of notes of the week.
     *
     * @param weekOfYear week of the year
     * @return number of notes
     */
    fun getWeekNotes(weekOfYear: Int): Int {
        if (weekNote.length <= weekOfYear - 1) return 0

        return weekNote[weekOfYear - 1].digitToInt()
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
     * Get number of pages of the day.
     *
     * @param dayOfYear day of the year
     * @return number of pages
     */
    fun getDayPages(dayOfYear: Int): Int {
        if (dayPage.length <= dayOfYear - 1) return 0

        return dayPage[dayOfYear - 1].digitToInt()
    }

    /**
     * Get number of notes of the day.
     *
     * @param dayOfYear day of the year
     * @return number of notes
     */
    fun getDayNotes(dayOfYear: Int): Int {
        if (dayNote.length <= dayOfYear - 1) return 0

        return dayNote[dayOfYear - 1].digitToInt()
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

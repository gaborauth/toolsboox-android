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
        for (i in 1..53) weekPage += "0"
        weekNote = ""
        for (i in 1..53) weekNote += "0"

        dayPage = ""
        for (i in 1..366) dayPage += "0"
        dayNote = ""
        for (i in 1..366) dayNote += "0"

        return this
    }

    /**
     * Set the index to the 'number'.
     *
     * @param item the string
     * @param index the index
     * @param number the number
     */
    fun setIndex(item: String, index: Int, number: Int): String {
        if (item.length <= index) return item
        val list = item.toMutableList()
        list[index] = "$number"[0]
        return list.joinToString(separator = "")
    }
}

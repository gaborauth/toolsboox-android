package com.toolsboox.plugin.calendar

import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.toolsboox.R
import com.toolsboox.ui.plugin.ScreenFragment
import timber.log.Timber
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*

/**
 * Navigator methods of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
object CalendarNavigator {

    /**
     * Navigate to the daily calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     * @param notes navigate to the notes page
     */
    fun toDay(fragment: ScreenFragment, localDate: LocalDate, notes: Boolean) {
        val year = localDate.year
        val month = localDate.monthValue
        val day = localDate.dayOfMonth

        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("month", "$month")
        bundle.putString("day", "$day")
        bundle.putString("notes", "$notes")

        Timber.i("Navigate to the '$year-$month-$day' ($notes) daily calendar")
        fragment.findNavController().navigate(R.id.action_to_calendar_day, bundle)
    }

    /**
     * Navigate to the monthly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     * @param notes navigate to the notes page
     */
    fun toMonth(fragment: ScreenFragment, localDate: LocalDate, notes: Boolean) {
        val year = localDate.year
        val month = localDate.monthValue

        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("month", "$month")
        bundle.putString("notes", "$notes")

        Timber.i("Navigate to the '$year-$month' ($notes) monthly calendar")
        fragment.findNavController().navigate(R.id.action_to_calendar_month, bundle)
    }

    /**
     * Navigate to the quarterly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     * @param notes navigate to the notes page
     */
    fun toQuarter(fragment: ScreenFragment, localDate: LocalDate, notes: Boolean) {
        val year = localDate.year
        val month = localDate.monthValue
        val quarter = (month - 1) / 3 + 1

        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("quarter", "$quarter")
        bundle.putString("notes", "$notes")

        Timber.i("Navigate to the '$year-$quarter' ($notes) quarterly calendar")
        fragment.findNavController().navigate(R.id.action_to_calendar_quarter, bundle)
    }

    /**
     * Navigate to the weekly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     * @param notes navigate to the notes page
     */
    fun toWeek(fragment: ScreenFragment, localDate: LocalDate, locale: Locale, notes: Boolean) {
        val year = localDate.year
        val weekOfWeekBasedYear = WeekFields.of(locale).weekOfWeekBasedYear()
        val weekOfYear = localDate.plusWeeks(0L).get(weekOfWeekBasedYear)

        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("weekOfYear", "$weekOfYear")
        bundle.putString("notes", "$notes")

        Timber.i("Navigate to the '$year-$weekOfYear' ($notes) weekly calendar")
        fragment.findNavController().navigate(R.id.action_to_calendar_week, bundle)
    }

    /**
     * Navigate to the yearly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     * @param notes navigate to the notes page
     */
    fun toYear(fragment: ScreenFragment, localDate: LocalDate, notes: Boolean) {
        val year = localDate.year

        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("notes", "$notes")

        Timber.i("Navigate to the '$year' ($notes) yearly calendar")
        fragment.findNavController().navigate(R.id.action_to_calendar_year, bundle)
    }
}
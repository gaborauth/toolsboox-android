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
     * @param extended navigate to the extended page
     */
    fun toDay(fragment: ScreenFragment, localDate: LocalDate, extended: Boolean = false) {
        val year = localDate.year
        val month = localDate.monthValue
        val day = localDate.dayOfMonth

        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("month", "$month")
        bundle.putString("day", "$day")
        bundle.putString("extended", "$extended")

        Timber.i("Navigate to the '$year-$month-$day' ($extended) daily calendar")
        fragment.findNavController().navigate(R.id.action_to_calendar_day, bundle)
    }

    /**
     * Navigate to the monthly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     * @param extended navigate to the extended page
     */
    fun toMonth(fragment: ScreenFragment, localDate: LocalDate, extended: Boolean = false) {
        val year = localDate.year
        val month = localDate.monthValue

        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("month", "$month")
        bundle.putString("extended", "$extended")

        Timber.i("Navigate to the '$year-$month' ($extended) monthly calendar")
        fragment.findNavController().navigate(R.id.action_to_calendar_month, bundle)
    }

    /**
     * Navigate to the quarterly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     * @param extended navigate to the extended page
     */
    fun toQuarter(fragment: ScreenFragment, localDate: LocalDate, extended: Boolean = false) {
        val year = localDate.year
        val month = localDate.monthValue
        val quarter = (month - 1) / 3 + 1

        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("quarter", "$quarter")

        if (extended) {
            Timber.i("Navigate to the '$year-$quarter' quarterly extended calendar")
            fragment.findNavController().navigate(R.id.action_to_calendar_extended_quarter, bundle)
        } else {
            Timber.i("Navigate to the '$year-$quarter' quarterly calendar")
            fragment.findNavController().navigate(R.id.action_to_calendar_quarter, bundle)
        }
    }

    /**
     * Navigate to the weekly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     * @param extended navigate to the extended page
     */
    fun toWeek(fragment: ScreenFragment, localDate: LocalDate, locale: Locale?, extended: Boolean = false) {
        val year = localDate.year
        val weekOfWeekBasedYear = WeekFields.of(locale ?: Locale.getDefault()).weekOfWeekBasedYear()
        val weekOfYear = localDate.plusWeeks(0L).get(weekOfWeekBasedYear)

        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("weekOfYear", "$weekOfYear")
        bundle.putString("extended", "$extended")

        Timber.i("Navigate to the '$year-$weekOfYear' ($extended) weekly calendar")
        fragment.findNavController().navigate(R.id.action_to_calendar_week, bundle)
    }

    /**
     * Navigate to the yearly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     * @param extended navigate to the extended page
     */
    fun toYear(fragment: ScreenFragment, localDate: LocalDate, extended: Boolean = false) {
        val year = localDate.year

        val bundle = bundleOf()
        bundle.putString("year", "$year")

        if (extended) {
            Timber.i("Navigate to the '$year' yearly extended calendar")
            fragment.findNavController().navigate(R.id.action_to_calendar_extended_year, bundle)
        } else {
            Timber.i("Navigate to the '$year' yearly calendar")
            fragment.findNavController().navigate(R.id.action_to_calendar_year, bundle)
        }
    }
}
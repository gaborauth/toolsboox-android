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
     */
    fun toDay(fragment: ScreenFragment, localDate: LocalDate) {
        val year = localDate.year
        val month = localDate.monthValue
        val day = localDate.dayOfMonth
        Timber.i("Navigate to the '$year-$month-$day' daily calendar")
        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("month", "$month")
        bundle.putString("day", "$day")
        fragment.findNavController().navigate(R.id.action_to_calendar_day, bundle)
    }

    /**
     * Navigate to the monthly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     */
    fun toMonth(fragment: ScreenFragment, localDate: LocalDate) {
        val year = localDate.year
        val month = localDate.monthValue

        Timber.i("Navigate to the '$year-$month' monthly calendar")
        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("month", "$month")
        fragment.findNavController().navigate(R.id.action_to_calendar_month, bundle)
    }

    /**
     * Navigate to the quarterly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     */
    fun toQuarter(fragment: ScreenFragment, localDate: LocalDate) {
        val year = localDate.year
        val month = localDate.monthValue
        val quarter = (month - 1) / 3 + 1
        Timber.i("Navigate to the '$year-$quarter' quarterly calendar")
        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("quarter", "$quarter")
        fragment.findNavController().navigate(R.id.action_to_calendar_quarter, bundle)
    }

    /**
     * Navigate to the weekly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     */
    fun toWeek(fragment: ScreenFragment, localDate: LocalDate, locale: Locale?) {
        val year = localDate.year
        val weekOfWeekBasedYear = WeekFields.of(locale ?: Locale.getDefault()).weekOfWeekBasedYear()
        val weekOfYear = localDate.plusWeeks(0L).get(weekOfWeekBasedYear)
        Timber.i("Navigate to the '$year-$weekOfYear' weekly calendar")
        val bundle = bundleOf()
        bundle.putString("year", "$year")
        bundle.putString("weekOfYear", "$weekOfYear")
        fragment.findNavController().navigate(R.id.action_to_calendar_week, bundle)
    }

    /**
     * Navigate to the yearly calendar.
     *
     * @param fragment the fragment
     * @param localDate the local date
     */
    fun toYear(fragment: ScreenFragment, localDate: LocalDate) {
        val year = localDate.year
        Timber.i("Navigate to the '$year' yearly calendar")
        val bundle = bundleOf()
        bundle.putString("year", "$year")
        fragment.findNavController().navigate(R.id.action_to_calendar_year, bundle)
    }
}
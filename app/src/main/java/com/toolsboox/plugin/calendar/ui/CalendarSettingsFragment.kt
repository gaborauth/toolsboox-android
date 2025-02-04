package com.toolsboox.plugin.calendar.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.toolsboox.R
import com.toolsboox.da.LocaleItem
import com.toolsboox.databinding.FragmentCalendarSettingsBinding
import com.toolsboox.ot.LocaleItemAdapter
import com.toolsboox.ot.NoFilterAdapter
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*
import javax.inject.Inject

/**
 * Calendar settings fragment.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@AndroidEntryPoint
class CalendarSettingsFragment @Inject constructor() : ScreenFragment() {

    /**
     * The shared preferences.
     */
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    /**
     * The Firebase analytics.
     */
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The presenter of the fragment.
     */
    @Inject
    lateinit var presenter: CalendarSettingsPresenter

    /**
     * The inflated layout.
     */
    override val view = R.layout.fragment_calendar_settings

    /**
     * The view binding.
     */
    private lateinit var binding: FragmentCalendarSettingsBinding

    /**
     * The selected locale language tag.
     */
    private lateinit var selectedLocaleLanguageTag: String

    /**
     * The selected start view.
     */
    private var selectedStartView: Int = 0

    /**
     * The selected start hour.
     */
    private var selectedStartHour: Int = -1

    /**
     * The selected note template.
     */
    private var selectedNoteTemplate: Int = 0

    /**
     * OnViewCreated hook.
     *
     * @param view the parent view
     * @param savedInstanceState the saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarSettingsBinding.bind(view)

        toolbar.toolbarPager.visibility = View.GONE
    }

    /**
     * OnResume hook.
     */
    override fun onResume() {
        super.onResume()

        toolbar.root.title = getString(R.string.calendar_main_title, getString(R.string.calendar_settings_title))

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val savedLocaleLanguageTag = sharedPreferences.getString("calendarLocale", Locale.getDefault().toLanguageTag())
        selectedLocaleLanguageTag = savedLocaleLanguageTag ?: Locale.getDefault().toLanguageTag()

        selectedStartView = sharedPreferences.getInt("calendarStartView", 0)
        selectedStartHour = sharedPreferences.getInt("calendarStartHour", 5)
        selectedNoteTemplate = sharedPreferences.getInt("calendarNoteTemplate", 0)

        // Start view settings
        val listOfStartViews = mutableListOf<String>()
        listOfStartViews.add(getString(R.string.calendar_settings_start_view_day))
        listOfStartViews.add(getString(R.string.calendar_settings_start_view_week))
        listOfStartViews.add(getString(R.string.calendar_settings_start_view_month))
        listOfStartViews.add(getString(R.string.calendar_settings_start_view_quarter))
        listOfStartViews.add(getString(R.string.calendar_settings_start_view_year))

        val startViewAdapter = NoFilterAdapter(this.requireContext(), R.layout.list_item_locale, listOfStartViews)
        binding.startViewSpinner.setAdapter(startViewAdapter)
        startViewAdapter.notifyDataSetChanged()

        binding.startViewSpinner.inputType = 0
        binding.startViewSpinner.setOnItemClickListener { _, _, position, _ ->
            requireActivity().currentFocus?.let {
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
            selectedStartView = position
        }

        // Locale settings
        val listOfLocales = mutableListOf<LocaleItem>()
        for (locale in Locale.getAvailableLocales()) {
            listOfLocales.add(LocaleItem(locale.toLanguageTag(), locale.displayName))
        }

        val localeIndex = listOfLocales.indexOfFirst { it.languageTag == selectedLocaleLanguageTag }

        val localeAdapter = LocaleItemAdapter(this.requireContext(), R.layout.list_item_locale, listOfLocales)
        binding.localesSpinner.setAdapter(localeAdapter)
        localeAdapter.notifyDataSetChanged()

        binding.localesSpinner.setOnItemClickListener { _, _, position, _ ->
            requireActivity().currentFocus?.let {
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
            updateLocaleSettings(localeAdapter, position)
        }

        // Start hour settings
        val listOfStartHours = mutableListOf<String>()
        listOfStartHours.add(getString(R.string.calendar_settings_select_start_hour_empty))
        val hourPattern = if (DateFormat.is24HourFormat(context)) "HH" else "ha"
        listOfStartHours.add(LocalTime.of(0, 0, 0).format(DateTimeFormatter.ofPattern(hourPattern)))
        listOfStartHours.add(LocalTime.of(1, 0, 0).format(DateTimeFormatter.ofPattern(hourPattern)))
        listOfStartHours.add(LocalTime.of(2, 0, 0).format(DateTimeFormatter.ofPattern(hourPattern)))
        listOfStartHours.add(LocalTime.of(3, 0, 0).format(DateTimeFormatter.ofPattern(hourPattern)))
        listOfStartHours.add(LocalTime.of(4, 0, 0).format(DateTimeFormatter.ofPattern(hourPattern)))
        listOfStartHours.add(LocalTime.of(5, 0, 0).format(DateTimeFormatter.ofPattern(hourPattern)))
        listOfStartHours.add(LocalTime.of(6, 0, 0).format(DateTimeFormatter.ofPattern(hourPattern)))
        listOfStartHours.add(LocalTime.of(7, 0, 0).format(DateTimeFormatter.ofPattern(hourPattern)))

        val startHourAdapter = NoFilterAdapter(this.requireContext(), R.layout.list_item_locale, listOfStartHours)
        binding.startHourSpinner.setAdapter(startHourAdapter)
        startHourAdapter.notifyDataSetChanged()

        binding.startHourSpinner.inputType = 0
        binding.startHourSpinner.setOnItemClickListener { _, _, position, _ ->
            requireActivity().currentFocus?.let {
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
            selectedStartHour = position - 1
        }

        // Note template
        val listOfNoteTemplates = mutableListOf<String>()
        listOfNoteTemplates.add(getString(R.string.calendar_settings_select_note_template_lines))
        listOfNoteTemplates.add(getString(R.string.calendar_settings_select_note_template_grid))

        val noteTemplateAdapter = NoFilterAdapter(this.requireContext(), R.layout.list_item_locale, listOfNoteTemplates)
        binding.noteTemplateSpinner.setAdapter(noteTemplateAdapter)
        noteTemplateAdapter.notifyDataSetChanged()

        binding.noteTemplateSpinner.inputType = 0
        binding.noteTemplateSpinner.setOnItemClickListener { _, _, position, _ ->
            requireActivity().currentFocus?.let {
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
            selectedNoteTemplate = position
        }

        // Create shortcut of calendar
        binding.buttonShortcut.setOnClickListener {
            presenter.createShortcut(this@CalendarSettingsFragment, binding)
        }

        // Sync the patterns of the calendar
        binding.buttonPatternSync.setOnClickListener {
            val languageTag = sharedPreferences.getString("calendarLocale", null)
            val locale = languageTag?.let { Locale.forLanguageTag(it) }
            presenter.patternSync(this@CalendarSettingsFragment, binding, locale ?: Locale.getDefault())
        }

        // Export the calendar
        binding.buttonBackup.setOnClickListener {
            presenter.export(this@CalendarSettingsFragment, binding)
        }

        // Save and back
        binding.buttonSave.setOnClickListener {
            sharedPreferences.edit().putString("calendarLocale", selectedLocaleLanguageTag).apply()
            sharedPreferences.edit().putInt("calendarStartView", selectedStartView).apply()
            sharedPreferences.edit().putInt("calendarStartHour", selectedStartHour).apply()
            sharedPreferences.edit().putInt("calendarNoteTemplate", selectedNoteTemplate).apply()
            this@CalendarSettingsFragment.requireActivity().onBackPressed()
        }

        binding.buttonBack.setOnClickListener {
            this@CalendarSettingsFragment.requireActivity().onBackPressed()
        }

        // Set start of spinners
        if (localeIndex > -1) {
            binding.localesSpinner.setText(listOfLocales[localeIndex].toString())
            updateLocaleSettings(localeAdapter, localeIndex)
        }

        binding.startViewSpinner.setText(listOfStartViews[selectedStartView])
        binding.startHourSpinner.setText(listOfStartHours[selectedStartHour + 1])
        binding.noteTemplateSpinner.setText(listOfNoteTemplates[selectedNoteTemplate])
    }

    /**
     * Update the locale settings fields.
     *
     * @param adapter the adapter
     * @param position the position of the locale in the list
     */
    private fun updateLocaleSettings(adapter: LocaleItemAdapter, position: Int) {
        val localeItem = adapter.getItem(position)
        val locale = Locale.forLanguageTag(localeItem.languageTag)
        val localDate = LocalDate.now()

        val weekFields = WeekFields.of(locale)
        val startWeekDate = localDate
            .with(weekFields.dayOfWeek(), 1)

        val dayName = when (startWeekDate.dayOfWeek.value) {
            DayOfWeek.MONDAY.value -> DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, Locale.getDefault())
            DayOfWeek.TUESDAY.value -> DayOfWeek.TUESDAY.getDisplayName(TextStyle.FULL, Locale.getDefault())
            DayOfWeek.WEDNESDAY.value -> DayOfWeek.WEDNESDAY.getDisplayName(TextStyle.FULL, Locale.getDefault())
            DayOfWeek.THURSDAY.value -> DayOfWeek.THURSDAY.getDisplayName(TextStyle.FULL, Locale.getDefault())
            DayOfWeek.FRIDAY.value -> DayOfWeek.FRIDAY.getDisplayName(TextStyle.FULL, Locale.getDefault())
            DayOfWeek.SATURDAY.value -> DayOfWeek.SATURDAY.getDisplayName(TextStyle.FULL, Locale.getDefault())
            DayOfWeek.SUNDAY.value -> DayOfWeek.SUNDAY.getDisplayName(TextStyle.FULL, Locale.getDefault())
            else -> "?"
        }

        binding.calendarFirstDayOfTheWeekValue.text = dayName

        val firstWeek = LocalDate.of(localDate.year, 1, 1)
            .get(WeekFields.of(locale).weekOfWeekBasedYear())
        val lastWeek = LocalDate.of(localDate.year + 1, 1, 1).minusDays(1L)
            .get(WeekFields.of(locale).weekOfWeekBasedYear())

        binding.calendarWeekNumberOfFirstDayValue.text = requireContext().getString(R.string.week_abbreviation, firstWeek)
        binding.calendarWeekNumberOfLastDayValue.text = requireContext().getString(R.string.week_abbreviation, lastWeek)

        selectedLocaleLanguageTag = localeItem.languageTag
    }

    /**
     * OnPause hook.
     */
    override fun onPause() {
        super.onPause()

        toolbar.toolbarPager.visibility = View.GONE
    }

    /**
     * Show the progress bar.
     */
    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    /**
     * Hide the progress bar.
     */
    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }
}

package com.toolsboox.plugin.calendar.ui

import android.graphics.Rect
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.toolsboox.databinding.FragmentCalendarQuarterBinding
import com.toolsboox.plugin.calendar.da.CalendarQuarter
import com.toolsboox.plugin.teamdrawer.nw.domain.Stroke
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Calendar quarter presenter.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarQuarterPresenter @Inject constructor() : FragmentPresenter() {
    /**
     * Load the quarter if available.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     */
    fun load(
        fragment: CalendarQuarterFragment, binding: FragmentCalendarQuarterBinding,
        currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                var calendarQuarter = CalendarQuarter(currentDate.year, currentDate.monthValue / 3 + 1, mutableListOf())

                val rootPath = Environment.getExternalStorageDirectory()
                File("$rootPath/toolsBoox/").mkdirs()
                val filename = currentDate.format(DateTimeFormatter.ofPattern("yyyy-QQ"))
                val path = "$rootPath/toolsBoox/quarter-$filename.json"
                try {
                    if (File(path).exists()) {
                        FileReader(File(path)).use {
                            calendarQuarter = GsonBuilder().create().fromJson(it, CalendarQuarter::class.java)
                            calendarQuarter.normalizeStrokes(1404, 1872, surfaceSize.width(), surfaceSize.height())
                        }
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }

                withContext(Dispatchers.Main) { fragment.renderPage(calendarQuarter) }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }

    /**
     * Save the quarter to the storage.
     *
     * @param fragment the fragment
     * @param binding the data binding
     * @param strokes the strokes to save
     * @param currentDate the current date
     * @param surfaceSize the actual size of surface view
     */
    fun save(
        fragment: CalendarQuarterFragment, binding: FragmentCalendarQuarterBinding,
        strokes: List<Stroke>, currentDate: LocalDate, surfaceSize: Rect
    ) {
        if (!checkPermissions(fragment, binding.root)) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.showLoading() } }

                val listType: Type = object : TypeToken<List<Stroke>>() {}.type
                val gson = Gson()
                val json: String = gson.toJson(strokes, listType)
                val strokesCopy = gson.fromJson<List<Stroke>>(json, listType)

                val calendarQuarter = CalendarQuarter(currentDate.year, currentDate.monthValue / 3 + 1, strokesCopy)
                calendarQuarter.normalizeStrokes(surfaceSize.width(), surfaceSize.height(), 1404, 1872)

                val rootPath = Environment.getExternalStorageDirectory()
                File("$rootPath/toolsBoox/").mkdirs()
                val filename = currentDate.format(DateTimeFormatter.ofPattern("yyyy-QQ"))
                val path = "$rootPath/toolsBoox/quarter-$filename.json"
                try {
                    PrintWriter(FileWriter(File(path))).use {
                        it.write(GsonBuilder().create().toJson(calendarQuarter).toString())
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) { fragment.somethingHappened(e) }
                }
            } finally {
                withContext(Dispatchers.Main) { fragment.runOnActivity { fragment.hideLoading() } }
            }
        }
    }
}

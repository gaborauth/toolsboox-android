package com.toolsboox.plugin.calendar.widget

import android.content.ContentUris
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.CalendarContract
import android.text.TextPaint
import android.text.TextUtils
import android.text.format.DateFormat
import com.squareup.moshi.Moshi
import com.toolsboox.R
import com.toolsboox.da.Stroke
import com.toolsboox.ot.DateJsonAdapter
import com.toolsboox.ot.LocaleJsonAdapter
import com.toolsboox.ot.UUIDJsonAdapter
import com.toolsboox.plugin.calendar.da.v1.CalendarEvent
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import java.io.File
import java.io.FileReader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.min
import kotlin.math.roundToInt

object WidgetRenderer {

    const val CW = 1404f
    const val CH = 1872f

    private const val cew = 600f
    private const val ceh = 50f
    private const val lo = 20f
    private val to = (CH - 35 * ceh) / 2f

    private const val MAX_BITMAP_PX = 768

    enum class Mode { FULL, SCHEDULE, TASKS_NOTES }

    fun render(context: Context, date: LocalDate, widthDp: Int, heightDp: Int, mode: Mode): Bitmap {
        val calendarDay = loadCalendarDay(context, date)
        val events = loadCalendarEvents(context, date)
        val startHour = calendarDay?.startHour ?: 5

        val fullBitmap = Bitmap.createBitmap(CW.toInt(), CH.toInt(), Bitmap.Config.ARGB_8888)
        val fullCanvas = Canvas(fullBitmap)

        drawTemplate(context, fullCanvas, calendarDay, events, startHour)

        if (calendarDay != null) {
            val strokes = calendarDay.calendarStrokes[CalendarDay.DEFAULT_STYLE] ?: emptyList()
            drawStrokes(fullCanvas, strokes)
        }

        val cropped = when (mode) {
            Mode.FULL -> fullBitmap
            Mode.SCHEDULE -> compositeSchedule(fullBitmap)
            Mode.TASKS_NOTES -> compositeTasksNotes(fullBitmap)
        }

        if (mode != Mode.FULL) fullBitmap.recycle()

        val cropW = cropped.width.toFloat()
        val cropH = cropped.height.toFloat()

        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).roundToInt()
        val heightPx = (heightDp * density).roundToInt()

        val fitScale = min(widthPx / cropW, heightPx / cropH)
        val rawW = (cropW * fitScale).roundToInt()
        val rawH = (cropH * fitScale).roundToInt()
        val capScale = min(1f, MAX_BITMAP_PX.toFloat() / maxOf(rawW, rawH))
        val outW = maxOf(1, (rawW * capScale).roundToInt())
        val outH = maxOf(1, (rawH * capScale).roundToInt())

        val outBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val outCanvas = Canvas(outBitmap)
        outCanvas.drawBitmap(cropped, Rect(0, 0, cropped.width, cropped.height), Rect(0, 0, outW, outH), null)
        cropped.recycle()

        return outBitmap
    }

    /**
     * Schedule widget: morning (5am-1pm) on the left, afternoon (1pm-10pm) on the right.
     * Splits the schedule column at cell 17 (1:00pm with startHour=5).
     */
    private fun compositeSchedule(fullBitmap: Bitmap): Bitmap {
        val splitY = (to + 17 * ceh).toInt() // 911
        val xLeft = 0
        val xRight = (lo + cew + 20f).toInt() // 640
        val halfW = xRight - xLeft

        val topH = splitY
        val botH = CH.toInt() - splitY
        val outH = maxOf(topH, botH)

        val out = Bitmap.createBitmap(halfW * 2, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        // Left: morning (top half of schedule, includes header + 5am-1pm)
        canvas.drawBitmap(
            fullBitmap,
            Rect(xLeft, 0, xRight, topH),
            Rect(0, 0, halfW, topH),
            null
        )

        // Right: afternoon (bottom half of schedule, 1pm onwards)
        canvas.drawBitmap(
            fullBitmap,
            Rect(xLeft, splitY, xRight, CH.toInt()),
            Rect(halfW, 0, halfW * 2, botH),
            null
        )

        return out
    }

    /**
     * Tasks/Notes widget: Tasks on the left, Notes on the right (originally stacked vertically).
     */
    private fun compositeTasksNotes(fullBitmap: Bitmap): Bitmap {
        val tasksTop = to.toInt()                       // 61
        val tasksBot = (to + 17 * ceh).toInt()          // 911
        val notesTop = (to + 18 * ceh).toInt()          // 961
        val notesBot = (to + 35 * ceh).toInt()          // 1811
        val xLeft = (lo + cew + 30f).toInt()            // 650
        val xRight = (lo + 2 * cew + 70f).toInt()       // 1290

        val halfW = xRight - xLeft
        val halfH = tasksBot - tasksTop  // 850

        val out = Bitmap.createBitmap(halfW * 2, halfH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        canvas.drawBitmap(
            fullBitmap,
            Rect(xLeft, tasksTop, xRight, tasksBot),
            Rect(0, 0, halfW, halfH),
            null
        )

        canvas.drawBitmap(
            fullBitmap,
            Rect(xLeft, notesTop, xRight, notesBot),
            Rect(halfW, 0, halfW * 2, halfH),
            null
        )

        return out
    }

    // ---- Data loading ----

    private fun buildMoshi(): Moshi = Moshi.Builder()
        .add(LocaleJsonAdapter()).add(DateJsonAdapter()).add(UUIDJsonAdapter()).build()

    fun loadCalendarDay(context: Context, date: LocalDate): CalendarDay? {
        val rootPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        } else {
            File(Environment.getExternalStorageDirectory(), "toolsBoox")
        } ?: return null

        val year = date.format(DateTimeFormatter.ofPattern("yyyy"))
        val month = date.format(DateTimeFormatter.ofPattern("MM"))
        val day = date.format(DateTimeFormatter.ofPattern("dd"))
        val path = File(rootPath, "calendar/$year/$month")

        val v2File = File(path, "day-$year-$month-$day-v2.json")
        val v1File = File(path, "day-$year-$month-$day.json")

        try {
            val moshi = buildMoshi()
            if (v2File.exists()) {
                FileReader(v2File).use { reader ->
                    return moshi.adapter(CalendarDay::class.java).fromJson(reader.readText())
                }
            }
            if (v1File.exists()) {
                FileReader(v1File).use { reader ->
                    val v1 = moshi.adapter(com.toolsboox.plugin.calendar.da.v1.CalendarDay::class.java)
                        .fromJson(reader.readText())
                    return v1?.let { CalendarDay.convert(it) }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    fun loadCalendarEvents(context: Context, date: LocalDate): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val julianDay = 2440588 + date.toEpochDay()

        val uriBuilder = CalendarContract.Instances.CONTENT_BY_DAY_URI.buildUpon()
        ContentUris.appendId(uriBuilder, julianDay)
        ContentUris.appendId(uriBuilder, julianDay)

        val projection = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.START_DAY,
            CalendarContract.Instances.END_DAY
        )

        try {
            context.contentResolver.query(
                uriBuilder.build(), projection,
                CalendarContract.Calendars.VISIBLE + "=?", arrayOf("1"), null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val eventColor = 0xff000000 or (cursor.getLong(1))
                    val calendarColor = 0xff000000 or (cursor.getLong(2))
                    val title = cursor.getString(3) ?: ""
                    val description = cursor.getString(4) ?: ""
                    val allDay = cursor.getInt(5) > 0
                    val dtStart = cursor.getLong(6)
                    val dtEnd = cursor.getLong(7)
                    val startDay = cursor.getLong(8)
                    val endDay = cursor.getLong(9)

                    if (startDay > julianDay || endDay < julianDay) continue

                    events.add(CalendarEvent(id, title, description, allDay, dtStart, dtEnd, calendarColor, eventColor))
                }
            }
        } catch (_: SecurityException) {
        }

        events.sortBy { it.startDate }
        return events
    }

    // ---- Template drawing ----

    fun drawTemplate(
        context: Context, canvas: Canvas, calendarDay: CalendarDay?,
        events: List<CalendarEvent>, startHour: Int
    ) {
        canvas.drawRect(0f, 0f, CW, CH, fillWhite)

        // --- Left column: Schedules ---
        canvas.drawRect(lo, to, lo + cew, to + ceh, fillGrey80)
        canvas.drawText(context.getString(R.string.calendar_day_schedules), lo + 10f, to + ceh - 10f, textDefaultWhite)

        canvas.drawLine(lo, to + ceh, lo + cew, to + ceh, lineBlack)
        for (i in 1..34) {
            if (i % 2 == 1) {
                canvas.drawLine(lo, to + i * ceh, lo + cew, to + i * ceh, lineGrey50)
                canvas.drawText(":00", lo + 115f, to + 35f + i * ceh, textSmallRight)
                if (startHour > -1) {
                    val localTime = LocalTime.of(i / 2 + startHour, 0)
                    if (DateFormat.is24HourFormat(context)) {
                        val hourText = localTime.format(DateTimeFormatter.ofPattern("HH"))
                        canvas.drawText(hourText, lo + 40f, to + 20f + (i + 1) * ceh, text60Center)
                    } else {
                        val hourText = localTime.format(DateTimeFormatter.ofPattern("h"))
                        val ampmText = localTime.format(DateTimeFormatter.ofPattern("a"))
                        canvas.drawText(hourText, lo + 70f, to + 40f + i * ceh, textDefaultRight)
                        canvas.drawText(ampmText, lo + 70f, to + 30f + (i + 1) * ceh, textDefaultRight)
                    }
                }
            } else {
                canvas.drawLine(lo + 80f, to + i * ceh, lo + cew, to + i * ceh, lineGrey50)
                canvas.drawRect(lo + 80f, to + i * ceh, lo + cew, to + i * ceh + ceh, fillGrey20)
                canvas.drawText(":30", lo + 115f, to + 35f + i * ceh, textSmallRight)
            }
        }
        canvas.drawLine(lo, to + 35 * ceh, lo + cew, to + 35 * ceh, lineBlack)
        canvas.drawLine(lo + 120f, to + ceh, lo + 120f, to + 35 * ceh, lineBlack)

        if (calendarDay != null && calendarDay.hasLanes && startHour >= 0) {
            val laneOne = mutableListOf<CalendarEvent>()
            val laneTwo = mutableListOf<CalendarEvent>()

            for (event in events) {
                if (event.allDay) continue
                val s = Instant.ofEpochMilli(event.startDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                if (s.hour * 60 + s.minute < startHour * 60) continue
                val e = Instant.ofEpochMilli(event.endDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                if (e.hour * 60 + e.minute > (startHour + 17) * 60) continue

                if (overlaps(event, laneOne)) {
                    if (!overlaps(event, laneTwo)) laneTwo.add(event)
                } else {
                    laneOne.add(event)
                }
            }

            if (laneOne.isNotEmpty()) drawEventLane(canvas, startHour, laneOne, 120f, (cew - 120f) / 2)
            if (laneTwo.isNotEmpty()) drawEventLane(canvas, startHour, laneTwo, 120f + (cew - 120f) / 2, (cew - 120f) / 2)
        }

        // --- Right column: Tasks ---
        canvas.drawRect(lo + cew + 50f, to, lo + 2 * cew + 50f, to + ceh, fillGrey80)
        canvas.drawText(context.getString(R.string.calendar_day_tasks), lo + cew + 60f, to + ceh - 10f, textDefaultWhite)

        canvas.drawLine(lo + cew + 50f, to + ceh, lo + 2 * cew + 50f, to + ceh, lineBlack)
        for (i in 1..16) {
            canvas.drawLine(lo + cew + 50f, to + i * ceh, lo + 2 * cew + 50f, to + i * ceh, lineGrey50)
            if (i % 2 == 0) {
                canvas.drawRect(lo + cew + 50f, to + i * ceh, lo + 2 * cew + 50f, to + i * ceh + ceh, fillGrey20)
            }
            canvas.drawRect(lo + cew + 60f, to + i * ceh + 10f, lo + cew + 90f, to + i * ceh + 40f, lineGrey50)
        }
        canvas.drawLine(lo + cew + 50f, to + 17 * ceh, lo + 2 * cew + 50f, to + 17 * ceh, lineBlack)
        canvas.drawLine(lo + cew + 100f, to + ceh, lo + cew + 100f, to + 17 * ceh, lineBlack)

        // --- Right column: Notes ---
        val readingProgress = calendarDay?.readingProgress ?: emptyList()
        val outsideEvents = events.filter { it.allDay || calendarDay?.hasLanes != true }

        val notesLabel = if (outsideEvents.isEmpty()) {
            context.getString(R.string.calendar_day_notes)
        } else {
            context.getString(R.string.calendar_day_notes_events).format(outsideEvents.size)
        }
        canvas.drawRect(lo + cew + 50f, to + 18 * ceh, lo + 2 * cew + 50f, to + 19 * ceh, fillGrey80)
        canvas.drawText(notesLabel, lo + cew + 60f, to + 19 * ceh - 10f, textDefaultWhite)

        canvas.drawLine(lo + cew + 50f, to + 19 * ceh, lo + 2 * cew + 50f, to + 19 * ceh, lineBlack)
        for (i in 20..35) {
            canvas.drawLine(lo + cew + 50f, to + i * ceh, lo + 2 * cew + 50f, to + i * ceh, lineGrey50)
            if (i % 2 == 0) {
                canvas.drawRect(lo + cew + 50f, to + i * ceh, lo + 2 * cew + 50f, to + i * ceh + ceh, fillGrey20)
            }
        }
        canvas.drawLine(lo + cew + 50f, to + 35 * ceh, lo + 2 * cew + 50f, to + 35 * ceh, lineBlack)

        val notesTitle = mutableListOf<String>()
        val notesLeft = mutableListOf<String>()
        val notesRight = mutableListOf<String>()

        readingProgress.take(8).forEach {
            if (it.authors == null) notesTitle.add(it.title)
            else notesTitle.add("${it.authors}: ${it.title}")
            notesLeft.add(it.progress ?: "-")
            notesRight.add(DateFormat.getTimeFormat(context).format(it.lastAccess))
        }

        if (notesTitle.size < 8) {
            outsideEvents.take(8 - notesTitle.size).forEach { ev ->
                notesTitle.add(ev.title)
                if (ev.allDay) {
                    notesLeft.add(context.getString(R.string.calendar_day_all_day))
                    notesRight.add("")
                } else {
                    val s = Instant.ofEpochMilli(ev.startDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    val e = Instant.ofEpochMilli(ev.endDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    notesLeft.add(s.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)))
                    notesRight.add(e.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)))
                }
            }
        }

        for (i in 0..7) {
            if (i < notesTitle.size) {
                val ellipsized = TextUtils.ellipsize(notesTitle[i], textDefaultBlack, cew, TextUtils.TruncateAt.END).toString()
                canvas.drawText(ellipsized, lo + cew + 60f, to + (20 + i * 2) * ceh - 10f, textDefaultBlack)
                canvas.drawText(notesLeft[i], lo + cew + 60f, to + (21 + i * 2) * ceh - 10f, textSmall)
                canvas.drawText(notesRight[i], lo + cew + 40f + cew, to + (21 + i * 2) * ceh - 10f, textSmallRight)
            }
        }
    }

    private fun drawEventLane(canvas: Canvas, startHour: Int, lane: List<CalendarEvent>, llo: Float, lw: Float) {
        for (event in lane) {
            val s = Instant.ofEpochMilli(event.startDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val e = Instant.ofEpochMilli(event.endDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val cehs = (s.hour * 60 + s.minute - startHour * 60) / 30f * ceh + ceh
            val cehe = (e.hour * 60 + e.minute - startHour * 60) / 30f * ceh + ceh
            canvas.drawRect(lo + llo + 5f, to + cehs, lo + llo + lw - 5f, to + cehe, fillGrey10)
            canvas.drawRect(lo + llo + 5f, to + cehs, lo + llo + lw - 5f, to + cehe, lineBlack)
            val ellipsized = TextUtils.ellipsize(event.title, textSmall, lw - 20f, TextUtils.TruncateAt.END).toString()
            canvas.drawText(ellipsized, lo + llo + 15f, to + cehs + ceh * 0.66f - 10f, textSmall)
        }
    }

    private fun overlaps(event: CalendarEvent, lane: List<CalendarEvent>): Boolean {
        for (e in lane) {
            if (event.startDate in e.startDate..<e.endDate) return true
            if (e.startDate in event.startDate..<event.endDate) return true
        }
        return false
    }

    fun drawStrokes(canvas: Canvas, strokes: List<Stroke>) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in strokes) {
            val points = stroke.strokePoints
            if (points.isEmpty()) continue

            paint.color = stroke.color
            paint.strokeWidth = stroke.strokeWidth

            val path = Path()
            val pre = PointF(points[0].x, points[0].y)
            if (points.size == 1) {
                path.moveTo(pre.x - 1f, pre.y - 1f)
            } else {
                path.moveTo(pre.x, pre.y)
            }
            for (pt in points) {
                path.quadTo(pre.x, pre.y, pt.x, pt.y)
                pre.x = pt.x
                pre.y = pt.y
            }
            canvas.drawPath(path, paint)
        }
    }

    // ---- Paint objects ----

    private val fillWhite = Paint().apply {
        color = Color.WHITE; style = Paint.Style.FILL_AND_STROKE; strokeWidth = 1f
    }
    private val fillGrey10 = Paint().apply {
        color = Color.argb(255, 230, 230, 230); style = Paint.Style.FILL_AND_STROKE; strokeWidth = 1f
    }
    private val fillGrey20 = Paint().apply {
        color = Color.argb(20, 128, 128, 128); style = Paint.Style.FILL_AND_STROKE; strokeWidth = 1f
    }
    private val fillGrey80 = Paint().apply {
        color = Color.argb(204, 128, 128, 128); style = Paint.Style.FILL_AND_STROKE; strokeWidth = 1f
    }
    private val lineBlack = Paint().apply {
        color = Color.BLACK; strokeWidth = 2f; style = Paint.Style.STROKE
    }
    private val lineGrey50 = Paint().apply {
        color = Color.argb(128, 128, 128, 128); strokeWidth = 2f; style = Paint.Style.STROKE
    }
    private val text60Center = TextPaint().apply {
        color = Color.BLACK; textAlign = Paint.Align.CENTER; textSize = 60f; typeface = Typeface.DEFAULT
    }
    val textDefaultBlack = TextPaint().apply {
        color = Color.BLACK; textSize = 40f; typeface = Typeface.DEFAULT_BOLD
    }
    private val textDefaultRight = TextPaint().apply {
        color = Color.BLACK; textAlign = Paint.Align.RIGHT; textSize = 40f; typeface = Typeface.DEFAULT_BOLD
    }
    private val textDefaultWhite = TextPaint().apply {
        color = Color.WHITE; textSize = 40f; typeface = Typeface.DEFAULT_BOLD
    }
    private val textSmall = TextPaint().apply {
        color = Color.BLACK; textSize = 25f; typeface = Typeface.DEFAULT
    }
    private val textSmallRight = TextPaint().apply {
        color = Color.BLACK; textAlign = Paint.Align.RIGHT; textSize = 25f; typeface = Typeface.DEFAULT
    }
}

package com.toolsboox.plugin.calendar.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.toolsboox.R
import com.toolsboox.ui.main.MainActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Full day-page widget. Renders the entire 1404x1872 planner canvas.
 */
open class CalendarWidgetProvider : AppWidgetProvider() {

    companion object {
        fun refreshAll(context: Context) {
            broadcastUpdate(context, CalendarWidgetProvider::class.java)
            broadcastUpdate(context, ScheduleWidgetProvider::class.java)
            broadcastUpdate(context, TasksNotesWidgetProvider::class.java)
        }

        private fun broadcastUpdate(context: Context, cls: Class<*>) {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, cls))
            if (ids.isEmpty()) return
            val intent = Intent(context, cls).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        fun openAppIntent(context: Context): PendingIntent {
            val today = LocalDate.now()
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("year", today.year.toString())
                putExtra("month", today.monthValue.toString())
                putExtra("day", today.dayOfMonth.toString())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    protected open val mode: WidgetRenderer.Mode = WidgetRenderer.Mode.FULL

    /**
     * Run widget rendering on a background thread — file I/O, calendar queries,
     * and bitmap drawing should never block the main thread.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val pendingResult = goAsync()
            Thread {
                try { super.onReceive(context, intent) }
                finally { pendingResult.finish() }
            }.start()
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
        scheduleMidnightUpdate(context)
    }

    /**
     * Schedule an alarm shortly after midnight so the widget rolls over to the new day.
     * Without this, the widget waits up to 30 minutes (the updatePeriodMillis interval).
     */
    private fun scheduleMidnightUpdate(context: Context) {
        val cls = this::class.java
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, cls))
        if (ids.isEmpty()) return

        val intent = Intent(context, cls).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, cls.name.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextMidnight = LocalDate.now().plusDays(1)
            .atTime(LocalTime.of(0, 0, 5))
            .atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC, nextMidnight, pendingIntent)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    protected fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.calendar_widget_layout)
        val today = LocalDate.now()

        val options = manager.getAppWidgetOptions(widgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180)

        val bitmap = WidgetRenderer.render(context, today, widthDp, heightDp, mode)
        views.setImageViewBitmap(R.id.widget_page_image, bitmap)
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))

        manager.updateAppWidget(widgetId, views)
    }
}

/**
 * Schedule-only widget. Renders the left column (hours grid + event lanes).
 */
class ScheduleWidgetProvider : CalendarWidgetProvider() {
    override val mode: WidgetRenderer.Mode = WidgetRenderer.Mode.SCHEDULE
}

/**
 * Tasks + Notes widget. Renders the right column (tasks on top, notes on bottom).
 */
class TasksNotesWidgetProvider : CalendarWidgetProvider() {
    override val mode: WidgetRenderer.Mode = WidgetRenderer.Mode.TASKS_NOTES
}

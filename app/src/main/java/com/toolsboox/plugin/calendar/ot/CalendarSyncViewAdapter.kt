package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.toolsboox.R
import com.toolsboox.plugin.calendar.da.v1.CalendarSyncViewItem
import com.toolsboox.utils.FluentDuration
import java.time.Instant

/**
 * Calendar sync view adapter.
 *
 * @param context the context
 * @param items the items
 * @param onItemClick the on item click
 */
class CalendarSyncViewAdapter(
    private val context: Context,
    private val items: List<CalendarSyncViewItem>,
    private val onItemClick: ((CalendarSyncViewItem) -> Unit)
) : RecyclerView.Adapter<CalendarSyncViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_calendar_sync, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView = itemView.findViewById<TextView>(R.id.calendar_sync_name)
        private val syncButton = itemView.findViewById<TextView>(R.id.calendar_sync_button)
        private val fileTextView = itemView.findViewById<TextView>(R.id.calendar_sync_file_last_modification)
        private val cloudTextView = itemView.findViewById<TextView>(R.id.calendar_sync_cloud_last_modification)

        fun bindTo(item: CalendarSyncViewItem) {
            nameTextView.text = item.title.baseName

            val fileLastModified = item.file?.updated?.time ?: 0L
            val fileLastModifiedText = FluentDuration.convert(itemView.context, Instant.now().toEpochMilli() - fileLastModified)
            if (fileLastModified == 0L) {
                fileTextView.text = context.getString(R.string.calendar_google_drive_sync_file_missing)
            } else {
                fileTextView.text = context.getString(R.string.calendar_google_drive_sync_file_last_modification, fileLastModifiedText)
            }

            val cloudLastModified = item.cloud?.updated?.time ?: 0L
            val cloudLastModifiedText = FluentDuration.convert(itemView.context, Instant.now().toEpochMilli() - cloudLastModified)
            if (cloudLastModified == 0L) {
                cloudTextView.text = context.getString(R.string.calendar_google_drive_sync_cloud_missing)
            } else {
                cloudTextView.text = context.getString(R.string.calendar_google_drive_sync_cloud_last_modification, cloudLastModifiedText)
            }

            if (fileLastModified < cloudLastModified) {
                syncButton.text = context.getString(R.string.calendar_google_drive_sync_button_download)
            } else {
                syncButton.text = context.getString(R.string.calendar_google_drive_sync_button_upload)
            }

            syncButton.setOnClickListener {
                onItemClick.invoke(item)
            }
        }
    }
}

package com.toolsboox.plugin.calendar.da.v1

/**
 * Calendar sync view item data class.
 *
 * @param title the title item
 * @param file the file item
 * @param cloud the cloud item
 */
data class CalendarSyncViewItem(
    val title: CalendarSyncItem,
    val file: CalendarSyncItem?,
    val cloud: CalendarSyncItem?
)

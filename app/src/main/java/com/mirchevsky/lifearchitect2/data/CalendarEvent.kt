package com.mirchevsky.lifearchitect2.data

/**
 * Represents a single event read from the device's native calendar provider.
 *
 * @param id          The [CalendarContract.Events._ID] of the event.
 * @param title       The event title (display name).
 * @param startMillis Event start time in epoch-milliseconds.
 * @param endMillis   Event end time in epoch-milliseconds.
 * @param isAllDay    True when the event spans the entire day.
 * @param color       The calendar colour associated with the event (ARGB int).
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean,
    val color: Int
)

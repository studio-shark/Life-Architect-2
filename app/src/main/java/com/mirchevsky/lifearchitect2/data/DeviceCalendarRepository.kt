package com.mirchevsky.lifearchitect2.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * DeviceCalendarRepository
 * ─────────────────────────────────────────────────────────────────────────────
 * Reads calendar events from the device's native [CalendarContract] provider
 * and exposes them as reactive [Flow]s that re-emit whenever any calendar
 * change is detected (our app, system calendar app, or any other app that
 * writes to the device calendar).
 *
 * Requires [android.Manifest.permission.READ_CALENDAR] to be granted before
 * calling any method.
 */
class DeviceCalendarRepository(private val context: Context) {

    data class EventUpdate(
        val title: String,
        val startMillis: Long,
        val endMillis: Long,
        val isAllDay: Boolean
    )

    // ── Per-date event list ──────────────────────────────────────────────────

    /**
     * Returns a [Flow] of all [CalendarEvent]s for [date].
     * Re-emits whenever the device calendar data changes.
     * Must only be called when READ_CALENDAR permission is granted.
     */
    fun observeEventsForDate(date: LocalDate): Flow<List<CalendarEvent>> = callbackFlow {
        trySend(queryEventsForDate(date))
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(queryEventsForDate(date)) }
        }
        context.contentResolver.registerContentObserver(
            CalendarContract.Events.CONTENT_URI, true, observer
        )
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()

    // ── Month-level event days ───────────────────────────────────────────────

    /**
     * Returns a [Flow] of the set of [LocalDate]s in [month] that have at
     * least one device calendar event. Re-emits on any calendar change.
     * Must only be called when READ_CALENDAR permission is granted.
     */
    fun observeEventDaysForMonth(month: YearMonth): Flow<Set<LocalDate>> = callbackFlow {
        fun query(): Set<LocalDate> = queryEventDaysForMonth(month)
        trySend(query())
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(query()) }
        }
        context.contentResolver.registerContentObserver(
            CalendarContract.Events.CONTENT_URI, true, observer
        )
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()

    // ── Total event count (current month) ───────────────────────────────────

    /**
     * Returns a [Flow] of the total number of distinct device calendar events
     * in the current calendar month. Re-emits on any calendar change.
     * Must only be called when READ_CALENDAR permission is granted.
     */
    fun observeTotalEventCount(): Flow<Int> = callbackFlow {
        fun query(): Int {
            val zone = ZoneId.systemDefault()
            val month = YearMonth.now()
            val startMillis = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = month.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val selection = "(${CalendarContract.Events.DTSTART} <= ?) AND " +
                    "(${CalendarContract.Events.DTEND} >= ?) AND " +
                    "(${CalendarContract.Events.DELETED} = 0)"
            val selectionArgs = arrayOf(endMillis.toString(), startMillis.toString())
            return try {
                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(CalendarContract.Events._ID),
                    selection,
                    selectionArgs,
                    null
                )?.use { it.count } ?: 0
            } catch (e: SecurityException) { 0 }
        }
        trySend(query())
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(query()) }
        }
        context.contentResolver.registerContentObserver(
            CalendarContract.Events.CONTENT_URI, true, observer
        )
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()

    // ── Synchronous helpers ──────────────────────────────────────────────────

    /**
     * Synchronous query for all events on [date].
     * Returns an empty list if the cursor is null (permission denied or no data).
     */
    fun queryEventsForDate(date: LocalDate): List<CalendarEvent> {
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay   = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.DISPLAY_COLOR
        )

        val selection = "(${CalendarContract.Events.DTSTART} <= ?) AND " +
                "(${CalendarContract.Events.DTEND} >= ?) AND " +
                "(${CalendarContract.Events.DELETED} = 0)"
        val selectionArgs = arrayOf(endOfDay.toString(), startOfDay.toString())

        val cursor = try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )
        } catch (e: SecurityException) {
            return emptyList()
        } ?: return emptyList()

        return cursor.use { c ->
            val idIdx     = c.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleIdx  = c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val startIdx  = c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val endIdx    = c.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            val allDayIdx = c.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
            val colorIdx  = c.getColumnIndexOrThrow(CalendarContract.Events.DISPLAY_COLOR)
            buildList {
                while (c.moveToNext()) {
                    add(
                        CalendarEvent(
                            id          = c.getLong(idIdx),
                            title       = c.getString(titleIdx) ?: "(No title)",
                            startMillis = c.getLong(startIdx),
                            endMillis   = c.getLong(endIdx),
                            isAllDay    = c.getInt(allDayIdx) == 1,
                            color       = c.getInt(colorIdx)
                        )
                    )
                }
            }
        }
    }

    /**
     * Synchronous query for all dates in [month] that have at least one event.
     */
    fun queryEventDaysForMonth(month: YearMonth): Set<LocalDate> {
        val zone = ZoneId.systemDefault()
        val startMillis = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = month.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val selection = "(${CalendarContract.Events.DTSTART} <= ?) AND " +
                "(${CalendarContract.Events.DTEND} >= ?) AND " +
                "(${CalendarContract.Events.DELETED} = 0)"
        val selectionArgs = arrayOf(endMillis.toString(), startMillis.toString())
        return try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events.DTSTART, CalendarContract.Events.ALL_DAY),
                selection,
                selectionArgs,
                null
            )?.use { c ->
                val startIdx = c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                buildSet {
                    while (c.moveToNext()) {
                        val millis = c.getLong(startIdx)
                        add(Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())
                    }
                }
            } ?: emptySet()
        } catch (e: SecurityException) { emptySet() }
    }

    /**
     * Updates an existing event in the device calendar provider by event id.
     * Returns true if the provider reports at least one updated row.
     *
     * Requires WRITE_CALENDAR permission.
     */
    fun updateEvent(eventId: Long, update: EventUpdate): Boolean {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, update.title)
            put(CalendarContract.Events.ALL_DAY, if (update.isAllDay) 1 else 0)
            if (update.isAllDay) {
                val utcZone = ZoneId.of("UTC")
                val day = Instant.ofEpochMilli(update.startMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val utcStart = day.atStartOfDay(utcZone).toInstant().toEpochMilli()
                val utcEnd = day.plusDays(1).atStartOfDay(utcZone).toInstant().toEpochMilli()
                put(CalendarContract.Events.DTSTART, utcStart)
                put(CalendarContract.Events.DTEND, utcEnd)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            } else {
                put(CalendarContract.Events.DTSTART, update.startMillis)
                put(CalendarContract.Events.DTEND, update.endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            }
        }

        return try {
            val updated = context.contentResolver.update(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
                values,
                null,
                null
            )
            updated > 0
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Deletes an existing event in the device calendar provider by id.
     * Returns true if at least one row was deleted.
     *
     * Requires WRITE_CALENDAR permission.
     */
    fun deleteEvent(eventId: Long): Boolean {
        return try {
            val deleted = context.contentResolver.delete(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
                null,
                null
            )
            deleted > 0
        } catch (e: SecurityException) {
            false
        }
    }
}
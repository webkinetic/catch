package com.catchapp.app.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct CalendarContract insert — no OAuth, no third-party auth, per the
 * brief. Only reachable for EVENT-kind captures on Confirm.
 */
@Singleton
class CalendarWriter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun writeEvent(title: String, description: String?, dueIso: String?): Result<Long> =
        withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext Result.failure(CalendarException.MissingPermission)

            val calendarId = findWritableCalendarId()
                ?: return@withContext Result.failure(CalendarException.NoCalendarFound)

            val zone = ZoneId.systemDefault()
            val start = resolveStart(dueIso, zone)
            val end = start.plusHours(1)

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                if (!description.isNullOrBlank()) put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.DTSTART, start.toInstant().toEpochMilli())
                put(CalendarContract.Events.DTEND, end.toInstant().toEpochMilli())
                // Explicit IANA zone, never a raw offset (hard rule #3).
                put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                ?: return@withContext Result.failure(CalendarException.InsertFailed)

            Result.success(ContentUris.parseId(uri))
        }

    /** Gemini's due_iso can land as a full offset timestamp, a bare local
     * date-time, or just a date — tries each, falls back to "an hour from
     * now" rather than failing the whole filing over an ambiguous date. */
    private fun resolveStart(dueIso: String?, zone: ZoneId): ZonedDateTime {
        if (dueIso.isNullOrBlank()) return ZonedDateTime.now(zone).plusHours(1)

        return runCatching { ZonedDateTime.parse(dueIso) }
            .recoverCatching { LocalDateTime.parse(dueIso).atZone(zone) }
            .recoverCatching { LocalDate.parse(dueIso).atTime(9, 0).atZone(zone) }
            .getOrElse { ZonedDateTime.now(zone).plusHours(1) }
    }

    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val primaryIndex = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
            val accessIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

            var fallbackId: Long? = null
            while (cursor.moveToNext()) {
                if (cursor.getInt(accessIndex) < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) continue
                val id = cursor.getLong(idIndex)
                if (primaryIndex >= 0 && cursor.getInt(primaryIndex) == 1) return id
                if (fallbackId == null) fallbackId = id
            }
            return fallbackId
        }
        return null
    }
}

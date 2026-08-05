package com.catchapp.app.data.calendar

sealed class CalendarException(message: String) : Exception(message) {
    data object MissingPermission :
        CalendarException("Calendar permission not granted.")

    data object NoCalendarFound :
        CalendarException("No writable calendar found on this device.")

    data object InsertFailed :
        CalendarException("Couldn't create the calendar event.")
}

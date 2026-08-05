package com.catchapp.app.domain

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Everything the model needs to resolve "Thursday" / "the Henderson thing" /
 * "first thing" correctly. Build this fresh on every call — it's the context
 * block the brief calls non-negotiable.
 *
 * [now] MUST be built with an explicit IANA zone (`ZonedDateTime.now(ZoneId.of("Europe/London"))`),
 * never `ZonedDateTime.now()` off the device's raw offset — that's how "Thursday"
 * silently resolves to the wrong day after a timezone change. ZonedDateTime
 * enforces this by construction: there's no way to read `now.zone` back out
 * as a bare offset.
 */
data class StructureRequest(
    val transcript: String,
    val now: ZonedDateTime,
    val existingProjects: List<String> = emptyList(),
    val recentTags: List<String> = emptyList(),
    val recentPeople: List<String> = emptyList(),
    val workingHoursStart: LocalTime = LocalTime.of(9, 0),
    val workingHoursEnd: LocalTime = LocalTime.of(18, 0)
)

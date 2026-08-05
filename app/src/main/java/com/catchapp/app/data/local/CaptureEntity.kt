package com.catchapp.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per captured thought. A row is written the instant on-device speech
 * recognition ends (state = CAPTURED) — that write must never wait on network.
 * Everything from STRUCTURING onward is driven by WorkManager, advancing the
 * `state` column and stamping the matching `*At` timestamp as it goes.
 *
 * kind/title/body/dueIso/project/tags/people/confidence mirror the Claude
 * `file_capture` tool schema — they're null/empty until STRUCTURING completes.
 */
@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // --- Raw capture, written synchronously at CAPTURED ---
    val rawTranscript: String,
    val audioFilePath: String? = null,

    // --- State machine ---
    val state: CaptureState = CaptureState.CAPTURED,
    val errorMessage: String? = null,

    // --- Claude structuring output (populated when state advances to
    //     AWAITING_CONFIRM; treated as a suggestion, not a fact, until the
    //     user confirms) ---
    val kind: CaptureKind? = null,
    val title: String? = null,
    val body: String? = null,
    val dueIso: String? = null,
    val project: String? = null,
    val tags: List<String> = emptyList(),
    val people: List<String> = emptyList(),
    val confidence: Float? = null,

    // --- Destination routing, populated at FILING/FILED ---
    val destinationId: String? = null,

    // --- State transition timestamps (epoch millis; null until reached).
    //     Every hop through the state machine is persisted, not just the
    //     current state, so captures can be audited/debugged after the fact. ---
    val capturedAt: Long,
    val structuringAt: Long? = null,
    val awaitingConfirmAt: Long? = null,
    val filingAt: Long? = null,
    val filedAt: Long? = null,
    val failedAt: Long? = null,
    val discardedAt: Long? = null
)

package com.catchapp.app.data.local

/** Mirrors the `kind` enum in the Claude `file_capture` tool schema. */
enum class CaptureKind {
    TASK,
    EVENT,
    NOTE,
    IDEA,
    CONTACT_FOLLOWUP
}

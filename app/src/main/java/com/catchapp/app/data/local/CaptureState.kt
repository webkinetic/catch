package com.catchapp.app.data.local

/**
 * The state machine every capture moves through, per the build brief:
 *
 *   CAPTURED -> STRUCTURING -> AWAITING_CONFIRM -> FILING -> FILED
 *   plus FAILED and DISCARDED (reachable from any non-terminal state).
 *
 * CAPTURED is written synchronously the instant speech recognition ends —
 * everything after that is WorkManager, never the UI thread.
 */
enum class CaptureState {
    CAPTURED,
    STRUCTURING,
    AWAITING_CONFIRM,
    FILING,
    FILED,
    FAILED,
    DISCARDED
}

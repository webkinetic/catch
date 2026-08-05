package com.catchapp.app.data

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.catchapp.app.data.calendar.CalendarWriter
import com.catchapp.app.data.local.CaptureDao
import com.catchapp.app.data.local.CaptureEntity
import com.catchapp.app.data.local.CaptureKind
import com.catchapp.app.data.local.CaptureState
import com.catchapp.app.work.StructureCaptureWorker
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only door between the capture UI and Room/WorkManager. [captureTranscript]
 * is the one method that matters for hard rule #1 (capture never blocks on
 * network): it does a local insert and returns — the WorkManager enqueue is
 * fire-and-forget, structuring happens whenever connectivity allows.
 */
@Singleton
class CaptureRepository @Inject constructor(
    private val dao: CaptureDao,
    private val workManager: WorkManager,
    private val calendarWriter: CalendarWriter
) {
    suspend fun captureTranscript(transcript: String): Long {
        val id = dao.insert(CaptureEntity(rawTranscript = transcript, capturedAt = System.currentTimeMillis()))
        enqueueStructuring(id, replaceExisting = false)
        return id
    }

    fun observeAll(): Flow<List<CaptureEntity>> = dao.observeAll()

    fun observeById(id: Long): Flow<CaptureEntity?> = dao.observeById(id)

    /**
     * AWAITING_CONFIRM -> FILED. EVENT-kind captures route to the device
     * calendar (CalendarWriter — no OAuth, direct CalendarContract insert,
     * per the brief). Everything else files into the internal inbox, the
     * brief's "always available, zero setup, ship this first" destination.
     */
    suspend fun confirmCapture(id: Long) {
        val capture = dao.getById(id) ?: return

        if (capture.kind == CaptureKind.EVENT) {
            dao.update(capture.copy(state = CaptureState.FILING, filingAt = System.currentTimeMillis()))

            val result = calendarWriter.writeEvent(
                title = capture.title ?: capture.rawTranscript,
                description = capture.body,
                dueIso = capture.dueIso
            )

            result.fold(
                onSuccess = {
                    dao.update(
                        capture.copy(
                            state = CaptureState.FILED,
                            filedAt = System.currentTimeMillis(),
                            destinationId = "google_calendar"
                        )
                    )
                },
                onFailure = { error ->
                    dao.update(
                        capture.copy(
                            state = CaptureState.FAILED,
                            failedAt = System.currentTimeMillis(),
                            errorMessage = error.message ?: "Couldn't add to Calendar."
                        )
                    )
                }
            )
            return
        }

        dao.update(
            capture.copy(
                state = CaptureState.FILED,
                filedAt = System.currentTimeMillis(),
                destinationId = "internal_inbox"
            )
        )
    }

    suspend fun discardCapture(id: Long) {
        val capture = dao.getById(id) ?: return
        dao.update(capture.copy(state = CaptureState.DISCARDED, discardedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCapture(id: Long) {
        val capture = dao.getById(id) ?: return
        dao.delete(capture)
    }

    /** Resets a FAILED row back to CAPTURED and re-enqueues — clears the old
     * error so the worker's idempotency guard doesn't just no-op the retry. */
    suspend fun retryStructuring(id: Long) {
        val capture = dao.getById(id) ?: return
        dao.update(capture.copy(state = CaptureState.CAPTURED, failedAt = null, errorMessage = null))
        enqueueStructuring(id, replaceExisting = true)
    }

    private fun enqueueStructuring(captureId: Long, replaceExisting: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<StructureCaptureWorker>()
            .setInputData(workDataOf(StructureCaptureWorker.KEY_CAPTURE_ID to captureId))
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        // KEEP for a fresh capture (avoid stacking a duplicate if this ever
        // races). REPLACE for a manual retry — a finished (failed) unique
        // work under this name would otherwise make WorkManager silently
        // ignore the new attempt, since KEEP treats "already exists" as true
        // even for terminal work.
        val policy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        workManager.enqueueUniqueWork("structure-capture-$captureId", policy, request)
    }
}

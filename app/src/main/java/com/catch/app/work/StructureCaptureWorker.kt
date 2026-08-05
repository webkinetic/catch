package com.catch.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.catch.app.data.local.CaptureDao
import com.catch.app.data.local.CaptureState
import com.catch.app.domain.CaptureStructurer
import com.catch.app.domain.StructureRequest
import com.catch.app.domain.StructuringException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Advances one CaptureEntity from CAPTURED through STRUCTURING to either
 * AWAITING_CONFIRM or FAILED. Never touches the UI thread and never runs
 * until WorkManager's network constraint is satisfied — see
 * CaptureRepository.enqueueStructuring for the constraint + backoff.
 */
@HiltWorker
class StructureCaptureWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: CaptureDao,
    private val structurer: CaptureStructurer
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val captureId = inputData.getLong(KEY_CAPTURE_ID, -1L)
        if (captureId == -1L) return Result.failure()

        val capture = dao.getById(captureId) ?: return Result.failure()

        // Idempotency guard: if a previous run got as far as AWAITING_CONFIRM
        // (or beyond) before the process died, don't re-structure on redelivery.
        if (capture.state != CaptureState.CAPTURED && capture.state != CaptureState.STRUCTURING) {
            return Result.success()
        }

        dao.update(capture.copy(state = CaptureState.STRUCTURING, structuringAt = System.currentTimeMillis()))

        val recent = dao.getRecent(excludingId = captureId)
        val request = StructureRequest(
            transcript = capture.rawTranscript,
            now = ZonedDateTime.now(ZoneId.systemDefault()),
            existingProjects = recent.mapNotNull { it.project }.distinct(),
            recentTags = recent.flatMap { it.tags }.distinct().take(20),
            recentPeople = recent.flatMap { it.people }.distinct().take(20)
        )

        val result = structurer.structure(request)

        return result.fold(
            onSuccess = { structured ->
                dao.update(
                    capture.copy(
                        state = CaptureState.AWAITING_CONFIRM,
                        awaitingConfirmAt = System.currentTimeMillis(),
                        kind = structured.kind,
                        title = structured.title,
                        body = structured.body,
                        dueIso = structured.dueIso,
                        project = structured.project,
                        tags = structured.tags,
                        people = structured.people,
                        confidence = structured.confidence
                    )
                )
                Result.success()
            },
            onFailure = { error -> handleFailure(capture.id, error) }
        )
    }

    private suspend fun handleFailure(captureId: Long, error: Throwable): Result {
        // MissingApiKey won't fix itself by retrying — fail fast so it shows
        // up in the inbox instead of silently retrying forever.
        if (error is StructuringException.MissingApiKey) {
            markFailed(captureId, "No Gemini API key set.")
            return Result.failure()
        }

        if (runAttemptCount >= MAX_ATTEMPTS) {
            markFailed(captureId, "Structuring failed after $MAX_ATTEMPTS attempts.")
            return Result.failure()
        }

        return Result.retry()
    }

    private suspend fun markFailed(captureId: Long, message: String) {
        val current = dao.getById(captureId) ?: return
        dao.update(
            current.copy(
                state = CaptureState.FAILED,
                failedAt = System.currentTimeMillis(),
                errorMessage = message
            )
        )
    }

    companion object {
        const val KEY_CAPTURE_ID = "capture_id"
        private const val MAX_ATTEMPTS = 8
    }
}

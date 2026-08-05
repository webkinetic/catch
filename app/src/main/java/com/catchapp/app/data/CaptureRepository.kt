package com.catchapp.app.data

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.catchapp.app.data.local.CaptureDao
import com.catchapp.app.data.local.CaptureEntity
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
    private val workManager: WorkManager
) {
    suspend fun captureTranscript(transcript: String): Long {
        val id = dao.insert(CaptureEntity(rawTranscript = transcript, capturedAt = System.currentTimeMillis()))
        enqueueStructuring(id)
        return id
    }

    fun observeAll(): Flow<List<CaptureEntity>> = dao.observeAll()

    private fun enqueueStructuring(captureId: Long) {
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

        // Unique per capture + KEEP: a retry after process death re-enqueues
        // the same logical job rather than stacking a duplicate.
        workManager.enqueueUniqueWork(
            "structure-capture-$captureId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}

package com.example.healthsync.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healthsync.data.local.SecureConfigStore
import com.example.healthsync.data.repository.HealthRepository
import com.example.healthsync.data.repository.SyncRepository
import com.example.healthsync.domain.model.SyncPayload
import com.example.healthsync.domain.model.SyncWindow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Periodic background sync. Reads Health Connect since the last successful sync window,
 * enqueues a batch, and drains the queue to the configured REST endpoint.
 *
 * Retries on failure with WorkManager exponential backoff.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val health: HealthRepository,
    private val sync: SyncRepository,
    private val config: SecureConfigStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        if (!config.isConfigured()) return Result.success() // nothing to do yet
        if (!health.hasAllPermissions()) return Result.retry()

        val now = Instant.now()
        val from = now.minus(config.syncIntervalMinutes.coerceAtLeast(15), ChronoUnit.MINUTES)

        val hr = health.readHeartRate(from, now)
        val steps = health.readDailySteps(from, now)

        if (hr.isNotEmpty() || steps.isNotEmpty()) {
            val payload = SyncPayload(
                deviceId = config.deviceId,
                syncedAt = DateTimeFormatter.ISO_INSTANT.format(now),
                window = SyncWindow(
                    from = DateTimeFormatter.ISO_INSTANT.format(from),
                    to = DateTimeFormatter.ISO_INSTANT.format(now),
                ),
                steps = steps,
                heartRate = hr,
            )
            sync.enqueue(payload)
        }

        val allOk = sync.drainQueue()
        if (allOk) Result.success() else Result.retry()
    }.getOrElse { Result.retry() }

    companion object {
        const val UNIQUE_NAME = "healthsync_periodic"
        const val ONE_SHOT = "healthsync_oneshot"
    }
}

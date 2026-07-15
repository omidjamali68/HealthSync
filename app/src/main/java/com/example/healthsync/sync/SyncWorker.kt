package com.example.healthsync.sync

import android.content.Context
import android.util.Log
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
        Log.d("SyncWorker", "Starting sync work...")
        if (!config.isConfigured()) {
            Log.w("SyncWorker", "Sync skipped: App not configured. " +
                    "BaseURL: ${config.baseUrl.isNotBlank()}, " +
                    "AuthToken: ${config.authToken.isNotBlank()}, " +
                    "DeviceId: ${config.deviceId.isNotBlank()}")
            return Result.success()
        }
        if (!health.hasAllPermissions()) {
            Log.w("SyncWorker", "Sync skipped: Missing Health Connect permissions")
            return Result.retry()
        }

        val isRetry = runAttemptCount > 0
        if (!isRetry) {
            val now = Instant.now()
            val from = now.minus(config.syncIntervalMinutes.coerceAtLeast(15), ChronoUnit.MINUTES)
            Log.d("SyncWorker", "Reading data from $from to $now")

            val hr = health.readHeartRate(from, now)
            val steps = health.readDailySteps(from, now)
            val bp = health.readBloodPressure(from, now)
            val bo = health.readBloodOxygen(from, now)
            val sleep = health.readSleepSessions(from, now)

            Log.d("SyncWorker", "Data found: HR=${hr.size}, Steps=${steps.size}, BP=${bp.size}, BO=${bo.size}, Sleep=${sleep.size}")

            if (hr.isNotEmpty() || steps.isNotEmpty() || bp.isNotEmpty() || bo.isNotEmpty() || sleep.isNotEmpty()) {
                val payload = SyncPayload(
                    deviceId = config.deviceId,
                    syncedAt = DateTimeFormatter.ISO_INSTANT.format(now),
                    window = SyncWindow(
                        from = DateTimeFormatter.ISO_INSTANT.format(from),
                        to = DateTimeFormatter.ISO_INSTANT.format(now),
                    ),
                    steps = steps,
                    heartRate = hr,
                    bloodPressure = bp,
                    bloodOxygen = bo,
                    sleep = sleep,
                )
                sync.enqueue(payload)
                Log.d("SyncWorker", "Payload enqueued")
            } else {
                Log.d("SyncWorker", "No new data to enqueue")
            }
        } else {
            Log.d("SyncWorker", "Retry attempt $runAttemptCount: skipping data collection to avoid duplicates")
        }

        Log.d("SyncWorker", "Draining queue...")
        val allOk = sync.drainQueue()
        Log.d("SyncWorker", "Sync work finished. All OK: $allOk")
        if (allOk) Result.success() else Result.retry()
    }.getOrElse { e ->
        Log.e("SyncWorker", "Sync work failed with exception", e)
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "healthsync_periodic"
        const val ONE_SHOT = "healthsync_oneshot"
    }
}

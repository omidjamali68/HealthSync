package com.example.healthsync.data.repository

import com.example.healthsync.data.local.QueuedBatchEntity
import com.example.healthsync.data.local.SecureConfigStore
import com.example.healthsync.data.local.SyncLogDao
import com.example.healthsync.data.local.SyncLogEntity
import com.example.healthsync.data.local.SyncQueueDao
import com.example.healthsync.data.remote.HealthApi
import com.example.healthsync.domain.model.SyncPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val api: HealthApi,
    private val queueDao: SyncQueueDao,
    private val logDao: SyncLogDao,
    private val config: SecureConfigStore,
    private val json: Json,
) {
    val queueSize: Flow<Int> = queueDao.queueSize()
    val recentLogs: Flow<List<SyncLogEntity>> = logDao.recent()
    val lastSuccess: Flow<SyncLogEntity?> = logDao.lastSuccess()

    /** Persist a payload so it survives crashes/network outages. */
    suspend fun enqueue(payload: SyncPayload) {
        queueDao.enqueue(
            QueuedBatchEntity(
                payloadJson = json.encodeToString(SyncPayload.serializer(), payload),
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    /**
     * Drain queued batches. Returns true if everything was sent.
     * Failed batches are kept and their attempt count is incremented.
     */
    suspend fun drainQueue(): Boolean {
        var allOk = true
        while (true) {
            val batch = queueDao.next(1).firstOrNull() ?: break
            val payload = runCatching { json.decodeFromString(SyncPayload.serializer(), batch.payloadJson) }
                .getOrNull()
            if (payload == null) {
                queueDao.delete(batch.id)
                continue
            }
            val ok = postPayload(payload)
            if (ok) {
                queueDao.delete(batch.id)
                logDao.insert(
                    SyncLogEntity(
                        timestamp = System.currentTimeMillis(),
                        success = true,
                        message = "Sent ${payload.heartRate.size} HR / ${payload.steps.size} step rows",
                        itemsSent = payload.heartRate.size + payload.steps.size,
                    )
                )
            } else {
                queueDao.markFailed(batch.id, "HTTP error or network failure")
                logDao.insert(
                    SyncLogEntity(
                        timestamp = System.currentTimeMillis(),
                        success = false,
                        message = "Failed batch ${batch.id} (attempt ${batch.attempts + 1})",
                        itemsSent = 0,
                    )
                )
                allOk = false
                break // WorkManager will retry with backoff
            }
        }
        return allOk
    }

    private suspend fun postPayload(payload: SyncPayload): Boolean {
        val base = config.baseUrl.trimEnd('/')
        val path = config.ingestPath.trimStart('/')
        val url = "$base/$path"
        val token = config.authToken
        if (token.isBlank()) return false
        return runCatching {
            val resp = api.ingest(url, "Bearer $token", payload)
            resp.isSuccessful
        }.getOrDefault(false)
    }
}

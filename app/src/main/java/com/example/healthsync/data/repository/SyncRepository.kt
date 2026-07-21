package com.example.healthsync.data.repository

import android.util.Log
import com.example.healthsync.data.local.QueuedBatchEntity
import com.example.healthsync.data.local.SecureConfigStore
import com.example.healthsync.data.local.SyncLogDao
import com.example.healthsync.data.local.SyncLogEntity
import com.example.healthsync.data.local.SyncQueueDao
import com.example.healthsync.data.remote.HealthApi
import com.example.healthsync.domain.model.SyncPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val drainMutex = Mutex()

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

    suspend fun clearQueue() {
        queueDao.clearAll()
    }

    /**
     * Drain queued batches. Returns true if everything was sent.
     * Failed batches are kept and their attempt count is incremented.
     */
    suspend fun drainQueue(): Boolean = drainMutex.withLock {
        Log.d("SyncRepository", "drainQueue started")
        var allOk = true
        while (true) {
            val batch = queueDao.next(1).firstOrNull() ?: break
            Log.d("SyncRepository", "Processing batch ${batch.id}")
            val payload = runCatching { json.decodeFromString(SyncPayload.serializer(), batch.payloadJson) }
                .getOrNull()
            if (payload == null) {
                Log.e("SyncRepository", "Failed to decode payload for batch ${batch.id}")
                queueDao.delete(batch.id)
                continue
            }
            
            val result = postPayloadWithResult(payload)
            if (result.isSuccess) {
                Log.d("SyncRepository", "Batch ${batch.id} sent successfully")
                queueDao.delete(batch.id)
                
                val hrCount = payload.heartRate.size
                val stepsCount = payload.steps.size
                val bpCount = payload.bloodPressure.size
                val oxCount = payload.bloodOxygen.size
                val sleepCount = payload.sleep.size
                val totalItems = hrCount + stepsCount + bpCount + oxCount + sleepCount
                
                val detailMsg = buildString {
                    append("Sent: ")
                    if (hrCount > 0) append("HR($hrCount) ")
                    if (stepsCount > 0) append("Steps($stepsCount) ")
                    if (bpCount > 0) append("BP($bpCount) ")
                    if (oxCount > 0) append("Ox($oxCount) ")
                    if (sleepCount > 0) append("Sleep($sleepCount) ")
                    if (totalItems == 0) append("No new records")
                }.trim()

                logDao.insert(
                    SyncLogEntity(
                        timestamp = System.currentTimeMillis(),
                        success = true,
                        message = detailMsg,
                        itemsSent = totalItems,
                    )
                )
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "HTTP ${result.statusCode}"
                val isPermanent = result.isPermanentFailure()
                
                if (isPermanent) {
                    Log.e("SyncRepository", "Permanent failure for batch ${batch.id}: $errorMsg")
                    // It's a 4xx error (not 429), no point in retrying this batch.
                    queueDao.delete(batch.id)
                    logDao.insert(
                        SyncLogEntity(
                            timestamp = System.currentTimeMillis(),
                            success = false,
                            message = "Dropped bad batch ${batch.id}: $errorMsg",
                            itemsSent = 0,
                        )
                    )
                } else {
                    Log.e("SyncRepository", "Failed to send batch ${batch.id}: $errorMsg")
                    queueDao.markFailed(batch.id, errorMsg)
                    logDao.insert(
                        SyncLogEntity(
                            timestamp = System.currentTimeMillis(),
                            success = false,
                            message = "Failed batch ${batch.id} (attempt ${batch.attempts + 1}): $errorMsg",
                            itemsSent = 0,
                        )
                    )
                    allOk = false
                    break // WorkManager will retry with backoff
                }
            }
        }
        Log.d("SyncRepository", "drainQueue finished, allOk=$allOk")
        return@withLock allOk
    }

    private data class SyncResult(
        val isSuccess: Boolean,
        val statusCode: Int = 0,
        val exception: Throwable? = null
    ) {
        fun isPermanentFailure(): Boolean {
            if (exception != null) return false // Network errors are not permanent
            return statusCode in 400..499 && statusCode != 429
        }
        fun exceptionOrNull() = exception
    }

    private suspend fun postPayloadWithResult(payload: SyncPayload): SyncResult {
        val base = config.baseUrl.trimEnd('/')
        val path = config.ingestPath.trimStart('/')
        val url = "$base/$path"
        val token = config.authToken
        if (token.isBlank()) {
            Log.e("SyncRepository", "postPayload failed: Auth token is blank")
            return SyncResult(false, exception = IllegalStateException("No auth token"))
        }
        
        Log.d("SyncRepository", "Posting payload to $url")
        return try {
            val resp = api.ingest(url, "Bearer $token", payload)
            Log.d("SyncRepository", "Response code: ${resp.code()}")
            if (resp.isSuccessful) {
                SyncResult(true, resp.code())
            } else {
                SyncResult(false, resp.code())
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "postPayload exception: ${e.message}", e)
            SyncResult(false, exception = e)
        }
    }

    private suspend fun postPayload(payload: SyncPayload): Boolean {
        return postPayloadWithResult(payload).isSuccess
    }
}

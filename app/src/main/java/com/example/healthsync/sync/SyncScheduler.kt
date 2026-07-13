package com.example.healthsync.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.healthsync.data.local.SecureConfigStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: SecureConfigStore,
) {
    private val wm get() = WorkManager.getInstance(context)

    fun ensureScheduled() {
        val intervalMin = config.syncIntervalMinutes.coerceAtLeast(15)
        val req = PeriodicWorkRequestBuilder<SyncWorker>(intervalMin, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(
            SyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            req,
        )
    }

    fun runNow() {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        wm.enqueueUniqueWork(SyncWorker.ONE_SHOT, ExistingWorkPolicy.REPLACE, req)
    }

    fun cancel() {
        wm.cancelUniqueWork(SyncWorker.UNIQUE_NAME)
        wm.cancelUniqueWork(SyncWorker.ONE_SHOT)
    }
}

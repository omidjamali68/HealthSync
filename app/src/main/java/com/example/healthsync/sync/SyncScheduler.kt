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
import java.time.Duration
import java.time.LocalDateTime
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
        // ۱. همگام‌سازی دوره‌ای (مثلاً هر ۶۰ دقیقه)
        val intervalMin = config.syncIntervalMinutes.coerceAtLeast(15)
        val periodicReq = PeriodicWorkRequestBuilder<SyncWorker>(intervalMin, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(
            SyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicReq,
        )

        // ۲. همگام‌سازی دقیق پایان روز (ساعت ۲۳:۵۵)
        scheduleDailySync()
    }

    fun scheduleDailySync() {
        val now = LocalDateTime.now()
        var target = now.withHour(23).withMinute(55).withSecond(0).withNano(0)
        
        // اگر الان از ۲۳:۵۵ گذشته، برای فردا برنامه‌ریزی کن
        if (now.isAfter(target)) {
            target = target.plusDays(1)
        }

        val delay = Duration.between(now, target).toMinutes()

        val dailyReq = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("DAILY_SYNC")
            .build()

        wm.enqueueUniqueWork(
            SyncWorker.DAILY_SYNC_NAME,
            ExistingWorkPolicy.REPLACE,
            dailyReq
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
        wm.cancelUniqueWork(SyncWorker.DAILY_SYNC_NAME)
    }
}

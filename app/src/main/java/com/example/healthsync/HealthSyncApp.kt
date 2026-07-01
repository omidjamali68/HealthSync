package com.example.healthsync

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.healthsync.sync.SyncScheduler
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HealthSyncApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        syncScheduler.ensureScheduled()

        // Set default locale to Persian if not already set by user
        // We use a simple check to see if we've ever set it before
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        if (!prefs.contains("locale_set")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fa"))
            prefs.edit().putBoolean("locale_set", true).apply()
        }
    }
}

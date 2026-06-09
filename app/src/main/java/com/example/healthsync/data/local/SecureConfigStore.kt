package com.example.healthsync.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.healthsync.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted storage for API configuration. Backed by Jetpack Security
 * EncryptedSharedPreferences (AES256_GCM values, AES256_SIV keys).
 */
@Singleton
class SecureConfigStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "healthsync_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, BuildConfig.DEFAULT_BASE_URL) ?: BuildConfig.DEFAULT_BASE_URL
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    var ingestPath: String
        get() = prefs.getString(KEY_INGEST_PATH, BuildConfig.DEFAULT_INGEST_PATH) ?: BuildConfig.DEFAULT_INGEST_PATH
        set(value) = prefs.edit().putString(KEY_INGEST_PATH, value).apply()

    var authToken: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var syncIntervalMinutes: Long
        get() = prefs.getLong(KEY_INTERVAL, 15L)
        set(value) = prefs.edit().putLong(KEY_INTERVAL, value.coerceAtLeast(15L)).apply()

    var deviceId: String
        get() {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (!existing.isNullOrBlank()) return existing
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            return generated
        }
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    fun isConfigured(): Boolean =
        baseUrl.isNotBlank() && authToken.isNotBlank() && deviceId.isNotBlank()

    private companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_INGEST_PATH = "ingest_path"
        const val KEY_TOKEN = "auth_token"
        const val KEY_INTERVAL = "sync_interval"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_ONBOARDED = "onboarded"
    }
}

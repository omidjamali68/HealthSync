package com.example.healthsync.ui.settings

import androidx.lifecycle.ViewModel
import com.example.healthsync.data.local.SecureConfigStore
import com.example.healthsync.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

data class SettingsUiState(
    val baseUrl: String = "",
    val ingestPath: String = "",
    val token: String = "",
    val deviceId: String = "",
    val intervalMinutes: String = "15",
    val isSaved: Boolean = false,
    val language: String = "fa",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val config: SecureConfigStore,
    private val scheduler: SyncScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            baseUrl = config.baseUrl,
            ingestPath = config.ingestPath,
            token = config.authToken,
            deviceId = config.deviceId,
            intervalMinutes = config.syncIntervalMinutes.toString(),
            language = AppCompatDelegate.getApplicationLocales().toLanguageTags().let { if (it.isEmpty()) "fa" else it },
        )
    )
    val state = _state.asStateFlow()

    fun setBaseUrl(v: String) { _state.value = _state.value.copy(baseUrl = v, isSaved = false) }
    fun setIngestPath(v: String) { _state.value = _state.value.copy(ingestPath = v, isSaved = false) }
    fun setToken(v: String) { _state.value = _state.value.copy(token = v, isSaved = false) }
    fun setDeviceId(v: String) { _state.value = _state.value.copy(deviceId = v, isSaved = false) }
    fun setInterval(v: String) { _state.value = _state.value.copy(intervalMinutes = v.filter(Char::isDigit), isSaved = false) }
    fun setLanguage(lang: String) {
        _state.value = _state.value.copy(language = lang)
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(lang)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun save() {
        val s = _state.value
        config.baseUrl = s.baseUrl.trim()
        config.ingestPath = s.ingestPath.trim()
        config.authToken = s.token.trim()
        config.deviceId = s.deviceId.trim().ifBlank { config.deviceId }
        config.syncIntervalMinutes = s.intervalMinutes.toLongOrNull()?.coerceAtLeast(15) ?: 15
        config.onboardingComplete = true
        scheduler.ensureScheduled()
        _state.value = s.copy(isSaved = true)
    }
}

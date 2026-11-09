package com.example.healthsync.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthsync.data.local.SecureConfigStore
import com.example.healthsync.data.local.SyncLogEntity
import com.example.healthsync.data.repository.SyncRepository
import com.example.healthsync.domain.model.*
import com.example.healthsync.sync.SyncScheduler
import com.example.healthsync.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class DashboardUiState(
    val deviceId: String = "",
    val intervalMinutes: Long = 15,
    val queueSize: Int = 0,
    val lastSuccessAt: Long? = null,
    val logs: List<SyncLogEntity> = emptyList(),
    val selectedLog: SyncLogEntity? = null,
    val isAdmin: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sync: SyncRepository,
    private val scheduler: SyncScheduler,
    private val config: SecureConfigStore,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DashboardUiState(
            deviceId = config.deviceId,
            intervalMinutes = config.syncIntervalMinutes,
            isAdmin = sessionManager.isAdmin(),
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sync.queueSize,
                sync.lastSuccess,
                sync.recentLogs,
                sessionManager.isAdminFlow
            ) { qs, last, logs, isAdmin ->
                _state.value.copy(
                    queueSize = qs,
                    lastSuccessAt = last?.timestamp,
                    logs = logs,
                    deviceId = config.deviceId,
                    intervalMinutes = config.syncIntervalMinutes,
                    isAdmin = isAdmin,
                )
            }.collect { _state.value = it }
        }
    }

    fun syncNow() = scheduler.runNow()

    fun selectLog(log: SyncLogEntity?) {
        _state.value = _state.value.copy(selectedLog = log)
    }

    fun clearQueue() {
        viewModelScope.launch {
            sync.clearQueue()
        }
    }

    fun sendTestData() {
        if (!sessionManager.isAdmin()) return
        // ... (existing code)
    }

    fun logout() {
        sessionManager.clear()
        config.authToken = ""
        config.onboardingComplete = false
        scheduler.cancel()
    }
}

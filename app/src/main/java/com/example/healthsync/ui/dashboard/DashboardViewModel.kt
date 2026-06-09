package com.example.healthsync.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthsync.data.local.SecureConfigStore
import com.example.healthsync.data.local.SyncLogEntity
import com.example.healthsync.data.repository.SyncRepository
import com.example.healthsync.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val deviceId: String = "",
    val intervalMinutes: Long = 15,
    val queueSize: Int = 0,
    val lastSuccessAt: Long? = null,
    val logs: List<SyncLogEntity> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sync: SyncRepository,
    private val scheduler: SyncScheduler,
    private val config: SecureConfigStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DashboardUiState(deviceId = config.deviceId, intervalMinutes = config.syncIntervalMinutes)
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(sync.queueSize, sync.lastSuccess, sync.recentLogs) { qs, last, logs ->
                _state.value.copy(
                    queueSize = qs,
                    lastSuccessAt = last?.timestamp,
                    logs = logs,
                    deviceId = config.deviceId,
                    intervalMinutes = config.syncIntervalMinutes,
                )
            }.collect { _state.value = it }
        }
    }

    fun syncNow() = scheduler.runNow()
}

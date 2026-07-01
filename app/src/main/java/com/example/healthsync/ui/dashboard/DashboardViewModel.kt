package com.example.healthsync.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthsync.data.local.SecureConfigStore
import com.example.healthsync.data.local.SyncLogEntity
import com.example.healthsync.data.repository.SyncRepository
import com.example.healthsync.domain.model.*
import com.example.healthsync.sync.SyncScheduler
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

    fun sendTestData() {
        viewModelScope.launch {
            val now = Instant.now()
            val nowStr = DateTimeFormatter.ISO_INSTANT.format(now)
            val today = now.toString().split("T")[0]

            val testPayload = SyncPayload(
                deviceId = config.deviceId.ifBlank { "TEST-DEVICE-${UUID.randomUUID().toString().take(8)}" },
                syncedAt = nowStr,
                window = SyncWindow(from = nowStr, to = nowStr),
                steps = listOf(
                    StepRecord(date = today, count = 7500, distanceKm = 5.4, caloriesBurned = 320.5)
                ),
                heartRate = listOf(
                    HeartRateSample(timestamp = nowStr, bpm = 72)
                ),
                bloodPressure = listOf(
                    BloodPressureSample(timestamp = nowStr, systolic = 120.0, diastolic = 80.0)
                ),
                bloodOxygen = listOf(
                    BloodOxygenSample(timestamp = nowStr, percentage = 98.5)
                ),
                sleep = listOf(
                    SleepSession(
                        startTime = nowStr,
                        endTime = nowStr,
                        totalDurationMinutes = 480,
                        lightSleepMinutes = 300,
                        deepSleepMinutes = 100,
                        remSleepMinutes = 80,
                        awakeMinutes = 0,
                        stages = listOf(
                            SleepStage(startTime = nowStr, endTime = nowStr, stage = 4, durationMinutes = 480)
                        )
                    )
                )
            )

            sync.enqueue(testPayload)
            sync.drainQueue()
        }
    }
}

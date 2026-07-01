package com.example.healthsync.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HeartRateSample(
    val timestamp: String, // ISO-8601 UTC
    val bpm: Long,
)

@Serializable
data class StepRecord(
    val date: String, // yyyy-MM-dd
    val count: Long,
    val distanceKm: Double,
    val caloriesBurned: Double,
)

@Serializable
data class BloodPressureSample(
    val timestamp: String,
    val systolic: Double,
    val diastolic: Double,
)

@Serializable
data class BloodOxygenSample(
    val timestamp: String,
    val percentage: Double,
)

@Serializable
data class SleepStage(
    val startTime: String,
    val endTime: String,
    val stage: Int, // 1: Awake, 2: Sleeping, 3: Out-of-bed, 4: Light, 5: Deep, 6: REM
    val durationMinutes: Long,
)

@Serializable
data class SleepSession(
    val startTime: String,
    val endTime: String,
    val totalDurationMinutes: Long,
    val lightSleepMinutes: Long,
    val deepSleepMinutes: Long,
    val remSleepMinutes: Long,
    val awakeMinutes: Long,
    val stages: List<SleepStage>,
)

@Serializable
data class SyncWindow(val from: String, val to: String)

@Serializable
data class SyncPayload(
    val deviceId: String,
    val syncedAt: String,
    val window: SyncWindow,
    val steps: List<StepRecord>,
    val heartRate: List<HeartRateSample>,
    val bloodPressure: List<BloodPressureSample>,
    val bloodOxygen: List<BloodOxygenSample>,
    val sleep: List<SleepSession>,
)

@Serializable
data class IngestResponse(val accepted: Boolean = true, val message: String? = null)

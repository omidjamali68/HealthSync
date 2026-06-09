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
)

@Serializable
data class IngestResponse(val accepted: Boolean = true, val message: String? = null)

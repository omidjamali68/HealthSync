package com.example.healthsync.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.healthsync.domain.model.HeartRateSample
import com.example.healthsync.domain.model.StepRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
    )

    enum class Availability { Available, ProviderUpdateRequired, NotSupported }

    fun availability(): Availability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.Available
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.ProviderUpdateRequired
        else -> Availability.NotSupported
    }

    private fun client(): HealthConnectClient? =
        if (availability() == Availability.Available) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasAllPermissions(): Boolean {
        val c = client() ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun readHeartRate(from: Instant, to: Instant): List<HeartRateSample> {
        val c = client() ?: return emptyList()
        val resp = c.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(from, to),
            )
        )
        return resp.records.flatMap { rec ->
            rec.samples.map {
                HeartRateSample(
                    timestamp = DateTimeFormatter.ISO_INSTANT.format(it.time),
                    bpm = it.beatsPerMinute,
                )
            }
        }
    }

    suspend fun readDailySteps(from: Instant, to: Instant): List<StepRecord> {
        val c = client() ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val startDate = LocalDate.ofInstant(from, zone)
        val endDate = LocalDate.ofInstant(to, zone)
        val out = mutableListOf<StepRecord>()
        var d = startDate
        while (!d.isAfter(endDate)) {
            val dayStart = d.atStartOfDay(zone).toInstant()
            val dayEnd = d.plusDays(1).atStartOfDay(zone).toInstant()
            val agg = c.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd),
                )
            )
            val total = agg[StepsRecord.COUNT_TOTAL] ?: 0L
            if (total > 0L) out += StepRecord(date = d.toString(), count = total)
            d = d.plusDays(1)
        }
        return out
    }
}

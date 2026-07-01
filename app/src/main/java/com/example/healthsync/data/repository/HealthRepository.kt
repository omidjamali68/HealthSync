package com.example.healthsync.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.healthsync.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
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
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
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
        val startDate = from.atZone(zone).toLocalDate()
        val endDate = to.atZone(zone).toLocalDate()
        val out = mutableListOf<StepRecord>()
        var d = startDate
        while (!d.isAfter(endDate)) {
            val dayStart = d.atStartOfDay(zone).toInstant()
            val dayEnd = d.plusDays(1).atStartOfDay(zone).toInstant()
            val agg = c.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd),
                )
            )
            val steps = agg[StepsRecord.COUNT_TOTAL] ?: 0L
            val distance = agg[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0
            val calories = agg[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0

            if (steps > 0L || distance > 0.0 || calories > 0.0) {
                out += StepRecord(
                    date = d.toString(),
                    count = steps,
                    distanceKm = distance,
                    caloriesBurned = calories
                )
            }
            d = d.plusDays(1)
        }
        return out
    }

    suspend fun readBloodPressure(from: Instant, to: Instant): List<BloodPressureSample> {
        val c = client() ?: return emptyList()
        val resp = c.readRecords(
            ReadRecordsRequest(
                recordType = BloodPressureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(from, to),
            )
        )
        return resp.records.map {
            BloodPressureSample(
                timestamp = DateTimeFormatter.ISO_INSTANT.format(it.time),
                systolic = it.systolic.inMillimetersOfMercury,
                diastolic = it.diastolic.inMillimetersOfMercury,
            )
        }
    }

    suspend fun readBloodOxygen(from: Instant, to: Instant): List<BloodOxygenSample> {
        val c = client() ?: return emptyList()
        val resp = c.readRecords(
            ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(from, to),
            )
        )
        return resp.records.map {
            BloodOxygenSample(
                timestamp = DateTimeFormatter.ISO_INSTANT.format(it.time),
                percentage = it.percentage.value,
            )
        }
    }

    suspend fun readSleepSessions(from: Instant, to: Instant): List<SleepSession> {
        val c = client() ?: return emptyList()
        val resp = c.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(from, to),
            )
        )
        return resp.records.map { session ->
            val totalDuration = Duration.between(session.startTime, session.endTime).toMinutes()
            
            var light = 0L
            var deep = 0L
            var rem = 0L
            var awake = 0L
            
            val stageList = session.stages.map { stage ->
                val duration = Duration.between(stage.startTime, stage.endTime).toMinutes()
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> light += duration
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deep += duration
                    SleepSessionRecord.STAGE_TYPE_REM -> rem += duration
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> awake += duration
                }
                SleepStage(
                    startTime = DateTimeFormatter.ISO_INSTANT.format(stage.startTime),
                    endTime = DateTimeFormatter.ISO_INSTANT.format(stage.endTime),
                    stage = stage.stage,
                    durationMinutes = duration
                )
            }

            SleepSession(
                startTime = DateTimeFormatter.ISO_INSTANT.format(session.startTime),
                endTime = DateTimeFormatter.ISO_INSTANT.format(session.endTime),
                totalDurationMinutes = totalDuration,
                lightSleepMinutes = light,
                deepSleepMinutes = deep,
                remSleepMinutes = rem,
                awakeMinutes = awake,
                stages = stageList
            )
        }
    }
}

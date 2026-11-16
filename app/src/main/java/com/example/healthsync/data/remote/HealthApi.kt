package com.example.healthsync.data.remote

import com.example.healthsync.domain.model.*
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.*

interface HealthApi {
    @POST
    suspend fun ingest(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Body payload: SyncPayload,
    ): Response<IngestResponse>

    @GET
    suspend fun getMetricTypes(
        @Url url: String
    ): Response<ResponseDto<List<HealthMetricViewModel>>>

    @GET
    suspend fun getMetricComponents(
        @Url url: String
    ): Response<ResponseDto<List<HealthMetricComponentViewModel>>>

    @POST
    suspend fun createHealthRecord(
        @Url url: String,
        @Body body: CreateHealthRecordDto
    ): Response<ResponseDto<JsonElement>>

    @PUT
    suspend fun updateHealthRecord(
        @Url url: String,
        @Body body: UpdateHealthRecordDto
    ): Response<ResponseDto<JsonElement>>

    @GET
    suspend fun getHealthRecordsByUser(
        @Url url: String
    ): Response<ResponseDto<PagedResultDto<HealthRecordIndexViewModel>>>

    @DELETE
    suspend fun deleteHealthRecord(
        @Url url: String
    ): Response<ResponseDto<JsonElement>>

    @GET
    suspend fun getLatestInsight(
        @Url url: String
    ): Response<JsonElement>
}

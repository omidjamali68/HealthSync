package com.example.healthsync.data.remote

import com.example.healthsync.domain.model.IngestResponse
import com.example.healthsync.domain.model.SyncPayload
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface HealthApi {
    @POST
    suspend fun ingest(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Body payload: SyncPayload,
    ): Response<IngestResponse>
}

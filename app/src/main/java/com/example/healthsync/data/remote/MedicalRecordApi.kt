package com.example.healthsync.data.remote

import com.example.healthsync.domain.model.*
import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface MedicalRecordApi {

    @GET
    suspend fun getMedicalRecordsByUser(
        @Url url: String
    ): Response<ResponseDto<PagedResultDto<MedicalRecordDto>>>

    @GET
    suspend fun getMedicalRecordById(
        @Url url: String
    ): Response<ResponseDto<MedicalRecordDto>>

    @Multipart
    @POST
    suspend fun createMedicalRecord(
        @Url url: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): Response<ResponseDto<CreateResponseDto>>

    @Multipart
    @PUT
    suspend fun updateMedicalRecord(
        @Url url: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): Response<ResponseDto<CreateResponseDto>>

    @DELETE
    suspend fun deleteMedicalRecord(
        @Url url: String
    ): Response<ResponseDto<JsonElement>>

    @Multipart
    @POST
    suspend fun interpretUpload(
        @Url url: String,
        @Part files: List<MultipartBody.Part>
    ): Response<JsonElement>

    @GET
    suspend fun getFileInterpretation(
        @Url url: String
    ): Response<JsonElement>

    @POST
    suspend fun interpretFile(
        @Url url: String
    ): Response<JsonElement>
}

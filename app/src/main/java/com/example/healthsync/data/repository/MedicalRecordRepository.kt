package com.example.healthsync.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.healthsync.data.local.SecureConfigStore
import com.example.healthsync.data.remote.MedicalRecordApi
import com.example.healthsync.domain.model.*
import com.example.healthsync.util.ApiConfig
import com.example.healthsync.util.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicalRecordRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MedicalRecordApi,
    private val config: SecureConfigStore
) {
    private val baseUrl: String
        get() = config.baseUrl.trimEnd('/')

    suspend fun getMedicalRecordsByUser(
        page: Int = 1,
        pageSize: Int = 10,
        search: String = "",
        sortBy: String = "",
        sortDirection: String = "",
        filterDate: String = ""
    ): Result<PagedResultDto<MedicalRecordDto>> {
        return try {
            val convertedFilterDate = if (filterDate.isNotBlank()) {
                DateUtils.convertJalaliStringToGregorian(filterDate)
            } else ""
            val encSearch = URLEncoder.encode(search, "UTF-8")
            val encSortBy = URLEncoder.encode(sortBy, "UTF-8")
            val encSortDir = URLEncoder.encode(sortDirection, "UTF-8")
            val encFilterDate = URLEncoder.encode(convertedFilterDate, "UTF-8")

            val url = "$baseUrl/${ApiConfig.MEDICAL_RECORDS_BY_USER}?page=$page&pageSize=$pageSize&search=$encSearch&sortBy=$encSortBy&sortDirection=$encSortDir&filterDate=$encFilterDate"
            Log.d("MedicalRecordRepo", "Fetching medical records URL: $url")
            val resp = api.getMedicalRecordsByUser(url)
            if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                val paged = resp.body()?.data ?: PagedResultDto()
                Result.success(paged)
            } else {
                val msg = resp.body()?.message ?: "HTTP ${resp.code()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("MedicalRecordRepo", "getMedicalRecordsByUser error", e)
            Result.failure(e)
        }
    }

    suspend fun getMedicalRecordById(id: String): Result<MedicalRecordDto> {
        return try {
            val url = "$baseUrl/${ApiConfig.MEDICAL_RECORDS}/$id"
            val resp = api.getMedicalRecordById(url)
            if (resp.isSuccessful && resp.body()?.isSuccess == true && resp.body()?.data != null) {
                Result.success(resp.body()!!.data!!)
            } else {
                Result.failure(Exception(resp.body()?.message ?: "خطا در دریافت اطلاعات پرونده"))
            }
        } catch (e: Exception) {
            Log.e("MedicalRecordRepo", "getMedicalRecordById error", e)
            Result.failure(e)
        }
    }

    suspend fun createMedicalRecord(
        title: String,
        category: Int,
        recordDate: String,
        description: String? = null,
        doctorName: String? = null,
        centerName: String? = null,
        fileUris: List<Uri> = emptyList()
    ): Result<CreateResponseDto> {
        return try {
            val url = "$baseUrl/${ApiConfig.MEDICAL_RECORDS}"
            val fields = mutableMapOf<String, RequestBody>()

            val convertedRecordDate = if (recordDate.isNotBlank()) {
                DateUtils.convertJalaliStringToGregorian(recordDate)
            } else recordDate

            fields["Title"] = createPartFromString(title)
            fields["Category"] = createPartFromString(category.toString())
            fields["RecordDate"] = createPartFromString(convertedRecordDate)
            description?.let { fields["Description"] = createPartFromString(it) }
            doctorName?.let { fields["DoctorName"] = createPartFromString(it) }
            centerName?.let { fields["CenterName"] = createPartFromString(it) }

            val files = fileUris.mapNotNull { uri -> uriToMultipartBodyPart(context, uri, "Files") }

            val resp = api.createMedicalRecord(url, fields, files)
            if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                Result.success(resp.body()?.data ?: CreateResponseDto())
            } else {
                val errBody = resp.errorBody()?.string()
                val msg = parseErrorMessage(errBody, resp.body()?.message ?: "ثبت پرونده ناموفق بود.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("MedicalRecordRepo", "createMedicalRecord error", e)
            Result.failure(e)
        }
    }

    suspend fun updateMedicalRecord(
        id: String,
        title: String,
        category: Int,
        recordDate: String,
        description: String? = null,
        doctorName: String? = null,
        centerName: String? = null,
        fileUris: List<Uri> = emptyList()
    ): Result<CreateResponseDto> {
        return try {
            val url = "$baseUrl/${ApiConfig.MEDICAL_RECORDS}/$id"
            val fields = mutableMapOf<String, RequestBody>()

            val convertedRecordDate = if (recordDate.isNotBlank()) {
                DateUtils.convertJalaliStringToGregorian(recordDate)
            } else recordDate

            fields["Id"] = createPartFromString(id)
            fields["Title"] = createPartFromString(title)
            fields["Category"] = createPartFromString(category.toString())
            fields["RecordDate"] = createPartFromString(convertedRecordDate)
            description?.let { fields["Description"] = createPartFromString(it) }
            doctorName?.let { fields["DoctorName"] = createPartFromString(it) }
            centerName?.let { fields["CenterName"] = createPartFromString(it) }

            val files = fileUris.mapNotNull { uri -> uriToMultipartBodyPart(context, uri, "Files") }

            val resp = api.updateMedicalRecord(url, fields, files)
            if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                Result.success(resp.body()?.data ?: CreateResponseDto())
            } else {
                val errBody = resp.errorBody()?.string()
                val msg = parseErrorMessage(errBody, resp.body()?.message ?: "ویرایش پرونده ناموفق بود.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("MedicalRecordRepo", "updateMedicalRecord error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMedicalRecord(id: String): Result<Unit> {
        return try {
            val url = "$baseUrl/${ApiConfig.MEDICAL_RECORDS}/$id"
            val resp = api.deleteMedicalRecord(url)
            if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.body()?.message ?: "خطا در حذف پرونده پزشکی."))
            }
        } catch (e: Exception) {
            Log.e("MedicalRecordRepo", "deleteMedicalRecord error", e)
            Result.failure(e)
        }
    }

    suspend fun interpretUpload(fileUri: Uri): Result<String> {
        return try {
            val url = "$baseUrl/${ApiConfig.FILES_INTERPRET_UPLOAD}"
            val part = uriToMultipartBodyPart(context, fileUri, "file")
            if (part == null) return Result.failure(Exception("خطا در خواندن فایل."))

            val resp = api.interpretUpload(url, listOf(part))
            if (resp.isSuccessful && resp.body() != null) {
                val json = resp.body()!!
                val resultText = extractInterpretationText(json)
                Result.success(resultText)
            } else {
                Result.failure(Exception("خطا در تحلیل تصویر/فایل."))
            }
        } catch (e: Exception) {
            Log.e("MedicalRecordRepo", "interpretUpload error", e)
            Result.failure(e)
        }
    }

    suspend fun getFileInterpretation(fileId: String): Result<String> {
        return try {
            val url = "$baseUrl/${ApiConfig.FILES_INTERPRETATION}/$fileId/interpretation"
            val resp = api.getFileInterpretation(url)
            if (resp.isSuccessful && resp.body() != null) {
                val json = resp.body()!!
                val resultText = extractInterpretationText(json)
                Result.success(resultText)
            } else {
                Result.failure(Exception("خطا در دریافت تحلیل فایل."))
            }
        } catch (e: Exception) {
            Log.e("MedicalRecordRepo", "getFileInterpretation error", e)
            Result.failure(e)
        }
    }

    suspend fun interpretFile(fileId: String, force: Boolean = false): Result<String> {
        return try {
            val url = "$baseUrl/${ApiConfig.FILES_INTERPRETATION}/$fileId/interpretation?force=$force"
            val resp = api.interpretFile(url)
            if (resp.isSuccessful && resp.body() != null) {
                val json = resp.body()!!
                val resultText = extractInterpretationText(json)
                Result.success(resultText)
            } else {
                Result.failure(Exception("خطا در درخواست تحلیل فایل."))
            }
        } catch (e: Exception) {
            Log.e("MedicalRecordRepo", "interpretFile error", e)
            Result.failure(e)
        }
    }

    private fun createPartFromString(value: String): RequestBody {
        return value.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    private fun uriToMultipartBodyPart(context: Context, uri: Uri, partName: String): MultipartBody.Part? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.use { it.readBytes() }
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData(partName, fileName, requestBody)
        } catch (e: Exception) {
            Log.e("MedicalRecordRepo", "Error reading uri: $uri", e)
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            val path = uri.path
            val cut = path?.lastIndexOf('/')
            if (path != null && cut != null && cut != -1) {
                name = path.substring(cut + 1)
            }
        }
        return name
    }

    private fun extractInterpretationText(json: kotlinx.serialization.json.JsonElement): String {
        return if (json is JsonObject) {
            json["text"]?.jsonPrimitive?.content
                ?: json["interpretation"]?.jsonPrimitive?.content
                ?: json["message"]?.jsonPrimitive?.content
                ?: json.toString()
        } else {
            json.toString()
        }
    }

    private fun parseErrorMessage(errorBodyString: String?, defaultMessage: String): String {
        if (errorBodyString.isNullOrBlank()) return defaultMessage
        return try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.parseToJsonElement(errorBodyString)
            if (json is JsonObject) {
                val msg = json["message"]?.jsonPrimitive?.content
                val title = json["title"]?.jsonPrimitive?.content
                val errorsObj = json["errors"] as? JsonObject
                val details = errorsObj?.entries?.joinToString(" | ") { (k, v) ->
                    val firstErr = if (v is kotlinx.serialization.json.JsonArray) v.firstOrNull()?.jsonPrimitive?.content else v.jsonPrimitive.content
                    if (firstErr != null) "$k: $firstErr" else k
                }
                details ?: msg ?: title ?: defaultMessage
            } else {
                defaultMessage
            }
        } catch (e: Exception) {
            defaultMessage
        }
    }
}

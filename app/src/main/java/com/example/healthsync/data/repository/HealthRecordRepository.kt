package com.example.healthsync.data.repository

import android.util.Log
import com.example.healthsync.data.local.SecureConfigStore
import com.example.healthsync.data.remote.HealthApi
import com.example.healthsync.domain.model.*
import com.example.healthsync.util.ApiConfig
import com.example.healthsync.util.DateUtils
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRecordRepository @Inject constructor(
    private val api: HealthApi,
    private val config: SecureConfigStore,
) {
    private val baseUrl: String
        get() = config.baseUrl.trimEnd('/')

    suspend fun getMetricTypes(): Result<List<HealthMetricViewModel>> {
        return try {
            val url = "$baseUrl/${ApiConfig.HEALTH_RECORDS_METRIC_TYPES}"
            val resp = api.getMetricTypes(url)
            if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                Result.success(resp.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(resp.body()?.message ?: "خطا در دریافت لیست شاخص‌ها"))
            }
        } catch (e: Exception) {
            Log.e("HealthRecordRepo", "getMetricTypes error", e)
            Result.failure(e)
        }
    }

    suspend fun getMetricComponents(): Result<List<HealthMetricComponentViewModel>> {
        return try {
            val url = "$baseUrl/${ApiConfig.HEALTH_RECORDS_METRIC_COMPONENTS}"
            val resp = api.getMetricComponents(url)
            if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                Result.success(resp.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(resp.body()?.message ?: "خطا در دریافت اجزای شاخص‌ها"))
            }
        } catch (e: Exception) {
            Log.e("HealthRecordRepo", "getMetricComponents error", e)
            Result.failure(e)
        }
    }

    suspend fun createHealthRecord(dto: CreateHealthRecordDto): Result<Unit> {
        return try {
            val url = "$baseUrl/${ApiConfig.HEALTH_RECORDS}"
            val resp = api.createHealthRecord(url, dto)
            if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.body()?.message ?: "خطا در ثبت پایش سلامت"))
            }
        } catch (e: Exception) {
            Log.e("HealthRecordRepo", "createHealthRecord error", e)
            Result.failure(e)
        }
    }

    suspend fun updateHealthRecord(dto: UpdateHealthRecordDto): Result<Unit> {
        return try {
            val url = "$baseUrl/${ApiConfig.HEALTH_RECORDS}/${dto.id}"
            val resp = api.updateHealthRecord(url, dto)
            if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.body()?.message ?: "خطا در ویرایش پایش سلامت"))
            }
        } catch (e: Exception) {
            Log.e("HealthRecordRepo", "updateHealthRecord error", e)
            Result.failure(e)
        }
    }

    suspend fun getHealthRecordsByUser(
        page: Int = 1,
        pageSize: Int = 10,
        search: String = "",
        filterDate: String = ""
    ): Result<PagedResultDto<HealthRecordIndexViewModel>> {
        return try {
            val convertedFilterDate = if (filterDate.isNotBlank()) {
                DateUtils.convertJalaliStringToGregorian(filterDate)
            } else ""
            val encSearch = URLEncoder.encode(search, "UTF-8")
            val encFilterDate = URLEncoder.encode(convertedFilterDate, "UTF-8")
            val url = "$baseUrl/${ApiConfig.HEALTH_RECORDS_BY_USER}?page=$page&pageSize=$pageSize&search=$encSearch&filterDate=$encFilterDate"
            Log.d("HealthRecordRepo", "Fetching records URL: $url (rawFilterDate: $filterDate, convertedFilterDate: $convertedFilterDate)")
            val resp = api.getHealthRecordsByUser(url)
            if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                val rawPaged = resp.body()?.data ?: PagedResultDto()
                val cleanedItems = rawPaged.items.filterNotNull().map { rec ->
                    rec.copy(metricValues = rec.metricValues.filterNotNull())
                }
                val cleanedPaged = PagedResultDto(
                    items = cleanedItems,
                    page = rawPaged.page,
                    pageSize = rawPaged.pageSize,
                    totalCount = rawPaged.totalCount,
                    totalPages = rawPaged.totalPages
                )
                Log.d("HealthRecordRepo", "Fetched ${cleanedPaged.items.size} items (totalCount: ${cleanedPaged.totalCount})")
                Result.success(cleanedPaged)
            } else {
                val msg = resp.body()?.message ?: "HTTP ${resp.code()}"
                Log.e("HealthRecordRepo", "Failed to fetch records: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("HealthRecordRepo", "getHealthRecordsByUser exception", e)
            Result.failure(e)
        }
    }

    suspend fun deleteHealthRecord(id: String): Result<Unit> {
        return try {
            val url = "$baseUrl/${ApiConfig.HEALTH_RECORDS}/$id"
            val resp = api.deleteHealthRecord(url)
            if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.body()?.message ?: "خطا در حذف پایش سلامت"))
            }
        } catch (e: Exception) {
            Log.e("HealthRecordRepo", "deleteHealthRecord error", e)
            Result.failure(e)
        }
    }

    suspend fun getLatestInsight(): Result<String> {
        return try {
            val url = "$baseUrl/${ApiConfig.HEALTH_INSIGHTS_LAST}"
            val resp = api.getLatestInsight(url)
            if (resp.isSuccessful && resp.body() != null) {
                val jsonElem = resp.body()!!
                val text = jsonElem.jsonObject["text"]?.jsonPrimitive?.content ?: ""
                if (text.isNotBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("متن تحلیلی پیدا نشد."))
                }
            } else {
                Result.failure(Exception("عدم دریافت پاسخ از سرویس هوش مصنوعی"))
            }
        } catch (e: Exception) {
            Log.e("HealthRecordRepo", "getLatestInsight error", e)
            Result.failure(e)
        }
    }
}

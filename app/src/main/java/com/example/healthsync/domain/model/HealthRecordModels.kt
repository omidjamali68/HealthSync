package com.example.healthsync.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateHealthRecordDto(
    @SerialName("recordedAt")
    val recordedAt: String,
    @SerialName("metricTypeId")
    val metricTypeId: String,
    @SerialName("source")
    val source: Int = 1,
    @SerialName("metricValues")
    val metricValues: List<HealthMetricValueInputDto> = emptyList()
)

@Serializable
data class UpdateHealthRecordDto(
    @SerialName("id")
    val id: String,
    @SerialName("recordedAt")
    val recordedAt: String,
    @SerialName("metricValues")
    val metricValues: List<HealthMetricValueInputDto> = emptyList()
)

@Serializable
data class HealthMetricValueInputDto(
    @SerialName("componentId")
    val componentId: String,
    @SerialName("value")
    val value: Double
)

@Serializable
data class HealthMetricViewModel(
    @SerialName("id")
    val id: String = "",
    @SerialName("code")
    val code: String = "",
    @SerialName("title")
    val title: String = "",
    @SerialName("unit")
    val unit: String = "",
    @SerialName("valueType")
    val valueType: Int = 1
)

@Serializable
data class HealthMetricComponentViewModel(
    @SerialName("id")
    val id: String = "",
    @SerialName("healthMetricTypeId")
    val healthMetricTypeId: String = "",
    @SerialName("code")
    val code: String = "",
    @SerialName("title")
    val title: String = "",
    @SerialName("unit")
    val unit: String = ""
)

@Serializable
data class HealthRecordIndexViewModel(
    @SerialName("id")
    val id: String = "",
    @SerialName("userId")
    val userId: String? = null,
    @SerialName("source")
    val source: Int = 1,
    @SerialName("recordedAt")
    val recordedAt: String = "",
    @SerialName("createdAt")
    val createdAt: String = "",
    @SerialName("metricValues")
    val metricValues: List<HealthMetricValueDto?> = emptyList()
)

@Serializable
data class HealthMetricValueDto(
    @SerialName("id")
    val id: JsonElement? = null,
    @SerialName("healthRecordId")
    val healthRecordId: String = "",
    @SerialName("healthMetricComponentId")
    val healthMetricComponentId: String = "",
    @SerialName("value")
    val value: Double = 0.0,
    @SerialName("component")
    val component: HealthMetricComponentDto? = null
)

@Serializable
data class HealthMetricComponentDto(
    @SerialName("id")
    val id: String = "",
    @SerialName("healthMetricTypeId")
    val healthMetricTypeId: String = "",
    @SerialName("code")
    val code: String = "",
    @SerialName("title")
    val title: String = "",
    @SerialName("unit")
    val unit: String = "",
    @SerialName("healthMetricType")
    val healthMetricType: HealthMetricTypeDto? = null
)

@Serializable
data class HealthMetricTypeDto(
    @SerialName("id")
    val id: String = "",
    @SerialName("code")
    val code: String = "",
    @SerialName("title")
    val title: String = "",
    @SerialName("isActive")
    val isActive: Boolean = true,
    @SerialName("components")
    val components: List<HealthMetricComponentSummaryDto?> = emptyList()
)

@Serializable
data class HealthMetricComponentSummaryDto(
    @SerialName("id")
    val id: String = "",
    @SerialName("healthMetricTypeId")
    val healthMetricTypeId: String = "",
    @SerialName("code")
    val code: String = "",
    @SerialName("title")
    val title: String = "",
    @SerialName("unit")
    val unit: String = ""
)

@Serializable
data class PagedResultDto<T>(
    @SerialName("items")
    val items: List<T?> = emptyList(),
    @SerialName("page")
    val page: Int = 1,
    @SerialName("pageSize")
    val pageSize: Int = 10,
    @SerialName("totalCount")
    val totalCount: Int = 0,
    @SerialName("totalPages")
    val totalPages: Int = 0
)

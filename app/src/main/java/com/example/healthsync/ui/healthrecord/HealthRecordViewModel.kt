package com.example.healthsync.ui.healthrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthsync.data.repository.HealthRecordRepository
import com.example.healthsync.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HealthRecordUiState(
    val isLoading: Boolean = false,
    val metricTypes: List<HealthMetricViewModel> = emptyList(),
    val metricComponents: List<HealthMetricComponentViewModel> = emptyList(),
    val selectedMetricType: HealthMetricViewModel? = null,
    val componentValues: Map<String, String> = emptyMap(),
    val records: List<HealthRecordIndexViewModel> = emptyList(),
    
    // Pagination & Search
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val totalCount: Int = 0,
    val totalPages: Int = 0,
    val searchQuery: String = "",
    val filterDateQuery: String = "",
    val selectedDetailRecord: HealthRecordIndexViewModel? = null,

    val latestInsight: String? = null,
    val insightLoading: Boolean = false,
    val message: String? = null,
    val isSuccessMessage: Boolean = false,
    val selectedTab: Int = 0,
)

@HiltViewModel
class HealthRecordViewModel @Inject constructor(
    private val repository: HealthRecordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HealthRecordUiState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val typesResult = repository.getMetricTypes()
            val componentsResult = repository.getMetricComponents()

            val types = typesResult.getOrDefault(emptyList())
            val components = componentsResult.getOrDefault(emptyList())

            val selectedType = types.firstOrNull()
            val initialValues = mutableMapOf<String, String>()
            if (selectedType != null) {
                components.filter { it.healthMetricTypeId == selectedType.id }.forEach { comp ->
                    initialValues[comp.id] = ""
                }
            }

            _state.value = _state.value.copy(
                isLoading = false,
                metricTypes = types,
                metricComponents = components,
                selectedMetricType = selectedType,
                componentValues = initialValues
            )

            loadRecords(1)
            loadInsight()
        }
    }

    fun selectTab(tab: Int) {
        _state.value = _state.value.copy(selectedTab = tab)
        if (tab == 1) {
            loadRecords(_state.value.currentPage)
        }
    }

    fun selectMetricType(type: HealthMetricViewModel) {
        val components = _state.value.metricComponents.filter { it.healthMetricTypeId == type.id }
        val newValues = components.associate { it.id to "" }
        _state.value = _state.value.copy(
            selectedMetricType = type,
            componentValues = newValues
        )
    }

    fun updateComponentValue(componentId: String, value: String) {
        val updated = _state.value.componentValues.toMutableMap()
        updated[componentId] = value
        _state.value = _state.value.copy(componentValues = updated)
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun setFilterDateQuery(date: String) {
        _state.value = _state.value.copy(filterDateQuery = date)
    }

    fun applyFilters() {
        loadRecords(page = 1)
    }

    fun clearFilters() {
        _state.value = _state.value.copy(searchQuery = "", filterDateQuery = "")
        loadRecords(page = 1)
    }

    fun goToPage(page: Int) {
        if (page in 1..(_state.value.totalPages.coerceAtLeast(1))) {
            loadRecords(page = page)
        }
    }

    fun selectDetailRecord(record: HealthRecordIndexViewModel?) {
        _state.value = _state.value.copy(selectedDetailRecord = record)
    }

    fun submitRecord() {
        val selectedType = _state.value.selectedMetricType ?: return
        val currentValues = _state.value.componentValues

        val inputDtos = currentValues.mapNotNull { (compId, valStr) ->
            val dVal = valStr.toDoubleOrNull() ?: return@mapNotNull null
            HealthMetricValueInputDto(componentId = compId, value = dVal)
        }

        if (inputDtos.isEmpty()) {
            _state.value = _state.value.copy(
                message = "لطفاً حداقل یک مقدار معتبر وارد کنید.",
                isSuccessMessage = false
            )
            return
        }

        val nowIran = ZonedDateTime.now(ZoneId.of("Asia/Tehran")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        val dto = CreateHealthRecordDto(
            recordedAt = nowIran,
            metricTypeId = selectedType.id,
            source = 1,
            metricValues = inputDtos
        )

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, message = null)
            val result = repository.createHealthRecord(dto)
            result.onSuccess {
                val resetValues = currentValues.keys.associateWith { "" }
                _state.value = _state.value.copy(
                    isLoading = false,
                    componentValues = resetValues,
                    message = "پایش سلامت با موفقیت ثبت شد.",
                    isSuccessMessage = true
                )
                loadRecords(1)
            }.onFailure { ex ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    message = ex.message ?: "خطا در ثبت پایش سلامت.",
                    isSuccessMessage = false
                )
            }
        }
    }

    fun updateRecord(id: String, recordedAt: String, updatedValues: Map<String, String>) {
        val inputDtos = updatedValues.mapNotNull { (compId, valStr) ->
            val dVal = valStr.toDoubleOrNull() ?: return@mapNotNull null
            HealthMetricValueInputDto(componentId = compId, value = dVal)
        }

        if (inputDtos.isEmpty()) {
            _state.value = _state.value.copy(
                message = "لطفاً حداقل یک مقدار معتبر وارد کنید.",
                isSuccessMessage = false
            )
            return
        }

        val dto = UpdateHealthRecordDto(
            id = id,
            recordedAt = recordedAt,
            metricValues = inputDtos
        )

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, message = null)
            val result = repository.updateHealthRecord(dto)
            result.onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    selectedDetailRecord = null,
                    message = "پایش سلامت با موفقیت ویرایش شد.",
                    isSuccessMessage = true
                )
                loadRecords(_state.value.currentPage)
            }.onFailure { ex ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    message = ex.message ?: "خطا در ویرایش پایش سلامت.",
                    isSuccessMessage = false
                )
            }
        }
    }

    fun loadRecords(page: Int = _state.value.currentPage) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val search = _state.value.searchQuery
            val filterDate = _state.value.filterDateQuery
            val pageSize = _state.value.pageSize

            repository.getHealthRecordsByUser(
                page = page,
                pageSize = pageSize,
                search = search,
                filterDate = filterDate
            ).onSuccess { paged ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    records = paged.items.filterNotNull(),
                    currentPage = paged.page.coerceAtLeast(1),
                    pageSize = paged.pageSize,
                    totalCount = paged.totalCount,
                    totalPages = paged.totalPages
                )
            }.onFailure { ex ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    message = ex.message ?: "خطا در دریافت لیست پایش‌ها"
                )
            }
        }
    }

    fun loadInsight() {
        viewModelScope.launch {
            _state.value = _state.value.copy(insightLoading = true)
            repository.getLatestInsight().onSuccess { text ->
                _state.value = _state.value.copy(latestInsight = text, insightLoading = false)
            }.onFailure {
                _state.value = _state.value.copy(insightLoading = false)
            }
        }
    }

    fun requestNewInsight() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                insightLoading = true,
                message = "درخواست تحلیل هوشمند جدید ارسال شد. (توجه: محدودیت روزانه ۳ بار می‌باشد.)"
            )
            repository.getLatestInsight().onSuccess { text ->
                _state.value = _state.value.copy(latestInsight = text, insightLoading = false)
            }.onFailure { ex ->
                _state.value = _state.value.copy(
                    insightLoading = false,
                    message = ex.message ?: "خطا در دریافت تحلیل هوش مصنوعی."
                )
            }
        }
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.deleteHealthRecord(id).onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    message = "پایش با موفقیت حذف شد.",
                    isSuccessMessage = true
                )
                loadRecords(_state.value.currentPage)
            }.onFailure { ex ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    message = ex.message ?: "خطا در حذف پایش.",
                    isSuccessMessage = false
                )
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}

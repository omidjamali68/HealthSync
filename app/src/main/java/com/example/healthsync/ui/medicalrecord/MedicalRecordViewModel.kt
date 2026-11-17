package com.example.healthsync.ui.medicalrecord

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthsync.data.repository.MedicalRecordRepository
import com.example.healthsync.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MedicalRecordUiState(
    val isLoading: Boolean = false,
    val records: List<MedicalRecordDto> = emptyList(),

    // Pagination & Filtering
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val totalCount: Int = 0,
    val totalPages: Int = 0,
    val searchQuery: String = "",
    val selectedCategory: MedicalRecordCategory? = null,
    val filterDateQuery: String = "",
    val selectedTab: Int = 0, // 0: History, 1: New/Edit Record

    // Form fields
    val editingRecordId: String? = null,
    val formTitle: String = "",
    val formCategory: MedicalRecordCategory = MedicalRecordCategory.VISIT,
    val formRecordDate: String = "",
    val formDescription: String = "",
    val formDoctorName: String = "",
    val formCenterName: String = "",
    val existingFiles: List<MedicalRecordFileDto> = emptyList(),
    val selectedFileUris: List<Uri> = emptyList(),

    // Detail & AI Interpretation
    val selectedRecordDetail: MedicalRecordDto? = null,
    val interpretationLoading: Boolean = false,
    val activeFileInterpretation: String? = null,
    val activeFileId: String? = null,

    // Feedback messages
    val message: String? = null,
    val isSuccessMessage: Boolean = false
)

@HiltViewModel
class MedicalRecordViewModel @Inject constructor(
    private val repository: MedicalRecordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MedicalRecordUiState())
    val state = _state.asStateFlow()

    init {
        loadRecords(1)
    }

    fun selectTab(tab: Int) {
        _state.value = _state.value.copy(selectedTab = tab)
        if (tab == 0) {
            loadRecords(_state.value.currentPage)
        }
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun setSelectedCategory(category: MedicalRecordCategory?) {
        _state.value = _state.value.copy(selectedCategory = category)
    }

    fun setFilterDateQuery(date: String) {
        _state.value = _state.value.copy(filterDateQuery = date)
    }

    fun applyFilters() {
        loadRecords(1)
    }

    fun clearFilters() {
        _state.value = _state.value.copy(
            searchQuery = "",
            selectedCategory = null,
            filterDateQuery = ""
        )
        loadRecords(1)
    }

    fun goToPage(page: Int) {
        if (page in 1..(_state.value.totalPages.coerceAtLeast(1))) {
            loadRecords(page)
        }
    }

    fun loadRecords(page: Int = _state.value.currentPage) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val categoryVal = _state.value.selectedCategory?.value?.toString() ?: ""
            val searchWithCategory = if (categoryVal.isNotBlank()) {
                "${_state.value.searchQuery} category:$categoryVal".trim()
            } else {
                _state.value.searchQuery
            }

            repository.getMedicalRecordsByUser(
                page = page,
                pageSize = _state.value.pageSize,
                search = searchWithCategory,
                filterDate = _state.value.filterDateQuery
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
                    message = ex.message ?: "خطا در دریافت لیست پرونده‌ها"
                )
            }
        }
    }

    // Form inputs update
    fun updateFormTitle(title: String) {
        _state.value = _state.value.copy(formTitle = title)
    }

    fun updateFormCategory(category: MedicalRecordCategory) {
        _state.value = _state.value.copy(formCategory = category)
    }

    fun updateFormRecordDate(date: String) {
        _state.value = _state.value.copy(formRecordDate = date)
    }

    fun updateFormDescription(desc: String) {
        _state.value = _state.value.copy(formDescription = desc)
    }

    fun updateFormDoctorName(doctor: String) {
        _state.value = _state.value.copy(formDoctorName = doctor)
    }

    fun updateFormCenterName(center: String) {
        _state.value = _state.value.copy(formCenterName = center)
    }

    fun addSelectedFiles(uris: List<Uri>) {
        val current = _state.value.selectedFileUris
        _state.value = _state.value.copy(selectedFileUris = current + uris)
    }

    fun removeSelectedFile(uri: Uri) {
        val current = _state.value.selectedFileUris.toMutableList()
        current.remove(uri)
        _state.value = _state.value.copy(selectedFileUris = current)
    }

    fun removeExistingFile(fileId: String) {
        val current = _state.value.existingFiles.filter { it.id != fileId }
        _state.value = _state.value.copy(existingFiles = current)
    }

    fun prepareEditForm(record: MedicalRecordDto) {
        _state.value = _state.value.copy(
            editingRecordId = record.id,
            formTitle = record.title,
            formCategory = MedicalRecordCategory.fromValue(record.category),
            formRecordDate = record.recordDate ?: "",
            formDescription = record.description ?: "",
            formDoctorName = record.doctorName ?: "",
            formCenterName = record.centerName ?: "",
            existingFiles = record.files,
            selectedFileUris = emptyList(),
            selectedTab = 1
        )

        // Fetch full record details from API to ensure complete files list is populated
        viewModelScope.launch {
            repository.getMedicalRecordById(record.id).onSuccess { fullRecord ->
                if (_state.value.editingRecordId == record.id) {
                    _state.value = _state.value.copy(
                        existingFiles = fullRecord.files,
                        formTitle = fullRecord.title.ifBlank { _state.value.formTitle },
                        formCategory = MedicalRecordCategory.fromValue(fullRecord.category),
                        formRecordDate = fullRecord.recordDate ?: _state.value.formRecordDate,
                        formDescription = fullRecord.description ?: _state.value.formDescription,
                        formDoctorName = fullRecord.doctorName ?: _state.value.formDoctorName,
                        formCenterName = fullRecord.centerName ?: _state.value.formCenterName
                    )
                }
            }
        }
    }

    fun clearForm() {
        _state.value = _state.value.copy(
            editingRecordId = null,
            formTitle = "",
            formCategory = MedicalRecordCategory.VISIT,
            formRecordDate = "",
            formDescription = "",
            formDoctorName = "",
            formCenterName = "",
            existingFiles = emptyList(),
            selectedFileUris = emptyList()
        )
    }

    fun submitForm() {
        val s = _state.value
        if (s.formTitle.isBlank()) {
            _state.value = s.copy(
                message = "لطفاً عنوان پرونده را وارد کنید.",
                isSuccessMessage = false
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, message = null)

            val recordDate = if (s.formRecordDate.isBlank()) {
                ZonedDateTime.now(ZoneId.of("Asia/Tehran"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            } else {
                s.formRecordDate
            }

            val result = if (s.editingRecordId != null) {
                repository.updateMedicalRecord(
                    id = s.editingRecordId,
                    title = s.formTitle,
                    category = s.formCategory.value,
                    recordDate = recordDate,
                    description = s.formDescription.ifBlank { null },
                    doctorName = s.formDoctorName.ifBlank { null },
                    centerName = s.formCenterName.ifBlank { null },
                    fileUris = s.selectedFileUris
                )
            } else {
                repository.createMedicalRecord(
                    title = s.formTitle,
                    category = s.formCategory.value,
                    recordDate = recordDate,
                    description = s.formDescription.ifBlank { null },
                    doctorName = s.formDoctorName.ifBlank { null },
                    centerName = s.formCenterName.ifBlank { null },
                    fileUris = s.selectedFileUris
                )
            }

            result.onSuccess {
                val msg = if (s.editingRecordId != null) "پرونده با موفقیت ویرایش شد." else "پرونده با موفقیت ثبت شد."
                clearForm()
                _state.value = _state.value.copy(
                    isLoading = false,
                    selectedTab = 0,
                    message = msg,
                    isSuccessMessage = true
                )
                loadRecords(1)
            }.onFailure { ex ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    message = ex.message ?: "خطا در ذخیره‌سازی پرونده.",
                    isSuccessMessage = false
                )
            }
        }
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.deleteMedicalRecord(id).onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    message = "پرونده پزشکی با موفقیت حذف شد.",
                    isSuccessMessage = true
                )
                loadRecords(_state.value.currentPage)
            }.onFailure { ex ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    message = ex.message ?: "خطا در حذف پرونده پزشکی.",
                    isSuccessMessage = false
                )
            }
        }
    }

    fun selectRecordDetail(record: MedicalRecordDto?) {
        _state.value = _state.value.copy(
            selectedRecordDetail = record,
            activeFileInterpretation = null,
            activeFileId = null
        )
    }

    // AI Interpretation Methods
    fun interpretUploadedFile(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(interpretationLoading = true)
            repository.interpretUpload(uri).onSuccess { text ->
                _state.value = _state.value.copy(
                    interpretationLoading = false,
                    activeFileInterpretation = text
                )
            }.onFailure { ex ->
                _state.value = _state.value.copy(
                    interpretationLoading = false,
                    message = ex.message ?: "خطا در تحلیل هوشمند تصویر/فایل."
                )
            }
        }
    }

    fun getFileInterpretation(fileId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                interpretationLoading = true,
                activeFileId = fileId
            )
            repository.getFileInterpretation(fileId).onSuccess { text ->
                _state.value = _state.value.copy(
                    interpretationLoading = false,
                    activeFileInterpretation = text
                )
            }.onFailure { ex ->
                _state.value = _state.value.copy(
                    interpretationLoading = false,
                    message = ex.message ?: "خطا در دریافت تحلیل فایل."
                )
            }
        }
    }

    fun reInterpretFile(fileId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                interpretationLoading = true,
                activeFileId = fileId
            )
            repository.interpretFile(fileId, force = true).onSuccess { text ->
                _state.value = _state.value.copy(
                    interpretationLoading = false,
                    activeFileInterpretation = text
                )
            }.onFailure { ex ->
                _state.value = _state.value.copy(
                    interpretationLoading = false,
                    message = ex.message ?: "خطا در درخواست تحلیل مجدد."
                )
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}

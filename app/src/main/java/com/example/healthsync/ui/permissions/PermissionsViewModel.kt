package com.example.healthsync.ui.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthsync.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionsUiState(
    val availability: HealthRepository.Availability = HealthRepository.Availability.NotSupported,
    val granted: Boolean = false,
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val health: HealthRepository,
) : ViewModel() {

    val required: Set<String> = health.permissions

    private val _state = MutableStateFlow(PermissionsUiState())
    val state = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val avail = health.availability()
            val granted = if (avail == HealthRepository.Availability.Available) health.hasAllPermissions() else false
            _state.value = PermissionsUiState(avail, granted)
        }
    }

    fun onPermissionResult(allGranted: Boolean) {
        _state.value = _state.value.copy(granted = allGranted)
    }
}

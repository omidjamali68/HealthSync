package com.example.healthsync.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthsync.R
import com.example.healthsync.data.repository.AuthRepository
import com.example.healthsync.util.SessionManager
import com.example.healthsync.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun onMobileNumberChange(number: String) {
        if (number.length <= 11) {
            _uiState.update { it.copy(mobileNumber = number, error = null) }
        }
    }

    fun editMobileNumber() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isCodeSent = false,
                verificationCode = "",
                timerSeconds = 0,
                error = null
            )
        }
    }

    fun onVerificationCodeChange(code: String) {
        val filtered = code.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(verificationCode = filtered, error = null) }
        if (filtered.length == 6) {
            login()
        }
    }

    fun onOtpAutoFilled(code: String) {
        val filtered = code.filter { it.isDigit() }.take(6)
        if (filtered.length == 6) {
            _uiState.update { it.copy(verificationCode = filtered, error = null) }
            login()
        }
    }

    fun sendCode() {
        val mobile = _uiState.value.mobileNumber
        if (mobile.length != 11 || !mobile.startsWith("09")) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.invalid_mobile)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.sendVerificationCode(mobile)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isCodeSent = true) }
                    startTimer()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message?.let { msg -> UiText.DynamicString(msg) }) }
                }
        }
    }

    fun login() {
        val codeString = _uiState.value.verificationCode
        if (codeString.length != 6) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.invalid_code_length)) }
            return
        }

        val code = codeString.toLongOrNull() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.registerOrLogin(_uiState.value.mobileNumber, code)
                .onSuccess { response ->
                    response.data?.token?.let { token ->
                        sessionManager.saveToken(token)
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    } ?: run {
                        _uiState.update { it.copy(isLoading = false, error = UiText.StringResource(R.string.error_getting_token)) }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message?.let { msg -> UiText.DynamicString(msg) }) }
                }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timerSeconds = 120) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(timerSeconds = it.timerSeconds - 1) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

data class AuthUiState(
    val mobileNumber: String = "",
    val verificationCode: String = "",
    val timerSeconds: Int = 0,
    val isLoading: Boolean = false,
    val isCodeSent: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: UiText? = null
)

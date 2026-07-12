package com.example.healthsync.data.repository

import android.util.Log
import com.example.healthsync.data.local.SecureConfigStore
import com.example.healthsync.data.remote.AuthApi
import com.example.healthsync.domain.model.LoginResponseDto
import com.example.healthsync.domain.model.RegisterOrLoginDto
import com.example.healthsync.domain.model.ResponseDto
import com.example.healthsync.domain.model.SendCodeResponse
import com.example.healthsync.util.ApiConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val config: SecureConfigStore
) {
    suspend fun sendVerificationCode(mobile: String): Result<ResponseDto<SendCodeResponse>> {
        return try {
            val base = config.baseUrl.trimEnd('/')
            val url = "$base/${ApiConfig.AUTH_SEND_CODE}/$mobile"
            Log.d("HealthSync", "Request URL (SendCode): $url")
            val response = authApi.sendVerificationCode(url)
            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to send code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerOrLogin(userName: String, code: Long): Result<ResponseDto<LoginResponseDto>> {
        return try {
            val base = config.baseUrl.trimEnd('/')
            val url = "$base/${ApiConfig.AUTH_LOGIN}"
            Log.d("HealthSync", "Request URL (Login): $url")
            val response = authApi.registerOrLogin(url, RegisterOrLoginDto(userName, code))
            if (response.isSuccessful) {
                Result.success(response.body() ?: throw Exception("Empty response"))
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

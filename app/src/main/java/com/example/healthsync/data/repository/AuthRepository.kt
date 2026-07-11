package com.example.healthsync.data.repository

import com.example.healthsync.data.remote.AuthApi
import com.example.healthsync.domain.model.LoginResponseDto
import com.example.healthsync.domain.model.RegisterOrLoginDto
import com.example.healthsync.domain.model.ResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi
) {
    suspend fun sendVerificationCode(mobile: String): Result<ResponseDto<Unit>> {
        return try {
            val response = authApi.sendVerificationCode(mobile)
            if (response.isSuccessful) {
                Result.success(response.body() ?: ResponseDto(true, null, Unit))
            } else {
                Result.failure(Exception("Failed to send code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerOrLogin(userName: String, code: Long): Result<ResponseDto<LoginResponseDto>> {
        return try {
            val response = authApi.registerOrLogin(RegisterOrLoginDto(userName, code))
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

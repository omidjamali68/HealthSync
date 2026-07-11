package com.example.healthsync.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterOrLoginDto(
    @SerialName("UserName")
    val userName: String,
    @SerialName("VerificationCode")
    val verificationCode: Long
)

@Serializable
data class LoginResponseDto(
    @SerialName("Id")
    val id: String,
    @SerialName("Mobile")
    val mobile: String,
    @SerialName("TenantId")
    val tenantId: String,
    @SerialName("Token")
    val token: String
)

@Serializable
data class ResponseDto<T>(
    @SerialName("IsSuccess")
    val isSuccess: Boolean,
    @SerialName("Message")
    val message: String? = null,
    @SerialName("Data")
    val data: T? = null
)

package com.example.healthsync.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RegisterOrLoginDto(
    @SerialName("userName")
    val userName: String,
    @SerialName("verificationCode")
    val verificationCode: Long
)

@Serializable
data class LoginResponseDto(
    @SerialName("id")
    val id: String,
    @SerialName("mobile")
    val mobile: String,
    @SerialName("tenantId")
    val tenantId: String,
    @SerialName("token")
    val token: String
)

@Serializable
data class SendCodeResponse(
    @SerialName("isUserRegistered")
    val isUserRegistered: JsonElement? = null
)

@Serializable
data class ResponseDto<T>(
    @SerialName("isSuccess")
    val isSuccess: Boolean,
    @SerialName("message")
    val message: String? = null,
    @SerialName("data")
    val data: T? = null
)

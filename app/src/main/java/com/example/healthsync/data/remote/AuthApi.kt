package com.example.healthsync.data.remote

import com.example.healthsync.domain.model.LoginResponseDto
import com.example.healthsync.domain.model.RegisterOrLoginDto
import com.example.healthsync.domain.model.ResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {
    @POST("send-verification-code/{mobile}")
    suspend fun sendVerificationCode(
        @Path("mobile") mobile: String
    ): Response<ResponseDto<Unit>>

    @POST("register-or-login")
    suspend fun registerOrLogin(
        @Body dto: RegisterOrLoginDto
    ): Response<ResponseDto<LoginResponseDto>>
}

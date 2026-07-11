package com.example.healthsync.data.remote

import com.example.healthsync.domain.model.LoginResponseDto
import com.example.healthsync.domain.model.RegisterOrLoginDto
import com.example.healthsync.domain.model.ResponseDto
import com.example.healthsync.domain.model.SendCodeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface AuthApi {
    @POST
    suspend fun sendVerificationCode(
        @Url url: String
    ): Response<ResponseDto<SendCodeResponse>>

    @POST
    suspend fun registerOrLogin(
        @Url url: String,
        @Body dto: RegisterOrLoginDto
    ): Response<ResponseDto<LoginResponseDto>>
}

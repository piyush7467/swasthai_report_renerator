package com.piyush.swasthaireportgenerator.data.api

import com.piyush.swasthaireportgenerator.data.model.ApiResponse
import com.piyush.swasthaireportgenerator.data.model.LoginRequest
import com.piyush.swasthaireportgenerator.data.model.LoginResponse
import com.piyush.swasthaireportgenerator.data.model.RefreshTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequest
    ): Response<ApiResponse<LoginResponse>>

    @POST("auth/logout")
    suspend fun logout(
        @Body request: RefreshTokenRequest
    ): Response<ApiResponse<Void>>
}

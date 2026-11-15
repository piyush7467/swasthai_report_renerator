package com.piyush.swasthaireportgenerator.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

data class PagedResponse<T>(
    @SerializedName("content") val content: List<T> = emptyList(),
    @SerializedName("page") val page: Int = 0,
    @SerializedName("size") val size: Int = 20,
    @SerializedName("totalElements") val totalElements: Long = 0,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("last") val last: Boolean = true
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("userRefId") val userRefId: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("organizationRefId") val organizationRefId: String?
)

data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val userRefId: String,
    val name: String,
    val email: String,
    val role: String,
    val organizationRefId: String?
)

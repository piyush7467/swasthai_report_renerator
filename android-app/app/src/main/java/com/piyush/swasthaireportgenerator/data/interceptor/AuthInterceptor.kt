package com.piyush.swasthaireportgenerator.data.interceptor

import com.piyush.swasthaireportgenerator.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath
        val isPublicAuthEndpoint = path.endsWith("/auth/login") || path.endsWith("/auth/refresh")

        val builder = originalRequest.newBuilder()
            .header("Accept", "application/json")
            .header("X-Client-Type", "ANDROID_LAB_APP")

        if (!isPublicAuthEndpoint) {
            val token = runBlocking { tokenManager.getAccessToken() }
            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
        }

        return chain.proceed(builder.build())
    }
}

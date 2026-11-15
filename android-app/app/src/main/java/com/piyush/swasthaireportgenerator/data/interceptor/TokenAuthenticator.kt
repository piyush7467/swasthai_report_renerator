package com.piyush.swasthaireportgenerator.data.interceptor

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.piyush.swasthaireportgenerator.data.local.SessionManager
import com.piyush.swasthaireportgenerator.data.local.TokenManager
import com.piyush.swasthaireportgenerator.data.model.ApiResponse
import com.piyush.swasthaireportgenerator.data.model.LoginResponse
import com.piyush.swasthaireportgenerator.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

/**
 * Handles transparent single-flight JWT access token refresh when the backend returns HTTP 401.
 * Synchronizes concurrent 401 retries to prevent refresh-token reuse detection on the server.
 */
class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val baseUrlProvider: () -> String
) : Authenticator {

    private val gson = Gson()
    private val refreshLock = Any()

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (path.endsWith("/auth/login") || path.endsWith("/auth/refresh") || path.endsWith("/auth/logout")) {
            return null
        }

        // Prevent infinite retry loops if the refreshed token itself is rejected
        if (response.request.header("X-Auth-Retried") == "true" || responseCount(response) >= 2) {
            return null
        }

        val failedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        synchronized(refreshLock) {
            val latestToken = runBlocking { tokenManager.getAccessToken() }

            // Another concurrent call already refreshed the token; retry directly with it
            if (!latestToken.isNullOrBlank() && latestToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .header("X-Auth-Retried", "true")
                    .build()
            }

            val refreshToken = runBlocking { tokenManager.getRefreshToken() }
            if (refreshToken.isNullOrBlank()) {
                runBlocking { tokenManager.clearSession() }
                SessionManager.notifySessionExpired("Session expired. Please sign in again.")
                return null
            }

            val refreshUrl = "${baseUrlProvider()}auth/refresh"
            val jsonBody = gson.toJson(RefreshTokenRequest(refreshToken))
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .header("Accept", "application/json")
                .header("X-Client-Type", "ANDROID_LAB_APP")
                .post(jsonBody)
                .build()

            return try {
                refreshClient.newCall(refreshRequest).execute().use { refreshResponse ->
                    if (refreshResponse.isSuccessful) {
                        val rawBody = refreshResponse.body?.string()
                        val type = object : TypeToken<ApiResponse<LoginResponse>>() {}.type
                        val parsed: ApiResponse<LoginResponse>? =
                            if (!rawBody.isNullOrBlank()) gson.fromJson(rawBody, type) else null
                        val newSession = parsed?.data

                        if (newSession != null && newSession.accessToken.isNotBlank()) {
                            runBlocking { tokenManager.saveSession(newSession) }
                            response.request.newBuilder()
                                .header("Authorization", "Bearer ${newSession.accessToken}")
                                .header("X-Auth-Retried", "true")
                                .build()
                        } else {
                            null
                        }
                    } else if (refreshResponse.code in setOf(400, 401, 403)) {
                        // Refresh token expired, revoked, or rejected by server -> force clean sign-in
                        runBlocking { tokenManager.clearSession() }
                        SessionManager.notifySessionExpired("Your session has expired. Please sign in again.")
                        null
                    } else {
                        null
                    }
                }
            } catch (_: Exception) {
                // Transient network/cold-start failure: preserve refresh token and let caller show retryable error
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

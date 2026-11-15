package com.piyush.swasthaireportgenerator.data.repository

import com.piyush.swasthaireportgenerator.data.api.ApiErrorParser
import com.piyush.swasthaireportgenerator.data.api.AuthApiService
import com.piyush.swasthaireportgenerator.data.local.TokenManager
import com.piyush.swasthaireportgenerator.data.model.LoginRequest
import com.piyush.swasthaireportgenerator.data.model.LoginResponse
import com.piyush.swasthaireportgenerator.data.model.RefreshTokenRequest
import com.piyush.swasthaireportgenerator.data.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager
) {

    val userSession: Flow<UserSession?> = tokenManager.userSessionFlow

    fun isAccessTokenExpiredOrExpiring(token: String): Boolean {
        return tokenManager.isAccessTokenExpiredOrExpiring(token)
    }

    suspend fun login(email: String, pass: String): Result<LoginResponse> {
        return try {
            val response = authApi.login(LoginRequest(email = email.trim(), password = pass))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Invalid email or password")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to sign in")))
        }
    }

    /**
     * Attempts to refresh the JWT pair using the persisted refresh token.
     * Distinguishes hard auth rejection (400/401/403) from transient network/cold-start errors
     * by wrapping terminal auth failures cleanly.
     */
    suspend fun refreshSession(refreshToken: String): Result<LoginResponse> {
        return try {
            val response = authApi.refresh(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val msg = ApiErrorParser.extractMessage(response, "Session expired. Please sign in again.")
                Result.failure(SessionRejectedException(msg, response.code() in setOf(400, 401, 403)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to verify session")))
        }
    }

    suspend fun saveSession(loginResponse: LoginResponse) {
        tokenManager.saveSession(loginResponse)
    }

    suspend fun logoutFromServerAndClearSession() {
        try {
            val session = tokenManager.userSessionFlow.first()
            val refreshToken = session?.refreshToken
            if (!refreshToken.isNullOrBlank()) {
                authApi.logout(RefreshTokenRequest(refreshToken))
            }
        } catch (_: Exception) {
            // Proceed to clear local session even if network call fails
        } finally {
            tokenManager.clearSession()
        }
    }

    suspend fun clearSession() {
        tokenManager.clearSession()
    }
}

class SessionRejectedException(
    message: String,
    val isTerminalAuthFailure: Boolean
) : Exception(message)

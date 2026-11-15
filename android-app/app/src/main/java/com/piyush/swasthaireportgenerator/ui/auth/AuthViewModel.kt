package com.piyush.swasthaireportgenerator.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.swasthaireportgenerator.data.api.ApiClient
import com.piyush.swasthaireportgenerator.data.local.SessionManager
import com.piyush.swasthaireportgenerator.data.local.TokenManager
import com.piyush.swasthaireportgenerator.data.repository.AuthRepository
import com.piyush.swasthaireportgenerator.data.repository.SessionRejectedException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Success(val role: String, val name: String) : LoginUiState
    data class RoleRestricted(val message: String) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(
        ApiClient.getAuthApi(application),
        TokenManager(application)
    )

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _sessionExpiredEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sessionExpiredEvent: SharedFlow<String> = _sessionExpiredEvent.asSharedFlow()

    init {
        observeGlobalSessionExpiry()
        checkSession()
    }

    private fun observeGlobalSessionExpiry() {
        viewModelScope.launch {
            SessionManager.sessionExpired.collect { reason ->
                authRepository.clearSession()
                _uiState.value = LoginUiState.Error(reason)
                _sessionExpiredEvent.tryEmit(reason)
            }
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            val session = authRepository.userSession.first()
            if (session == null || session.accessToken.isBlank()) return@launch

            if (session.role.equals("SUPER_ADMIN", ignoreCase = true)) {
                authRepository.clearSession()
                return@launch
            }

            if (!authRepository.isAccessTokenExpiredOrExpiring(session.accessToken)) {
                _uiState.value = LoginUiState.Success(session.role, session.name)
                return@launch
            }

            // Stored access token is expired (>15m) or malformed -> attempt clean refresh before auto-login
            if (session.refreshToken.isBlank()) {
                authRepository.clearSession()
                return@launch
            }

            _uiState.value = LoginUiState.Loading
            val refreshResult = authRepository.refreshSession(session.refreshToken)
            refreshResult.onSuccess { fresh ->
                if (fresh.role.equals("SUPER_ADMIN", ignoreCase = true)) {
                    authRepository.clearSession()
                    _uiState.value = LoginUiState.Idle
                } else {
                    authRepository.saveSession(fresh)
                    _uiState.value = LoginUiState.Success(fresh.role, fresh.name)
                }
            }.onFailure { error ->
                val isTerminal = (error as? SessionRejectedException)?.isTerminalAuthFailure == true
                if (isTerminal) {
                    authRepository.clearSession()
                    _uiState.value = LoginUiState.Error("Your previous session expired. Please sign in again.")
                } else {
                    _uiState.value = LoginUiState.Error(
                        error.localizedMessage ?: "Unable to verify session. Please sign in or check server connection."
                    )
                }
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter both email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.login(email, pass)
            result.onSuccess { response ->
                // Check if Super Admin is attempting to log in
                if (response.role.equals("SUPER_ADMIN", ignoreCase = true)) {
                    authRepository.clearSession()
                    _uiState.value = LoginUiState.RoleRestricted(
                        "Super Admin operations are restricted to the Web Dashboard. This mobile app is dedicated to Laboratory Staff and Organization Administrators."
                    )
                } else {
                    authRepository.saveSession(response)
                    _uiState.value = LoginUiState.Success(response.role, response.name)
                }
            }.onFailure { error ->
                _uiState.value = LoginUiState.Error(error.localizedMessage ?: "Login failed. Check server connection.")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logoutFromServerAndClearSession()
            _uiState.value = LoginUiState.Idle
        }
    }
}

package com.piyush.swasthaireportgenerator.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Coordinates global authentication session events (such as unrecoverable 401 / refresh expiry)
 * between OkHttp's background TokenAuthenticator and the UI navigation shell.
 */
object SessionManager {

    private val _sessionExpired = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sessionExpired = _sessionExpired.asSharedFlow()

    fun notifySessionExpired(reason: String = "Your session has expired. Please sign in again.") {
        _sessionExpired.tryEmit(reason)
    }
}

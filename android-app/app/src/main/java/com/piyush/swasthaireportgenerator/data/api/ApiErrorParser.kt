package com.piyush.swasthaireportgenerator.data.api

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

private data class BackendErrorPayload(
    @SerializedName("message") val message: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("errors") val errors: Map<String, String>? = null
)

object ApiErrorParser {

    private val gson = Gson()

    fun extractMessage(response: Response<*>, fallback: String): String {
        val rawError = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        if (!rawError.isNullOrBlank()) {
            try {
                val payload = gson.fromJson(rawError, BackendErrorPayload::class.java)
                val fieldErrors = payload?.errors
                    ?.values
                    ?.filter { it.isNotBlank() }
                    ?.joinToString("\n")

                if (!fieldErrors.isNullOrBlank()) {
                    return fieldErrors
                }

                if (!payload?.message.isNullOrBlank()) {
                    val msg = payload!!.message!!
                    return if (response.code() == 401 && msg.equals("Authentication failed.", ignoreCase = true)) {
                        "Session expired or unauthorized (HTTP 401). Please sign in again."
                    } else {
                        msg
                    }
                }
            } catch (_: Exception) {
                // Ignore non-JSON error bodies and use fallback below
            }
        }

        return "$fallback (HTTP ${response.code()})"
    }

    fun humanizeException(throwable: Throwable, fallback: String): String {
        return when (throwable) {
            is SocketTimeoutException ->
                "Server took too long to respond (cloud instance may be waking up). Please try again."
            is UnknownHostException ->
                "Unable to reach server. Check your internet connection or server URL."
            is ConnectException ->
                "Connection refused by server. Verify the backend is running and server URL is correct."
            is SSLHandshakeException ->
                "Secure HTTPS connection to server failed."
            is JsonParseException ->
                "Unexpected response format received from server."
            else -> throwable.message?.takeIf { it.isNotBlank() } ?: fallback
        }
    }
}

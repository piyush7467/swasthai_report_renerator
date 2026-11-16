package com.piyush.swasthaireportgenerator.data.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

object ApiErrorParser {

    fun extractMessage(response: Response<*>, fallback: String): String {
        val rawError = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        if (!rawError.isNullOrBlank()) {
            try {
                val element: JsonElement = JsonParser.parseString(rawError)
                if (element.isJsonObject) {
                    val obj: JsonObject = element.asJsonObject

                    // 1. Check if "errors" object or array has messages
                    if (obj.has("errors") && !obj.get("errors").isJsonNull) {
                        val errorsElem = obj.get("errors")
                        if (errorsElem.isJsonObject) {
                            val messages = errorsElem.asJsonObject.entrySet()
                                .mapNotNull {
                                    try {
                                        it.value.asString?.takeIf { s -> s.isNotBlank() }
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                                .joinToString("\n")
                            if (messages.isNotBlank()) return messages
                        } else if (errorsElem.isJsonArray) {
                            val messages = errorsElem.asJsonArray
                                .mapNotNull { item ->
                                    try {
                                        if (item.isJsonPrimitive) item.asString
                                        else if (item.isJsonObject && item.asJsonObject.has("message")) {
                                            item.asJsonObject.get("message").asString
                                        } else null
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                                .filter { it.isNotBlank() }
                                .joinToString("\n")
                            if (messages.isNotBlank()) return messages
                        }
                    }

                    // 2. Check "message"
                    if (obj.has("message") && !obj.get("message").isJsonNull) {
                        val msg = try { obj.get("message").asString } catch (_: Exception) { null }
                        if (!msg.isNullOrBlank()) {
                            return if (response.code() == 401 && msg.equals("Authentication failed.", ignoreCase = true)) {
                                "Session expired or unauthorized (HTTP 401). Please sign in again."
                            } else {
                                msg
                            }
                        }
                    }

                    // 3. Check "detail" or "error"
                    if (obj.has("detail") && !obj.get("detail").isJsonNull) {
                        val detail = try { obj.get("detail").asString } catch (_: Exception) { null }
                        if (!detail.isNullOrBlank()) return detail
                    }
                    if (obj.has("error") && !obj.get("error").isJsonNull) {
                        val err = try { obj.get("error").asString } catch (_: Exception) { null }
                        if (!err.isNullOrBlank()) return err
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

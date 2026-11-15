package com.piyush.swasthaireportgenerator.data.local

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.piyush.swasthaireportgenerator.data.model.LoginResponse
import com.piyush.swasthaireportgenerator.data.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.nio.charset.StandardCharsets

private val Context.authDataStore by preferencesDataStore(name = "auth_session")

class TokenManager(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_REF_ID = stringPreferencesKey("user_ref_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_ORG_REF_ID = stringPreferencesKey("org_ref_id")
    }

    val accessTokenFlow: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[KEY_ACCESS_TOKEN]
    }

    val userSessionFlow: Flow<UserSession?> = context.authDataStore.data.map { preferences ->
        val token = preferences[KEY_ACCESS_TOKEN] ?: return@map null
        UserSession(
            accessToken = token,
            refreshToken = preferences[KEY_REFRESH_TOKEN] ?: "",
            userRefId = preferences[KEY_USER_REF_ID] ?: "",
            name = preferences[KEY_USER_NAME] ?: "",
            email = preferences[KEY_USER_EMAIL] ?: "",
            role = preferences[KEY_USER_ROLE] ?: "",
            organizationRefId = preferences[KEY_ORG_REF_ID]
        )
    }

    suspend fun getAccessToken(): String? {
        return context.authDataStore.data.first()[KEY_ACCESS_TOKEN]
    }

    suspend fun getRefreshToken(): String? {
        return context.authDataStore.data.first()[KEY_REFRESH_TOKEN]
    }

    fun isAccessTokenExpiredOrExpiring(token: String, skewSeconds: Long = 45L): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            val decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val json = JSONObject(String(decodedBytes, StandardCharsets.UTF_8))
            val exp = json.optLong("exp", 0L)
            if (exp <= 0L) return true
            val nowSec = System.currentTimeMillis() / 1000L
            exp <= (nowSec + skewSeconds)
        } catch (_: Exception) {
            true
        }
    }

    suspend fun saveSession(loginResponse: LoginResponse) {
        context.authDataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = loginResponse.accessToken
            preferences[KEY_REFRESH_TOKEN] = loginResponse.refreshToken
            preferences[KEY_USER_REF_ID] = loginResponse.userRefId
            preferences[KEY_USER_NAME] = loginResponse.name
            preferences[KEY_USER_EMAIL] = loginResponse.email
            preferences[KEY_USER_ROLE] = loginResponse.role
            loginResponse.organizationRefId?.let {
                preferences[KEY_ORG_REF_ID] = it
            }
        }
    }

    suspend fun clearSession() {
        context.authDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

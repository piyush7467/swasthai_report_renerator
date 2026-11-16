package com.piyush.swasthaireportgenerator.data.api

import android.content.Context
import com.piyush.swasthaireportgenerator.BuildConfig
import com.piyush.swasthaireportgenerator.data.interceptor.AuthInterceptor
import com.piyush.swasthaireportgenerator.data.interceptor.TokenAuthenticator
import com.piyush.swasthaireportgenerator.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val PRODUCTION_BASE_URL =
        "https://swasthaireportrenerator-production-2370.up.railway.app/api/v1/"

    const val LOCAL_EMULATOR_BASE_URL = "http://10.0.2.2:8087/api/v1/"

    private const val PREFS_NAME = "swasthai_server_prefs"
    private const val KEY_BASE_URL = "server_base_url"

    @Volatile
    private var currentBaseUrl: String = PRODUCTION_BASE_URL

    @Volatile
    private var retrofitInstance: Retrofit? = null

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_BASE_URL, null)
        val normalized = saved?.let { normalizeApiBaseUrl(it) } ?: PRODUCTION_BASE_URL
        currentBaseUrl = normalized
    }

    fun setBaseUrl(url: String): Boolean {
        val normalized = normalizeApiBaseUrl(url) ?: return false
        currentBaseUrl = normalized
        return true
    }

    fun setBaseUrl(context: Context, url: String, clearSessionOnSwitch: Boolean = true): Boolean {
        val normalized = normalizeApiBaseUrl(url) ?: return false
        val changed = !normalized.equals(currentBaseUrl, ignoreCase = true)
        currentBaseUrl = normalized

        val appCtx = context.applicationContext
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, normalized)
            .apply()

        if (changed && clearSessionOnSwitch) {
            runBlocking { TokenManager(appCtx).clearSession() }
        }
        return true
    }

    fun getBaseUrl(): String = currentBaseUrl

    fun isProductionUrl(): Boolean = currentBaseUrl.equals(PRODUCTION_BASE_URL, ignoreCase = true)

    fun normalizeApiBaseUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim().trimEnd('/')
        if (trimmed.isBlank()) return null

        val withVersion = if (trimmed.endsWith("/api/v1")) {
            "$trimmed/"
        } else {
            "$trimmed/api/v1/"
        }

        return withVersion.toHttpUrlOrNull()?.toString()
    }

    private val dynamicBaseUrlInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val targetBase = currentBaseUrl.toHttpUrlOrNull()

        if (targetBase != null && currentBaseUrl != PRODUCTION_BASE_URL) {
            val defaultBase = PRODUCTION_BASE_URL.toHttpUrl()
            val relativePath = originalRequest.url.encodedPath
                .removePrefix(defaultBase.encodedPath)
                .trimStart('/')

            val updatedUrl = targetBase.newBuilder()
                .apply {
                    if (relativePath.isNotEmpty()) {
                        addEncodedPathSegments(relativePath)
                    }
                }
                .encodedQuery(originalRequest.url.encodedQuery)
                .build()

            val updatedRequest = originalRequest.newBuilder()
                .url(updatedUrl)
                .build()

            return@Interceptor chain.proceed(updatedRequest)
        }

        chain.proceed(originalRequest)
    }

    private fun createOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(AuthInterceptor(tokenManager))
            .authenticator(TokenAuthenticator(tokenManager) { getBaseUrl() })
            .addInterceptor(logging)
            // 90s accommodates Azure App Service cold starts without premature timeouts
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    private fun getRetrofit(context: Context): Retrofit {
        return retrofitInstance ?: synchronized(this) {
            retrofitInstance ?: run {
                val appCtx = context.applicationContext
                init(appCtx)
                val tokenManager = TokenManager(appCtx)
                Retrofit.Builder()
                    .baseUrl(PRODUCTION_BASE_URL)
                    .client(createOkHttpClient(tokenManager))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .also { retrofitInstance = it }
            }
        }
    }

    fun getAuthApi(context: Context): AuthApiService =
        getRetrofit(context).create(AuthApiService::class.java)

    fun getPatientApi(context: Context): PatientApiService =
        getRetrofit(context).create(PatientApiService::class.java)

    fun getReportApi(context: Context): ReportApiService =
        getRetrofit(context).create(ReportApiService::class.java)

    fun getAnalyticsApi(context: Context): AnalyticsApiService =
        getRetrofit(context).create(AnalyticsApiService::class.java)

    fun getLabStaffApi(context: Context): LabStaffApiService =
        getRetrofit(context).create(LabStaffApiService::class.java)

    fun getLicenseApi(context: Context): LicenseApiService =
        getRetrofit(context).create(LicenseApiService::class.java)
}

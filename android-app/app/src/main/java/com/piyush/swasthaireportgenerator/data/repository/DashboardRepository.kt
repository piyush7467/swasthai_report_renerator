package com.piyush.swasthaireportgenerator.data.repository

import com.piyush.swasthaireportgenerator.data.api.AnalyticsApiService
import com.piyush.swasthaireportgenerator.data.api.ApiErrorParser
import com.piyush.swasthaireportgenerator.data.model.CategoryUsageStatsResponse
import com.piyush.swasthaireportgenerator.data.model.OrgActivityItemResponse
import com.piyush.swasthaireportgenerator.data.model.OrgOverviewStatsResponse
import com.piyush.swasthaireportgenerator.data.model.OrganizationProfileResponse
import com.piyush.swasthaireportgenerator.data.model.TestParameterResponse
import com.piyush.swasthaireportgenerator.data.model.UpdateOrganizationProfileRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class DashboardRepository(private val analyticsApi: AnalyticsApiService) {

    suspend fun getOverviewStats(): Result<OrgOverviewStatsResponse> {
        return try {
            val response = analyticsApi.getOverviewStats()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load dashboard overview")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getRecentActivity(): Result<List<OrgActivityItemResponse>> {
        return try {
            val response = analyticsApi.getRecentActivity()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load recent activity")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getCategoryUsage(): Result<List<CategoryUsageStatsResponse>> {
        return try {
            val response = analyticsApi.getCategoryUsage()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load category usage")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getOrganizationProfile(): Result<OrganizationProfileResponse> {
        return try {
            val response = analyticsApi.getMyOrganizationProfile()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load organization profile")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun updateOrganizationProfile(request: UpdateOrganizationProfileRequest): Result<OrganizationProfileResponse> {
        return try {
            val response = analyticsApi.updateMyOrganizationProfile(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to save organization profile")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getLogoBytes(): ByteArray? {
        return try {
            val response = analyticsApi.getMyLogo()
            if (response.isSuccessful) response.body()?.bytes() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun uploadLogo(fileName: String, mimeType: String, bytes: ByteArray): Result<OrganizationProfileResponse> {
        return try {
            val part = MultipartBody.Part.createFormData(
                "file",
                fileName,
                bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            val response = analyticsApi.uploadMyLogo(part)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to upload logo")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun deleteLogo(): Result<OrganizationProfileResponse> {
        return try {
            val response = analyticsApi.deleteMyLogo()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to remove logo")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getSignatureBytes(): ByteArray? {
        return try {
            val response = analyticsApi.getMySignature()
            if (response.isSuccessful) response.body()?.bytes() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun uploadSignature(fileName: String, mimeType: String, bytes: ByteArray): Result<OrganizationProfileResponse> {
        return try {
            val part = MultipartBody.Part.createFormData(
                "file",
                fileName,
                bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            val response = analyticsApi.uploadMySignature(part)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to upload signature")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun deleteSignature(): Result<OrganizationProfileResponse> {
        return try {
            val response = analyticsApi.deleteMySignature()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to remove signature")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getTestParameters(testRefId: String): Result<List<TestParameterResponse>> {
        return try {
            val response = analyticsApi.getTestParameters(testRefId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.content)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load test parameters")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }
}

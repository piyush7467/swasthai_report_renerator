package com.piyush.swasthaireportgenerator.data.repository

import com.piyush.swasthaireportgenerator.data.api.ApiErrorParser
import com.piyush.swasthaireportgenerator.data.api.LicenseApiService
import com.piyush.swasthaireportgenerator.data.model.AvailablePlanResponse
import com.piyush.swasthaireportgenerator.data.model.CreateUpgradeRequest
import com.piyush.swasthaireportgenerator.data.model.LicenseResponse
import com.piyush.swasthaireportgenerator.data.model.OrganizationLicenseOverviewResponse
import com.piyush.swasthaireportgenerator.data.model.PlanUpgradeRequestResponse

class LicenseRepository(private val licenseApi: LicenseApiService) {

    suspend fun getOwnLicense(): Result<LicenseResponse> {
        return try {
            val response = licenseApi.getOwnLicense()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load active license")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getOverview(): Result<OrganizationLicenseOverviewResponse> {
        return try {
            val response = licenseApi.getOrganizationLicenseOverview()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load subscription overview")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getAvailablePlans(): Result<List<AvailablePlanResponse>> {
        return try {
            val response = licenseApi.getAvailablePlansForUpgrade()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load subscription plans")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getMyUpgradeRequests(): Result<List<PlanUpgradeRequestResponse>> {
        return try {
            val response = licenseApi.getMyUpgradeRequests()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.content)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load upgrade requests")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun createUpgradeRequest(request: CreateUpgradeRequest): Result<PlanUpgradeRequestResponse> {
        return try {
            val response = licenseApi.createUpgradeRequest(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to submit upgrade request")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun cancelUpgradeRequest(refId: String): Result<PlanUpgradeRequestResponse> {
        return try {
            val response = licenseApi.cancelUpgradeRequest(refId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to cancel upgrade request")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }
}

package com.piyush.swasthaireportgenerator.data.repository

import com.piyush.swasthaireportgenerator.data.api.ApiErrorParser
import com.piyush.swasthaireportgenerator.data.api.LabStaffApiService
import com.piyush.swasthaireportgenerator.data.model.CreateLabStaffRequest
import com.piyush.swasthaireportgenerator.data.model.LabStaffDetailsResponse
import com.piyush.swasthaireportgenerator.data.model.LabStaffResponse
import com.piyush.swasthaireportgenerator.data.model.LabStaffSummaryResponse
import com.piyush.swasthaireportgenerator.data.model.UpdateUserStatusRequest

class LabStaffRepository(private val labStaffApi: LabStaffApiService) {

    suspend fun getSummary(): Result<LabStaffSummaryResponse> {
        return try {
            val response = labStaffApi.getSummary()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load staff quota summary")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getStaffList(status: String? = null, search: String? = null): Result<List<LabStaffResponse>> {
        return try {
            val response = labStaffApi.getStaffList(
                status = status,
                search = search?.trim()?.ifBlank { null }
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.content)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load lab staff list")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getStaffDetails(refId: String): Result<LabStaffDetailsResponse> {
        return try {
            val response = labStaffApi.getStaffDetails(refId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to load staff details")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun createStaff(request: CreateLabStaffRequest): Result<LabStaffResponse> {
        return try {
            val response = labStaffApi.createStaff(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to create lab staff account")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun updateStaffStatus(refId: String, status: String): Result<LabStaffResponse> {
        return try {
            val response = labStaffApi.updateStaffStatus(refId, UpdateUserStatusRequest(status))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to update staff status")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun deleteStaff(refId: String): Result<Unit> {
        return try {
            val response = labStaffApi.deleteStaff(refId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to delete staff member")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }
}

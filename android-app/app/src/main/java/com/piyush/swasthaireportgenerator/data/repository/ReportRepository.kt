package com.piyush.swasthaireportgenerator.data.repository

import com.piyush.swasthaireportgenerator.data.api.ApiErrorParser
import com.piyush.swasthaireportgenerator.data.api.ReportApiService
import com.piyush.swasthaireportgenerator.data.model.AddReportTestsBulkRequest
import com.piyush.swasthaireportgenerator.data.model.CreateReportRequest
import com.piyush.swasthaireportgenerator.data.model.CreateReportShareRequest
import com.piyush.swasthaireportgenerator.data.model.OrganizationTestResponse
import com.piyush.swasthaireportgenerator.data.model.ParameterValueUpdate
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.data.model.ReportShareResponse
import com.piyush.swasthaireportgenerator.data.model.UpdateReportHeaderOptionRequest
import com.piyush.swasthaireportgenerator.data.model.UpdateReportParametersRequest

class ReportRepository(private val reportApi: ReportApiService) {

    suspend fun getMyOrganizationTests(): Result<List<OrganizationTestResponse>> {
        return try {
            val response = reportApi.getMyOrganizationTests(status = "ACTIVE", page = 0, size = 100)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.content)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to load organization tests")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getMyReports(status: String? = null): Result<List<ReportResponse>> {
        return try {
            val response = reportApi.getMyReports(status = status, page = 0, size = 50)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.content)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to load reports")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getReport(reportRefId: String): Result<ReportResponse> {
        return try {
            val response = reportApi.getReport(reportRefId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to retrieve report")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun createReportWithTests(
        patientRefId: String,
        testRefIds: List<String>,
        includeOrganizationHeader: Boolean = true
    ): Result<ReportResponse> {
        return try {
            val createResponse = reportApi.createReport(
                CreateReportRequest(
                    patientRefId = patientRefId,
                    includeOrganizationHeader = includeOrganizationHeader
                )
            )
            if (!createResponse.isSuccessful || createResponse.body()?.data == null) {
                val errorMsg = ApiErrorParser.extractMessage(createResponse, "Failed to create report draft")
                return Result.failure(Exception(errorMsg))
            }

            val createdDraft = createResponse.body()!!.data!!

            if (testRefIds.isEmpty()) {
                return Result.success(createdDraft)
            }

            val bulkResponse = reportApi.addTestsBulk(
                reportRefId = createdDraft.refId,
                request = AddReportTestsBulkRequest(
                    testRefIds = testRefIds,
                    lockVersion = createdDraft.lockVersion
                )
            )
            if (bulkResponse.isSuccessful && bulkResponse.body()?.data != null) {
                Result.success(bulkResponse.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(bulkResponse, "Report draft created, but failed to attach tests")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun addTestsBulk(
        reportRefId: String,
        testRefIds: List<String>,
        lockVersion: Long?
    ): Result<ReportResponse> {
        return try {
            val response = reportApi.addTestsBulk(
                reportRefId = reportRefId,
                request = AddReportTestsBulkRequest(
                    testRefIds = testRefIds,
                    lockVersion = lockVersion
                )
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to add tests to report")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun removeTest(reportRefId: String, reportTestRefId: String): Result<ReportResponse> {
        return try {
            val response = reportApi.removeTest(reportRefId, reportTestRefId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to remove test")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun updateParameters(
        reportRefId: String,
        reportTestRefId: String,
        lockVersion: Long?,
        updates: List<ParameterValueUpdate>
    ): Result<ReportResponse> {
        return try {
            val response = reportApi.updateParameters(
                reportRefId = reportRefId,
                reportTestRefId = reportTestRefId,
                request = UpdateReportParametersRequest(
                    lockVersion = lockVersion,
                    parameters = updates
                )
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to update parameters")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun recalculate(reportRefId: String): Result<ReportResponse> {
        return try {
            val response = reportApi.recalculate(reportRefId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to recalculate parameters")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun updateHeaderOption(
        reportRefId: String,
        includeOrganizationHeader: Boolean
    ): Result<ReportResponse> {
        return try {
            val response = reportApi.updateHeaderOption(
                reportRefId = reportRefId,
                request = UpdateReportHeaderOptionRequest(includeOrganizationHeader)
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to update header option")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun finalizeReport(reportRefId: String): Result<ReportResponse> {
        return try {
            val response = reportApi.finalizeReport(reportRefId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to finalize report")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun deleteReport(reportRefId: String): Result<Unit> {
        return try {
            val response = reportApi.deleteReport(reportRefId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to delete report")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun downloadPdfBytes(reportRefId: String, includeOrganizationHeader: Boolean?): Result<ByteArray> {
        return try {
            val response = reportApi.downloadReportPdf(reportRefId, includeOrganizationHeader)
            val bytes = response.body()?.bytes()
            if (response.isSuccessful && bytes != null && bytes.isNotEmpty()) {
                Result.success(bytes)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to generate PDF")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getQrBytes(reportRefId: String): ByteArray? {
        return try {
            val response = reportApi.getReportQrCode(reportRefId)
            if (response.isSuccessful) response.body()?.bytes() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun createShareLink(reportRefId: String, channel: String = "LINK", expiresInDays: Int = 7): Result<ReportShareResponse> {
        return try {
            val response = reportApi.createReportShare(
                reportRefId,
                CreateReportShareRequest(channel = channel, recipient = null, expiresInDays = expiresInDays)
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(ApiErrorParser.extractMessage(response, "Failed to generate share link")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }
}

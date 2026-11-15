package com.piyush.swasthaireportgenerator.data.repository

import com.piyush.swasthaireportgenerator.data.api.ApiErrorParser
import com.piyush.swasthaireportgenerator.data.api.PatientApiService
import com.piyush.swasthaireportgenerator.data.model.CreatePatientRequest
import com.piyush.swasthaireportgenerator.data.model.PatientResponse
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.data.model.UpdatePatientRequest

class PatientRepository(private val patientApi: PatientApiService) {

    suspend fun getPatients(search: String? = null): Result<List<PatientResponse>> {
        return try {
            val response = patientApi.getPatients(search = search)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.content)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to load patients")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getPatient(patientRefId: String): Result<PatientResponse> {
        return try {
            val response = patientApi.getPatient(patientRefId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to retrieve patient")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun createPatient(request: CreatePatientRequest): Result<PatientResponse> {
        return try {
            val response = patientApi.createPatient(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to create patient")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun updatePatient(patientRefId: String, request: UpdatePatientRequest): Result<PatientResponse> {
        return try {
            val response = patientApi.updatePatient(patientRefId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to update patient")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun deletePatient(patientRefId: String): Result<Unit> {
        return try {
            val response = patientApi.deletePatient(patientRefId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to delete patient")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }

    suspend fun getPatientReports(patientRefId: String): Result<List<ReportResponse>> {
        return try {
            val response = patientApi.getPatientReports(patientRefId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.content)
            } else {
                val errorMsg = ApiErrorParser.extractMessage(response, "Failed to load patient reports")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.humanizeException(e, "Unable to reach server")))
        }
    }
}

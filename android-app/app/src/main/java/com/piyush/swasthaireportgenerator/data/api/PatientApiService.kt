package com.piyush.swasthaireportgenerator.data.api

import com.piyush.swasthaireportgenerator.data.model.ApiResponse
import com.piyush.swasthaireportgenerator.data.model.CreatePatientRequest
import com.piyush.swasthaireportgenerator.data.model.PagedResponse
import com.piyush.swasthaireportgenerator.data.model.PatientResponse
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.data.model.UpdatePatientRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PatientApiService {

    @GET("patients")
    suspend fun getPatients(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("search") search: String? = null
    ): Response<ApiResponse<PagedResponse<PatientResponse>>>

    @POST("patients")
    suspend fun createPatient(
        @Body request: CreatePatientRequest
    ): Response<ApiResponse<PatientResponse>>

    @GET("patients/{patientRefId}")
    suspend fun getPatient(
        @Path("patientRefId") patientRefId: String
    ): Response<ApiResponse<PatientResponse>>

    @PUT("patients/{patientRefId}")
    suspend fun updatePatient(
        @Path("patientRefId") patientRefId: String,
        @Body request: UpdatePatientRequest
    ): Response<ApiResponse<PatientResponse>>

    @DELETE("patients/{patientRefId}")
    suspend fun deletePatient(
        @Path("patientRefId") patientRefId: String
    ): Response<ApiResponse<Void>>

    @GET("patients/{patientRefId}/reports")
    suspend fun getPatientReports(
        @Path("patientRefId") patientRefId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): Response<ApiResponse<PagedResponse<ReportResponse>>>
}

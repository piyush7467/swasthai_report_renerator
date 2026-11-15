package com.piyush.swasthaireportgenerator.data.api

import com.piyush.swasthaireportgenerator.data.model.ApiResponse
import com.piyush.swasthaireportgenerator.data.model.CreateLabStaffRequest
import com.piyush.swasthaireportgenerator.data.model.LabStaffDetailsResponse
import com.piyush.swasthaireportgenerator.data.model.LabStaffResponse
import com.piyush.swasthaireportgenerator.data.model.LabStaffSummaryResponse
import com.piyush.swasthaireportgenerator.data.model.PagedResponse
import com.piyush.swasthaireportgenerator.data.model.UpdateUserStatusRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LabStaffApiService {

    @GET("users/lab-staff/summary")
    suspend fun getSummary(): Response<ApiResponse<LabStaffSummaryResponse>>

    @GET("users/lab-staff")
    suspend fun getStaffList(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("sortDirection") sortDirection: String = "DESC",
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Response<ApiResponse<PagedResponse<LabStaffResponse>>>

    @GET("users/lab-staff/{refId}")
    suspend fun getStaffDetails(
        @Path("refId") refId: String
    ): Response<ApiResponse<LabStaffDetailsResponse>>

    @POST("users/lab-staff")
    suspend fun createStaff(
        @Body request: CreateLabStaffRequest
    ): Response<ApiResponse<LabStaffResponse>>

    @PATCH("users/lab-staff/{refId}/status")
    suspend fun updateStaffStatus(
        @Path("refId") refId: String,
        @Body request: UpdateUserStatusRequest
    ): Response<ApiResponse<LabStaffResponse>>

    @PATCH("users/lab-staff/{refId}/deactivate")
    suspend fun deactivateStaff(
        @Path("refId") refId: String
    ): Response<ApiResponse<LabStaffResponse>>

    @DELETE("users/lab-staff/{refId}")
    suspend fun deleteStaff(
        @Path("refId") refId: String
    ): Response<ApiResponse<Void>>
}

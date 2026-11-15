package com.piyush.swasthaireportgenerator.data.api

import com.piyush.swasthaireportgenerator.data.model.AvailablePlanResponse
import com.piyush.swasthaireportgenerator.data.model.CreateUpgradeRequest
import com.piyush.swasthaireportgenerator.data.model.LicenseResponse
import com.piyush.swasthaireportgenerator.data.model.OrganizationLicenseOverviewResponse
import com.piyush.swasthaireportgenerator.data.model.PagedResponse
import com.piyush.swasthaireportgenerator.data.model.PlanUpgradeRequestResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LicenseApiService {

    @GET("license/me")
    suspend fun getOwnLicense(): Response<LicenseResponse>

    @GET("license/overview")
    suspend fun getOrganizationLicenseOverview(): Response<OrganizationLicenseOverviewResponse>

    @GET("license/available-plans")
    suspend fun getAvailablePlansForUpgrade(): Response<List<AvailablePlanResponse>>

    @POST("license/upgrade-requests")
    suspend fun createUpgradeRequest(
        @Body request: CreateUpgradeRequest
    ): Response<PlanUpgradeRequestResponse>

    @GET("license/upgrade-requests/my")
    suspend fun getMyUpgradeRequests(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("status") status: String? = null
    ): Response<PagedResponse<PlanUpgradeRequestResponse>>

    @POST("license/upgrade-requests/{refId}/cancel")
    suspend fun cancelUpgradeRequest(
        @Path("refId") refId: String
    ): Response<PlanUpgradeRequestResponse>
}

package com.piyush.swasthaireportgenerator.data.api

import com.piyush.swasthaireportgenerator.data.model.ApiResponse
import com.piyush.swasthaireportgenerator.data.model.CategoryUsageStatsResponse
import com.piyush.swasthaireportgenerator.data.model.OrgActivityItemResponse
import com.piyush.swasthaireportgenerator.data.model.OrgOverviewStatsResponse
import com.piyush.swasthaireportgenerator.data.model.OrganizationProfileResponse
import com.piyush.swasthaireportgenerator.data.model.PagedResponse
import com.piyush.swasthaireportgenerator.data.model.TestParameterResponse
import com.piyush.swasthaireportgenerator.data.model.UpdateOrganizationProfileRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface AnalyticsApiService {

    @GET("organization/analytics/overview")
    suspend fun getOverviewStats(): Response<ApiResponse<OrgOverviewStatsResponse>>

    @GET("organization/analytics/activity")
    suspend fun getRecentActivity(): Response<ApiResponse<List<OrgActivityItemResponse>>>

    @GET("organization/analytics/tests/category-usage")
    suspend fun getCategoryUsage(): Response<ApiResponse<List<CategoryUsageStatsResponse>>>

    @GET("organization-profile/me")
    suspend fun getMyOrganizationProfile(): Response<ApiResponse<OrganizationProfileResponse>>

    @PUT("organization-profile/me")
    suspend fun updateMyOrganizationProfile(
        @Body request: UpdateOrganizationProfileRequest
    ): Response<ApiResponse<OrganizationProfileResponse>>

    @GET("organization-profile/me/logo")
    suspend fun getMyLogo(): Response<ResponseBody>

    @Multipart
    @POST("organization-profile/me/logo")
    suspend fun uploadMyLogo(
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<OrganizationProfileResponse>>

    @DELETE("organization-profile/me/logo")
    suspend fun deleteMyLogo(): Response<ApiResponse<OrganizationProfileResponse>>

    @GET("organization-profile/me/signature")
    suspend fun getMySignature(): Response<ResponseBody>

    @Multipart
    @POST("organization-profile/me/signature")
    suspend fun uploadMySignature(
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<OrganizationProfileResponse>>

    @DELETE("organization-profile/me/signature")
    suspend fun deleteMySignature(): Response<ApiResponse<OrganizationProfileResponse>>

    @GET("tests/{testRefId}/parameters")
    suspend fun getTestParameters(
        @Path("testRefId") testRefId: String,
        @Query("status") status: String? = "ACTIVE",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<ApiResponse<PagedResponse<TestParameterResponse>>>
}

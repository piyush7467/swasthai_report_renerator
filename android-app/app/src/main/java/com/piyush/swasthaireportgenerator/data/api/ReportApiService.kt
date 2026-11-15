package com.piyush.swasthaireportgenerator.data.api

import com.piyush.swasthaireportgenerator.data.model.AddReportTestsBulkRequest
import com.piyush.swasthaireportgenerator.data.model.ApiResponse
import com.piyush.swasthaireportgenerator.data.model.CreateReportRequest
import com.piyush.swasthaireportgenerator.data.model.CreateReportShareRequest
import com.piyush.swasthaireportgenerator.data.model.OrganizationTestResponse
import com.piyush.swasthaireportgenerator.data.model.PagedResponse
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.data.model.ReportShareResponse
import com.piyush.swasthaireportgenerator.data.model.UpdateReportHeaderOptionRequest
import com.piyush.swasthaireportgenerator.data.model.UpdateReportParametersRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReportApiService {

    @GET("organization-tests/my")
    suspend fun getMyOrganizationTests(
        @Query("status") status: String? = "ACTIVE",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<ApiResponse<PagedResponse<OrganizationTestResponse>>>

    @GET("reports/my")
    suspend fun getMyReports(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PagedResponse<ReportResponse>>>

    @POST("reports")
    suspend fun createReport(
        @Body request: CreateReportRequest
    ): Response<ApiResponse<ReportResponse>>

    @GET("reports/{reportRefId}")
    suspend fun getReport(
        @Path("reportRefId") reportRefId: String
    ): Response<ApiResponse<ReportResponse>>

    @POST("reports/{reportRefId}/tests/bulk")
    suspend fun addTestsBulk(
        @Path("reportRefId") reportRefId: String,
        @Body request: AddReportTestsBulkRequest
    ): Response<ApiResponse<ReportResponse>>

    @DELETE("reports/{reportRefId}/tests/{reportTestRefId}")
    suspend fun removeTest(
        @Path("reportRefId") reportRefId: String,
        @Path("reportTestRefId") reportTestRefId: String
    ): Response<ApiResponse<ReportResponse>>

    @PATCH("reports/{reportRefId}/tests/{reportTestRefId}/parameters")
    suspend fun updateParameters(
        @Path("reportRefId") reportRefId: String,
        @Path("reportTestRefId") reportTestRefId: String,
        @Body request: UpdateReportParametersRequest
    ): Response<ApiResponse<ReportResponse>>

    @POST("reports/{reportRefId}/recalculate")
    suspend fun recalculate(
        @Path("reportRefId") reportRefId: String
    ): Response<ApiResponse<ReportResponse>>

    @PATCH("reports/{reportRefId}/header-option")
    suspend fun updateHeaderOption(
        @Path("reportRefId") reportRefId: String,
        @Body request: UpdateReportHeaderOptionRequest
    ): Response<ApiResponse<ReportResponse>>

    @POST("reports/{reportRefId}/finalize")
    suspend fun finalizeReport(
        @Path("reportRefId") reportRefId: String
    ): Response<ApiResponse<ReportResponse>>

    @DELETE("reports/{reportRefId}")
    suspend fun deleteReport(
        @Path("reportRefId") reportRefId: String
    ): Response<ApiResponse<Any>>

    @GET("reports/{reportRefId}/pdf")
    suspend fun downloadReportPdf(
        @Path("reportRefId") reportRefId: String,
        @Query("includeOrganizationHeader") includeOrganizationHeader: Boolean? = null
    ): Response<ResponseBody>

    @GET("reports/{reportRefId}/qr")
    suspend fun getReportQrCode(
        @Path("reportRefId") reportRefId: String
    ): Response<ResponseBody>

    @POST("reports/{reportRefId}/shares")
    suspend fun createReportShare(
        @Path("reportRefId") reportRefId: String,
        @Body request: CreateReportShareRequest
    ): Response<ApiResponse<ReportShareResponse>>
}

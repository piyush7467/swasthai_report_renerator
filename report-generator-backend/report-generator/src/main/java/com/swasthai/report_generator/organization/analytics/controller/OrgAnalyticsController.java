package com.swasthai.report_generator.organization.analytics.controller;

import com.swasthai.report_generator.admin.analytics.dto.ReportTrendPointResponse;
import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.organization.analytics.dto.CategoryUsageStatsResponse;
import com.swasthai.report_generator.organization.analytics.dto.OrgActivityItemResponse;
import com.swasthai.report_generator.organization.analytics.dto.OrgOverviewStatsResponse;
import com.swasthai.report_generator.organization.analytics.dto.PatientTrendPointResponse;
import com.swasthai.report_generator.organization.analytics.service.OrgAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organization/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ORG_ADMIN', 'LAB_STAFF')")
public class OrgAnalyticsController {

    private final OrgAnalyticsService orgAnalyticsService;

    @GetMapping("/overview")
    public ApiResponse<OrgOverviewStatsResponse> getOverviewStats() {
        return ApiResponse.success(
                "Organization overview statistics retrieved successfully",
                orgAnalyticsService.getOverviewStats()
        );
    }

    @GetMapping("/reports/trend")
    public ApiResponse<List<ReportTrendPointResponse>> getReportTrend(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ApiResponse.success(
                "Report trend retrieved successfully",
                orgAnalyticsService.getReportTrend(days, from, to)
        );
    }

    @GetMapping("/tests/category-usage")
    public ApiResponse<List<CategoryUsageStatsResponse>> getCategoryUsage() {
        return ApiResponse.success(
                "Test category usage statistics retrieved successfully",
                orgAnalyticsService.getCategoryUsage()
        );
    }

    @GetMapping("/patients/trend")
    public ApiResponse<List<PatientTrendPointResponse>> getPatientTrend(
            @RequestParam(required = false) Integer days) {

        return ApiResponse.success(
                "Patient registration trend retrieved successfully",
                orgAnalyticsService.getPatientTrend(days)
        );
    }

    @GetMapping("/activity")
    public ApiResponse<List<OrgActivityItemResponse>> getRecentActivity() {
        return ApiResponse.success(
                "Recent laboratory activity retrieved successfully",
                orgAnalyticsService.getRecentActivity()
        );
    }
}

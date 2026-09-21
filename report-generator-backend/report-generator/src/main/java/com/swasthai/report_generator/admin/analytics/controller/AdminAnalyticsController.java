package com.swasthai.report_generator.admin.analytics.controller;

import com.swasthai.report_generator.admin.analytics.dto.AdminOverviewStatsResponse;
import com.swasthai.report_generator.admin.analytics.dto.OrganizationReportActivityResponse;
import com.swasthai.report_generator.admin.analytics.dto.ReportTrendPointResponse;
import com.swasthai.report_generator.admin.analytics.dto.TestUsageStatsResponse;
import com.swasthai.report_generator.admin.analytics.service.AdminAnalyticsService;
import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewStatsResponse> getOverviewStats() {
        return ApiResponse.success(
                "Overview statistics retrieved successfully",
                adminAnalyticsService.getOverviewStats()
        );
    }

    @GetMapping("/reports/trend")
    public ApiResponse<List<ReportTrendPointResponse>> getReportTrend(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String organizationRefId) {

        return ApiResponse.success(
                "Report trend retrieved successfully",
                adminAnalyticsService.getReportTrend(days, from, to, organizationRefId)
        );
    }

    @GetMapping("/reports/organizations")
    public ApiResponse<PageResponse<OrganizationReportActivityResponse>> getOrganizationReportActivity(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "totalReports") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        return ApiResponse.success(
                "Organization report activity retrieved successfully",
                adminAnalyticsService.getOrganizationReportActivity(search, page, size, sort, direction)
        );
    }

    @GetMapping("/tests/usage")
    public ApiResponse<List<TestUsageStatsResponse>> getTestUsage(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String organizationRefId) {

        return ApiResponse.success(
                "Test usage statistics retrieved successfully",
                adminAnalyticsService.getTestUsage(limit, organizationRefId)
        );
    }
}

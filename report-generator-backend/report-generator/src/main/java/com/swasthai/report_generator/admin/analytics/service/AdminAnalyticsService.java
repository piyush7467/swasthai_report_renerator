package com.swasthai.report_generator.admin.analytics.service;

import com.swasthai.report_generator.admin.analytics.dto.AdminOverviewStatsResponse;
import com.swasthai.report_generator.admin.analytics.dto.OrganizationReportActivityResponse;
import com.swasthai.report_generator.admin.analytics.dto.ReportTrendPointResponse;
import com.swasthai.report_generator.admin.analytics.dto.TestUsageStatsResponse;
import com.swasthai.report_generator.common.response.PageResponse;

import java.time.LocalDate;
import java.util.List;

public interface AdminAnalyticsService {

    AdminOverviewStatsResponse getOverviewStats();

    List<ReportTrendPointResponse> getReportTrend(Integer days, LocalDate from, LocalDate to, String organizationRefId);

    PageResponse<OrganizationReportActivityResponse> getOrganizationReportActivity(
            String search,
            int page,
            int size,
            String sort,
            String direction
    );

    List<TestUsageStatsResponse> getTestUsage(Integer limit, String organizationRefId);
}

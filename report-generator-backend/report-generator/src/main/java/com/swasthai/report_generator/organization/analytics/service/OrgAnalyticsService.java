package com.swasthai.report_generator.organization.analytics.service;

import com.swasthai.report_generator.admin.analytics.dto.ReportTrendPointResponse;
import com.swasthai.report_generator.organization.analytics.dto.CategoryUsageStatsResponse;
import com.swasthai.report_generator.organization.analytics.dto.OrgActivityItemResponse;
import com.swasthai.report_generator.organization.analytics.dto.OrgOverviewStatsResponse;
import com.swasthai.report_generator.organization.analytics.dto.PatientTrendPointResponse;

import java.time.LocalDate;
import java.util.List;

public interface OrgAnalyticsService {

    OrgOverviewStatsResponse getOverviewStats();

    List<ReportTrendPointResponse> getReportTrend(Integer days, LocalDate from, LocalDate to);

    List<CategoryUsageStatsResponse> getCategoryUsage();

    List<PatientTrendPointResponse> getPatientTrend(Integer days);

    List<OrgActivityItemResponse> getRecentActivity();
}

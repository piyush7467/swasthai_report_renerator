package com.swasthai.report_generator.admin.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewStatsResponse {

    private long totalReports;
    private long reportsToday;
    private long reportsThisWeek;
    private long reportsThisMonth;

    private long draftReports;
    private long calculatedReports;
    private long finalizedReports;

    private long deletedReportsToday;
    private long deletedReportsThisMonth;
    private long totalDeletedReports;

    private long activeOrganizationsCount;
    private long organizationsWithReportsCount;

    private long breakGlassAccessCount30Days;
}

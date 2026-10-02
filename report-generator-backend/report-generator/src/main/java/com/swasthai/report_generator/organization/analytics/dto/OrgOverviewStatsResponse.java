package com.swasthai.report_generator.organization.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgOverviewStatsResponse {
    private long totalReports;
    private long finalizedReports;
    private long draftReports;
    private long cancelledReports;
    private long pendingReviewReports;
    private long totalPatients;
    private long activeTests;
    private long reportsToday;
    private Double reportsGrowthPercentage;
    private Double finalizedGrowthPercentage;
    private Double draftsGrowthPercentage;
    private Double patientsGrowthPercentage;
}

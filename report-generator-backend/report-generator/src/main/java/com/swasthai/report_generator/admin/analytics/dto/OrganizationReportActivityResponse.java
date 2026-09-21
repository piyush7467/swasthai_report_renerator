package com.swasthai.report_generator.admin.analytics.dto;

import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationReportActivityResponse {

    private String organizationRefId;
    private String organizationName;
    private String organizationCode;
    private OrganizationStatus organizationStatus;

    private long totalReports;
    private long reportsToday;
    private long reportsThisWeek;
    private long reportsThisMonth;

    private long draftReports;
    private long calculatedReports;
    private long finalizedReports;

    private Instant lastReportCreatedAt;
}

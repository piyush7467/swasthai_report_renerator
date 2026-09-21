package com.swasthai.report_generator.admin.analytics.repository;

import java.time.Instant;

public interface OrganizationActivityProjection {

    String getOrganizationRefId();

    String getOrganizationName();

    String getOrganizationCode();

    String getOrganizationStatus();

    Long getTotalReports();

    Long getReportsToday();

    Long getReportsThisWeek();

    Long getReportsThisMonth();

    Long getDraftReports();

    Long getCalculatedReports();

    Long getFinalizedReports();

    Instant getLastReportCreatedAt();
}

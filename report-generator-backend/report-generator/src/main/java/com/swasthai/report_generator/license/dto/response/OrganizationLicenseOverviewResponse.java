package com.swasthai.report_generator.license.dto.response;

import lombok.Builder;

@Builder
public record OrganizationLicenseOverviewResponse(
        LicenseResponse license,
        StaffUsageSummary staffUsage,
        PatientUsageSummary patientUsage,
        ReportUsageSummary reportUsage,
        long daysRemaining,
        String expiryStatus
) {

    @Builder
    public record StaffUsageSummary(
            long activeStaff,
            int maxLabStaff,
            int remainingSlots,
            long totalStaff,
            boolean limitReached,
            boolean overLimit
    ) {
    }

    @Builder
    public record PatientUsageSummary(
            long totalPatients
    ) {
    }

    @Builder
    public record ReportUsageSummary(
            long totalReports,
            long finalizedReports,
            long monthlyReportsCreated,
            int maxReportsPerMonth,
            long dailyReportsCreated,
            int maxReportsPerDay,
            int remainingMonthlyReports,
            boolean monthlyLimitReached,
            boolean dailyLimitReached
    ) {
    }
}

package com.swasthai.report_generator.report.dto.response;

import java.time.Instant;

public record PatientReportStats(
        String patientRefId,
        long totalReports,
        Instant lastReportDate
) {
}

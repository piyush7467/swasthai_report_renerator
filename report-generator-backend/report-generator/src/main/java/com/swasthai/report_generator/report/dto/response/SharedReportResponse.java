package com.swasthai.report_generator.report.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record SharedReportResponse(
        String shareToken,
        Instant expiresAt,
        String verificationUrl,
        ReportResponse report
) {
}

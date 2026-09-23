package com.swasthai.report_generator.report.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ReportShareResponse(
        String shareToken,
        String reportRefId,
        String shareChannel,
        String shareUrl,
        Instant expiresAt,
        Instant createdAt
) {
}

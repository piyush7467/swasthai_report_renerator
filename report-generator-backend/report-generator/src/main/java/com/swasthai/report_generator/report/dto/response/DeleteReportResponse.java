package com.swasthai.report_generator.report.dto.response;

import java.time.Instant;

public record DeleteReportResponse(
        String reportRefId,
        Instant deletedAt
) {}

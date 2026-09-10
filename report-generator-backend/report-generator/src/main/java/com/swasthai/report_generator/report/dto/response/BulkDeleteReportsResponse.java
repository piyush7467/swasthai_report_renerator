package com.swasthai.report_generator.report.dto.response;

import java.time.Instant;

public record BulkDeleteReportsResponse(
        int deletedCount,
        int skippedCount,
        Instant timestamp
) {
}

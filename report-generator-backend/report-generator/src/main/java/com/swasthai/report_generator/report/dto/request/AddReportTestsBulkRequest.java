package com.swasthai.report_generator.report.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request payload for adding multiple tests to a report draft in a single atomic transaction.
 */
public record AddReportTestsBulkRequest(
        @NotEmpty(message = "At least one test reference ID is required")
        List<@NotBlank(message = "Test reference ID cannot be blank") String> testRefIds,

        Long lockVersion
) {
}

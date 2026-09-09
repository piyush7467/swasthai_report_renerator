package com.swasthai.report_generator.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for dynamically adding a test to a report draft.
 */
public record AddReportTestRequest(
        @NotBlank(message = "Test reference ID is required")
        @Size(max = 30, message = "Test reference ID must not exceed 30 characters")
        String testRefId,

        Long lockVersion
) {
}
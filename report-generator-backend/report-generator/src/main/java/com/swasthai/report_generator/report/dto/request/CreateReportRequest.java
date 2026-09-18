package com.swasthai.report_generator.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new report draft.
 *
 * Security:
 * - Only patientRefId is provided by client.
 * - Organization is derived strictly from authenticated user context.
 * - Status, version, audit timestamps, and internal UUIDs are not accepted.
 */
public record CreateReportRequest(
        @NotBlank(message = "Patient reference ID is required")
        @Size(max = 50, message = "Patient reference ID must not exceed 50 characters")
        String patientRefId,

        Boolean includeOrganizationHeader
) {
    public CreateReportRequest(String patientRefId) {
        this(patientRefId, false);
    }
}
package com.swasthai.report_generator.report.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating the includeOrganizationHeader option on a report.
 */
public record UpdateReportHeaderOptionRequest(
        @NotNull(message = "includeOrganizationHeader is required")
        Boolean includeOrganizationHeader
) {
}
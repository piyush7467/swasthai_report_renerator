package com.swasthai.report_generator.report.dto.response;

import com.swasthai.report_generator.report.entity.ReportStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Authoritative response DTO for a medical report.
 * Provides complete data for the frontend to reconstruct the entire screen upon reload/refresh.
 */
@Builder
public record ReportResponse(
        String refId,
        String organizationRefId,
        String organizationName,
        String patientRefId,
        ReportStatus status,
        Integer reportVersion,
        Long lockVersion,
        String createdByEmail,
        String finalizedByEmail,
        Instant finalizedAt,
        Instant createdAt,
        Instant updatedAt,
        List<ReportTestItemResponse> tests,
        Boolean includeOrganizationHeader
) {
}
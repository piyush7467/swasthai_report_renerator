package com.swasthai.report_generator.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicReportVerificationResponse(
        boolean valid,
        String reportRefId,
        String organizationName,
        String reportStatus,
        Instant finalizedAt,
        String message
) {

    public static PublicReportVerificationResponse valid(
            String reportRefId,
            String organizationName,
            String reportStatus,
            Instant finalizedAt
    ) {
        return PublicReportVerificationResponse.builder()
                .valid(true)
                .reportRefId(reportRefId)
                .organizationName(organizationName)
                .reportStatus(reportStatus)
                .finalizedAt(finalizedAt)
                .build();
    }

    public static PublicReportVerificationResponse invalid(
            String reportRefId,
            String message
    ) {
        return PublicReportVerificationResponse.builder()
                .valid(false)
                .reportRefId(reportRefId)
                .message(message != null ? message : "Report could not be verified.")
                .build();
    }
}

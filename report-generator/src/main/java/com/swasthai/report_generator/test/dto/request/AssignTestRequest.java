package com.swasthai.report_generator.test.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AssignTestRequest(

        @NotBlank(message = "Organization refId is required")
        @Size(max = 30, message = "Organization refId is invalid")
        String organizationRefId,

        @NotBlank(message = "Test refId is required")
        @Size(max = 30, message = "Test refId is invalid")
        String testRefId,

        LocalDate effectiveFrom,

        LocalDate effectiveUntil
) {
}
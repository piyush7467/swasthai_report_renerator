package com.swasthai.report_generator.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BreakGlassAccessRequest(
        @NotBlank(message = "Target organization reference ID is required")
        String organizationRefId,

        @NotBlank(message = "Break-glass justification is required")
        @Size(min = 10, max = 500, message = "Justification must be between 10 and 500 characters")
        String justification
) {}

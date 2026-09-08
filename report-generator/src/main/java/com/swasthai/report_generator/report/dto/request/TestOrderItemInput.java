package com.swasthai.report_generator.report.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TestOrderItemInput(
        @NotBlank(message = "Report test refId is required")
        @Size(max = 30, message = "Report test refId must not exceed 30 characters")
        String reportTestRefId,

        @NotNull(message = "Display order is required")
        @Min(value = 1, message = "Display order must be at least 1")
        Integer displayOrder
) {
}
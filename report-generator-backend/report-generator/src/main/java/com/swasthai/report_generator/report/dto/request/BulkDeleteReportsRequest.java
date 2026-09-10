package com.swasthai.report_generator.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkDeleteReportsRequest(
        @NotNull(message = "Report reference IDs list is required")
        @NotEmpty(message = "At least one report reference ID is required")
        List<@NotBlank(message = "Report reference ID cannot be blank") String> reportRefIds
) {
}
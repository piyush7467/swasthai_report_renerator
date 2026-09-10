package com.swasthai.report_generator.report.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DeleteReportsByDateRangeRequest(

        @NotNull(message = "From date is required")
        LocalDate from,

        @NotNull(message = "To date is required")
        LocalDate to

) {
}
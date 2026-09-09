package com.swasthai.report_generator.report.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReorderReportTestsRequest(
        Long lockVersion,

        @NotEmpty(message = "Test orders list cannot be empty")
        @Size(max = 50, message = "Cannot reorder more than 50 tests at once")
        List<@Valid TestOrderItemInput> testOrders
) {
}
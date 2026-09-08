package com.swasthai.report_generator.report.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ReportTestItemResponse(
        String refId,
        String testRefId,
        String testCode,
        String testName,
        Integer displayOrder,
        Integer testVersion,
        List<ReportParameterItemResponse> parameters
) {
}
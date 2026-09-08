package com.swasthai.report_generator.test.dto.response;

import java.util.List;

public record CalculatedTestResponse(
        String testRefId,
        String testCode,
        String testName,
        List<CalculatedParameterItemResponse> parameters
) {
}

package com.swasthai.report_generator.test.service;

import com.swasthai.report_generator.test.dto.request.CalculateTestRequest;
import com.swasthai.report_generator.test.dto.response.CalculatedTestResponse;

public interface TestCalculationService {

    CalculatedTestResponse calculateParameters(
            String testRefId,
            CalculateTestRequest request
    );
}

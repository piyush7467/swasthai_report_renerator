package com.swasthai.report_generator.test.service;

import com.swasthai.report_generator.test.dto.request.CreateTestParameterRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestParameterRequest;
import com.swasthai.report_generator.test.dto.response.TestParameterResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TestParameterService {

    TestParameterResponse createParameter(
            String testRefId,
            CreateTestParameterRequest request
    );

    List<TestParameterResponse> createParametersBulk(
            String testRefId,
            List<CreateTestParameterRequest> requests
    );

    TestParameterResponse getParameter(
            String parameterRefId
    );

    Page<TestParameterResponse> getTestParameters(
            String testRefId,
            String status,
            Pageable pageable
    );

    TestParameterResponse updateParameter(
            String parameterRefId,
            UpdateTestParameterRequest request
    );

    void deactivateParameter(
            String parameterRefId
    );
}
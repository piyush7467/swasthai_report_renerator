package com.swasthai.report_generator.test.service;

import com.swasthai.report_generator.test.dto.request.CreateTestResultRequest;
import com.swasthai.report_generator.test.dto.response.TestResultResponse;
import com.swasthai.report_generator.test.entity.PatientTestResultStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TestResultService {

    
    TestResultResponse createResult(
            CreateTestResultRequest request
    );

    TestResultResponse getResult(
            String resultRefId
    );

    Page<TestResultResponse> getMyOrganizationResults(
            PatientTestResultStatus status,
            Pageable pageable
    );

    TestResultResponse finalizeResult(
            String resultRefId
    );
}
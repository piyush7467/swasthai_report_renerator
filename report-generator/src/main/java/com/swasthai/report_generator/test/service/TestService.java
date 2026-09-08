package com.swasthai.report_generator.test.service;

import com.swasthai.report_generator.test.dto.request.CreateTestRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestRequest;
import com.swasthai.report_generator.test.dto.response.TestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TestService {

    TestResponse createTest(CreateTestRequest request);

    List<TestResponse> createTestsBulk(List<CreateTestRequest> requests);

    TestResponse getTest(String testRefId);

    Page<TestResponse> getTests(
            String search,
            String categoryRefId,
            String status,
            Pageable pageable
    );

    TestResponse updateTest(
            String testRefId,
            UpdateTestRequest request
    );

    void deleteTest(String testRefId);
}
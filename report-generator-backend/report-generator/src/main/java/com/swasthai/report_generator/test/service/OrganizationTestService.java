package com.swasthai.report_generator.test.service;

import com.swasthai.report_generator.test.dto.request.AssignTestRequest;
import com.swasthai.report_generator.test.dto.request.UpdateOrganizationTestRequest;
import com.swasthai.report_generator.test.dto.response.OrganizationTestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrganizationTestService {

    OrganizationTestResponse assignTest(
            AssignTestRequest request
    );

    OrganizationTestResponse getAssignment(
            String organizationTestRefId
    );

    Page<OrganizationTestResponse> getAllAssignments(
            String organizationRefId,
            String status,
            Pageable pageable
    );

    Page<OrganizationTestResponse> getMyOrganizationTests(
            String status,
            Pageable pageable
    );

    OrganizationTestResponse updateAssignment(
            String organizationTestRefId,
            UpdateOrganizationTestRequest request
    );

    void deactivateAssignment(
            String organizationTestRefId
    );

    boolean hasTestAccess(
            String testRefId
    );
}
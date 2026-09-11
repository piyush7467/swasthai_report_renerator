package com.swasthai.report_generator.organization.service;

import com.swasthai.report_generator.organization.dto.request.CreateOrganizationRequest;
import com.swasthai.report_generator.organization.dto.request.UpdateOrganizationRequest;
import com.swasthai.report_generator.organization.dto.request.UpdateOrganizationStatusRequest;
import com.swasthai.report_generator.organization.dto.response.OrganizationResponse;
import com.swasthai.report_generator.organization.dto.response.OrganizationPageResponse;

public interface OrganizationService {

    OrganizationResponse createOrganization(
            CreateOrganizationRequest request
    );

    OrganizationPageResponse getOrganizations(
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    OrganizationResponse getOrganizationByRefId(
            String refId
    );

    OrganizationResponse updateOrganization(
            String refId,
            UpdateOrganizationRequest request
    );

    OrganizationResponse updateOrganizationStatus(
            String refId,
            UpdateOrganizationStatusRequest request
    );
}
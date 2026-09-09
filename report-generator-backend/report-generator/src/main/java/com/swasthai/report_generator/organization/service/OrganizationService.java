package com.swasthai.report_generator.organization.service;

import com.swasthai.report_generator.organization.dto.request.CreateOrganizationRequest;
import com.swasthai.report_generator.organization.dto.response.OrganizationResponse;

public interface OrganizationService {

    OrganizationResponse createOrganization( CreateOrganizationRequest request);
}
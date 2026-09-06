package com.swasthai.report_generator.organization.service;

import com.swasthai.report_generator.organization.entity.Organization;

public interface OrganizationSequenceService {

    String generatePatientCode(
            Organization organization
    );

}
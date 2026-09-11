package com.swasthai.report_generator.license.service;

import com.swasthai.report_generator.license.dto.request.ActivateLicenseRequest;
import com.swasthai.report_generator.license.dto.request.RenewLicenseRequest;
import com.swasthai.report_generator.license.dto.response.LicenseResponse;

public interface LicenseService {

    LicenseResponse activateLicense(
            String organizationRefId,
            ActivateLicenseRequest request
    );

    LicenseResponse renewLicense(
            String organizationRefId,
            RenewLicenseRequest request
    );

    LicenseResponse getOwnLicense();

    LicenseResponse getLicenseForOrganization(
            String organizationRefId
    );

    /**
     * Mandatory server-side authorization check for
     * report generation.
     */
    void requireActiveLicenseForOrganization(
            java.util.UUID organizationId
    );
}
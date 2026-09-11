package com.swasthai.report_generator.license.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LicenseGuard {

    private final LicenseService licenseService;

    /**
     * Must be called before creation of a billable report.
     *
     * This method intentionally accepts only the internal
     * organization UUID resolved by the authenticated user.
     *
     * It does NOT accept an organizationRefId from the client.
     */
    public void requireReportCreationAllowed(
            UUID organizationId
    ) {

        licenseService
                .requireActiveLicenseForOrganization(
                        organizationId
                );
    }

    /**
     * Must be called before modifying, calculating, or finalizing a clinical report.
     * Enforces that an active, non-expired license is held by the organization.
     */
    public void requireActiveLicenseForOrganization(
            UUID organizationId
    ) {
        licenseService
                .requireActiveLicenseForOrganization(
                        organizationId
                );
    }
}
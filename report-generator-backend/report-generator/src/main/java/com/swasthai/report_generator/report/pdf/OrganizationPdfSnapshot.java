package com.swasthai.report_generator.report.pdf;

import lombok.Builder;

@Builder
public record OrganizationPdfSnapshot(

        String name,

        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,

        String phone,
        String alternatePhone,
        String email,
        String website,

        String logoStorageKey,

        String signatureStorageKey,
        String signatureOwnerName,
        String signatureOwnerEmail,

        String reportFooterText,
        String reportDisclaimer
) {
}
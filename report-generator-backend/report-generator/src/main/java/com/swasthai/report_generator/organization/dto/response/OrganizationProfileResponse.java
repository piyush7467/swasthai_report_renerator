package com.swasthai.report_generator.organization.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class OrganizationProfileResponse {

    private String organizationRefId;

    private String organizationName;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private String phone;

    private String alternatePhone;

    private String email;

    private String website;

    private boolean logoConfigured;

    private boolean signatureConfigured;

    private String signatureOwnerRefId;

    private String reportFooterText;

    private String reportDisclaimer;

    private Long version;

    private Instant createdAt;

    private Instant updatedAt;
}
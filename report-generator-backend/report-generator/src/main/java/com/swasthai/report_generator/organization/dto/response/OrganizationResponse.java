package com.swasthai.report_generator.organization.dto.response;

import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class OrganizationResponse {

    private String refId;

    private String name;

    private String code;

    private OrganizationStatus status;

    private Instant createdAt;

    private Instant updatedAt;
}
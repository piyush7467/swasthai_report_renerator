package com.swasthai.report_generator.admin.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditLogResponse {

    private String refId;
    private String actorEmail;
    private String action;
    private String targetOrganizationRefId;
    private String targetOrganizationName;
    private String targetReportRefId;
    private String justification;
    private boolean success;
    private String failureReason;
    private String ipAddress;
    private Instant createdAt;
}

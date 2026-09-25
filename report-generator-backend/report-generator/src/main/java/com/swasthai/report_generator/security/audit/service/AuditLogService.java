package com.swasthai.report_generator.security.audit.service;

import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.security.audit.entity.SecurityAuditLog;
import com.swasthai.report_generator.user.entity.User;

public interface AuditLogService {

    SecurityAuditLog recordBreakGlassAccess(
            User actor,
            Organization targetOrg,
            Report targetReport,
            String targetOrgRefId,
            String targetReportRefId,
            String justification,
            boolean success,
            String failureReason,
            String ipAddress
    );

    SecurityAuditLog recordStaffAction(
            User actor,
            Organization organization,
            User targetStaff,
            String action,
            String justification,
            boolean success,
            String failureReason,
            String ipAddress
    );

    SecurityAuditLog recordUpgradeRequestAction(
            User actor,
            Organization organization,
            String upgradeRequestRefId,
            String action,
            String justification,
            boolean success,
            String failureReason,
            String ipAddress
    );
}


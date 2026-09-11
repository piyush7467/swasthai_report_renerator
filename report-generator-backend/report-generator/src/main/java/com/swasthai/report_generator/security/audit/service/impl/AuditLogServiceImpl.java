package com.swasthai.report_generator.security.audit.service.impl;

import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.security.audit.entity.SecurityAuditLog;
import com.swasthai.report_generator.security.audit.repository.SecurityAuditLogRepository;
import com.swasthai.report_generator.security.audit.service.AuditLogService;
import com.swasthai.report_generator.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final SecurityAuditLogRepository securityAuditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAuditLog recordBreakGlassAccess(
            User actor,
            Organization targetOrg,
            Report targetReport,
            String targetOrgRefId,
            String targetReportRefId,
            String justification,
            boolean success,
            String failureReason,
            String ipAddress) {

        if (actor == null) {
            throw new IllegalArgumentException("Audit actor is required");
        }

        String orgRefId = targetOrg != null ? targetOrg.getRefId() : targetOrgRefId;
        String reportRefId = targetReport != null ? targetReport.getRefId() : targetReportRefId;

        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .actor(actor)
                .actorEmail(actor.getEmail())
                .action("BREAK_GLASS_REPORT_ACCESS")
                .targetOrganizationId(targetOrg != null ? targetOrg.getId() : null)
                .targetOrganizationRefId(orgRefId)
                .targetReportId(targetReport != null ? targetReport.getId() : null)
                .targetReportRefId(reportRefId)
                .justification(justification)
                .success(success)
                .failureReason(failureReason)
                .ipAddress(ipAddress)
                .createdAt(Instant.now())
                .build();

        SecurityAuditLog saved = securityAuditLogRepository.saveAndFlush(auditLog);

        log.warn("BREAK-GLASS ACCESS AUDIT: actor={}, action={}, targetOrg={}, targetReport={}, success={}",
                actor.getEmail(), auditLog.getAction(), orgRefId, reportRefId, success);

        return saved;
    }
}

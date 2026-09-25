package com.swasthai.report_generator.security.audit.service.impl;

import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.security.audit.entity.SecurityAuditLog;
import com.swasthai.report_generator.security.audit.repository.SecurityAuditLogRepository;
import com.swasthai.report_generator.security.audit.service.AuditLogService;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.repository.UserRepository;
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
    private final UserRepository userRepository;

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
                .actor(resolveActorEntity(actor))
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

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAuditLog recordStaffAction(
            User actor,
            Organization organization,
            User targetStaff,
            String action,
            String justification,
            boolean success,
            String failureReason,
            String ipAddress) {

        if (actor == null) {
            throw new IllegalArgumentException("Audit actor is required");
        }

        String orgRefId = organization != null ? organization.getRefId() : null;
        java.util.UUID orgId = organization != null ? organization.getId() : null;

        String fullJustification = justification != null
                ? justification
                : (targetStaff != null ? "Target staff: " + targetStaff.getEmail() : action);
        if (fullJustification.length() > 500) {
            fullJustification = fullJustification.substring(0, 500);
        }

        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .actor(resolveActorEntity(actor))
                .actorEmail(actor.getEmail())
                .action(action)
                .targetOrganizationId(orgId)
                .targetOrganizationRefId(orgRefId)
                .justification(fullJustification)
                .success(success)
                .failureReason(failureReason)
                .ipAddress(ipAddress)
                .createdAt(Instant.now())
                .build();

        SecurityAuditLog saved = securityAuditLogRepository.saveAndFlush(auditLog);

        log.info("STAFF SECURITY AUDIT: actor={}, action={}, targetStaff={}, org={}, success={}",
                actor.getEmail(), action, targetStaff != null ? targetStaff.getEmail() : "N/A", orgRefId, success);

        return saved;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAuditLog recordUpgradeRequestAction(
            User actor,
            Organization organization,
            String upgradeRequestRefId,
            String action,
            String justification,
            boolean success,
            String failureReason,
            String ipAddress) {

        if (actor == null) {
            throw new IllegalArgumentException("Audit actor is required");
        }

        String orgRefId = organization != null ? organization.getRefId() : null;
        java.util.UUID orgId = organization != null ? organization.getId() : null;

        String fullJustification = justification != null
                ? justification
                : (upgradeRequestRefId != null ? "Upgrade request: " + upgradeRequestRefId : action);
        if (fullJustification.length() > 500) {
            fullJustification = fullJustification.substring(0, 500);
        }

        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .actor(resolveActorEntity(actor))
                .actorEmail(actor.getEmail())
                .action(action)
                .targetOrganizationId(orgId)
                .targetOrganizationRefId(orgRefId)
                .justification(fullJustification)
                .success(success)
                .failureReason(failureReason)
                .ipAddress(ipAddress)
                .createdAt(Instant.now())
                .build();

        SecurityAuditLog saved = securityAuditLogRepository.saveAndFlush(auditLog);

        log.info("UPGRADE REQUEST AUDIT: actor={}, action={}, requestRefId={}, org={}, success={}",
                actor.getEmail(), action, upgradeRequestRefId != null ? upgradeRequestRefId : "N/A", orgRefId, success);

        return saved;
    }

    private User resolveActorEntity(User actor) {
        if (actor == null || actor.getId() == null) {
            return null;
        }
        try {
            if (userRepository.existsById(actor.getId())) {
                return userRepository.getReferenceById(actor.getId());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}



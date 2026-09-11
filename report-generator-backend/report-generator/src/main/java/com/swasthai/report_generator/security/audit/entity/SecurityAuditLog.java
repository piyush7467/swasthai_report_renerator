package com.swasthai.report_generator.security.audit.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "security_audit_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_security_audit_logs_ref_id", columnNames = "ref_id")
        },
        indexes = {
                @Index(name = "idx_security_audit_logs_action", columnList = "action"),
                @Index(name = "idx_security_audit_logs_target_org", columnList = "target_organization_ref_id"),
                @Index(name = "idx_security_audit_logs_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SecurityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ref_id", nullable = false, updatable = false, length = 40)
    private String refId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false, updatable = false)
    private User actor;

    @Column(name = "actor_email", nullable = false, updatable = false, length = 255)
    private String actorEmail;

    @Column(name = "action", nullable = false, updatable = false, length = 50)
    private String action;

    @Column(name = "target_organization_id", updatable = false)
    private UUID targetOrganizationId;

    @Column(name = "target_organization_ref_id", updatable = false, length = 50)
    private String targetOrganizationRefId;

    @Column(name = "target_report_id", updatable = false)
    private UUID targetReportId;

    @Column(name = "target_report_ref_id", updatable = false, length = 50)
    private String targetReportRefId;

    @Column(name = "justification", nullable = false, updatable = false, length = 500)
    private String justification;

    @Column(name = "success", nullable = false, updatable = false)
    private boolean success;

    @Column(name = "failure_reason", updatable = false, length = 255)
    private String failureReason;

    @Column(name = "ip_address", updatable = false, length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (refId == null || refId.isBlank()) {
            refId = RefIdGenerator.generate("AUDIT");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

package com.swasthai.report_generator.report.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "report_shares",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_report_shares_ref_id", columnNames = "ref_id"),
                @UniqueConstraint(name = "uk_report_shares_token", columnNames = "share_token")
        },
        indexes = {
                @Index(name = "idx_report_shares_token", columnList = "share_token"),
                @Index(name = "idx_report_shares_report", columnList = "report_id"),
                @Index(name = "idx_report_shares_organization", columnList = "organization_id"),
                @Index(name = "idx_report_shares_created_at", columnList = "created_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ref_id", nullable = false, unique = true, updatable = false, length = 40)
    private String refId;

    @Column(name = "share_token", nullable = false, unique = true, updatable = false, length = 64)
    private String shareToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, foreignKey = @ForeignKey(name = "fk_report_shares_report"))
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, foreignKey = @ForeignKey(name = "fk_report_shares_organization"))
    private Organization organization;

    @Column(name = "report_ref_id", nullable = false, length = 30)
    private String reportRefId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "shared_by_user_id", nullable = true, foreignKey = @ForeignKey(name = "fk_report_shares_user"))
    private User sharedBy;

    @Column(name = "share_channel", nullable = false, length = 20)
    private String shareChannel;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 50)
    private String recipientPhone;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "access_count", nullable = false)
    @Builder.Default
    private int accessCount = 0;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (refId == null || refId.isBlank()) {
            refId = RefIdGenerator.generate("SHARE");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

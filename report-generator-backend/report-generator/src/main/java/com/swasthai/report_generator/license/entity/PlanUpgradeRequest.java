package com.swasthai.report_generator.license.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "plan_upgrade_requests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_plan_upgrade_requests_ref_id",
                        columnNames = "ref_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_plan_upgrade_requests_org_status",
                        columnList = "organization_id,status"
                ),
                @Index(
                        name = "idx_plan_upgrade_requests_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_plan_upgrade_requests_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanUpgradeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "ref_id",
            nullable = false,
            updatable = false,
            length = 40
    )
    private String refId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_plan_upgrade_requests_organization")
    )
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "requested_by_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_plan_upgrade_requests_requested_by")
    )
    private User requestedBy;

    @Column(
            name = "requested_by_email",
            nullable = false,
            updatable = false,
            length = 255
    )
    private String requestedByEmail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "current_plan_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_plan_upgrade_requests_current_plan")
    )
    private Plan currentPlan;

    @Column(
            name = "current_plan_name",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String currentPlanName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "requested_plan_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_plan_upgrade_requests_requested_plan")
    )
    private Plan requestedPlan;

    @Column(
            name = "requested_plan_name",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String requestedPlanName;

    @Column(
            name = "current_active_staff_count",
            nullable = false,
            updatable = false
    )
    private int currentActiveStaffCount;

    @Column(
            name = "requested_staff_capacity",
            nullable = false,
            updatable = false
    )
    private int requestedStaffCapacity;

    @Column(
            name = "reason",
            length = 500
    )
    private String reason;

    @Column(
            name = "contact_name",
            nullable = false,
            length = 150
    )
    private String contactName;

    @Column(
            name = "contact_email",
            nullable = false,
            length = 255
    )
    private String contactEmail;

    @Column(
            name = "contact_phone",
            length = 50
    )
    private String contactPhone;

    @Column(
            name = "additional_message",
            length = 1000
    )
    private String additionalMessage;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private UpgradeRequestStatus status = UpgradeRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reviewed_by_user_id",
            foreignKey = @ForeignKey(name = "fk_plan_upgrade_requests_reviewed_by")
    )
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(
            name = "admin_notes",
            length = 1000
    )
    private String adminNotes;

    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        if (refId == null || refId.isBlank()) {
            refId = RefIdGenerator.generate("UPG");
        }
        if (status == null) {
            status = UpgradeRequestStatus.PENDING;
        }
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}

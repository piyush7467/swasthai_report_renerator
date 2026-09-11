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
        name = "licenses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_licenses_ref_id",
                        columnNames = "ref_id"
                ),
                @UniqueConstraint(
                        name = "uk_licenses_organization",
                        columnNames = "organization_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_licenses_org_status",
                        columnList = "organization_id,status"
                ),
                @Index(
                        name = "idx_licenses_expires_at",
                        columnList = "expires_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Public immutable reference.
     */
    @Column(
            name = "ref_id",
            nullable = false,
            updatable = false,
            length = 40
    )
    private String refId;

    /**
     * Exactly one license record per organization.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_licenses_organization"
            )
    )
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "plan_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_licenses_plan"
            )
    )
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private LicenseStatus status;

    @Column(
            name = "started_at",
            nullable = false
    )
    private Instant startedAt;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private Instant expiresAt;

    /**
     * Optional manual payment reference.
     *
     * This is NOT a gateway transaction.
     * It can contain your manually verified UTR,
     * receipt number, etc.
     */
    @Column(
            name = "payment_reference",
            length = 150
    )
    private String paymentReference;

    /**
     * Who manually verified the payment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "payment_verified_by",
            foreignKey = @ForeignKey(
                    name = "fk_licenses_payment_verified_by"
            )
    )
    private User paymentVerifiedBy;

    @Column(name = "payment_verified_at")
    private Instant paymentVerifiedAt;

    @Version
    @Column(
            name = "lock_version",
            nullable = false
    )
    private Long lockVersion;

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
            refId = RefIdGenerator.generate("LIC");
        }

        if (status == null) {
            status = LicenseStatus.ACTIVE;
        }

        if (lockVersion == null) {
            lockVersion = 0L;
        }

        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}
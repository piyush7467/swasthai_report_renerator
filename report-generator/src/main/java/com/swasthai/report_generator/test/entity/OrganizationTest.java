package com.swasthai.report_generator.test.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "organization_tests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_org_test_ref_id",
                        columnNames = "ref_id"
                ),
                @UniqueConstraint(
                        name = "uk_org_test",
                        columnNames = {
                                "organization_id",
                                "test_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_org_test_organization",
                        columnList = "organization_id"
                ),
                @Index(
                        name = "idx_org_test_test",
                        columnList = "test_id"
                ),
                @Index(
                        name = "idx_org_test_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_org_test_effective_dates",
                        columnList = "effective_from,effective_until"
                ),
                @Index(
                        name = "idx_org_test_ref_id",
                        columnList = "ref_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationTest {

    // INTERNAL IDENTITY

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // PUBLIC IDENTITY

    @Column(
            name = "ref_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 30
    )
    private String refId;

    // RELATIONSHIPS

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_org_test_organization"
            )
    )
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "test_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_org_test_test"
            )
    )
    private Test test;

    // ASSIGNMENT / LIFECYCLE

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private OrganizationTestStatus status =
            OrganizationTestStatus.ACTIVE;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    // AUDIT

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

    // JPA LIFECYCLE

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (refId == null || refId.isBlank()) {
            refId = RefIdGenerator.generate("OT");
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = Instant.now();
    }
}
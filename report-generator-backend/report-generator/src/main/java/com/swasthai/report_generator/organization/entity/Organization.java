package com.swasthai.report_generator.organization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "organizations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_organization_code",
                        columnNames = "code"
                ),
                @UniqueConstraint(
                        name = "uk_organization_ref_id",
                        columnNames = "ref_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_organization_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_organization_ref_id",
                        columnList = "ref_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    /**
     * Internal database identifier.
     * Never expose this directly through API responses.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Public reference identifier.
     * Used by frontend and external API consumers.
     *
     * Example: ORG-7K4M92XQ
     */
    @Column(
            name = "ref_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 30
    )
    private String refId;

    /**
     * Official organization/clinic name.
     */
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Internal/business-friendly organization code.
     *
     * Example: ABC-DIAGNOSTIC
     */
    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (refId == null || refId.isBlank()) {
            refId = generateRefId();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    private String generateRefId() {
        return com.swasthai.report_generator.common.util.RefIdGenerator.generate("ORG");
    }
}
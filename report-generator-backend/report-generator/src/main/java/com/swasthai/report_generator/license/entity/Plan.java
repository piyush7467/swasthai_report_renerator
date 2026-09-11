package com.swasthai.report_generator.license.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "plans",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_plans_ref_id",
                        columnNames = "ref_id"
                ),
                @UniqueConstraint(
                        name = "uk_plans_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_plans_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Public immutable identifier.
     */
    @Column(
            name = "ref_id",
            nullable = false,
            updatable = false,
            length = 40
    )
    private String refId;

    /**
     * Stable machine-readable code.
     *
     * Example:
     * STARTER
     * PROFESSIONAL
     * ENTERPRISE
     */
    @Column(
            name = "code",
            nullable = false,
            length = 50
    )
    private String code;

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

    /**
     * V1 is annual-only.
     */
    @Column(
            name = "annual_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal annualPrice;

    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    private String currency;

    /**
     * Inactive plans cannot be newly purchased/activated.
     *
     * Existing licenses remain valid.
     */
    @Column(
            name = "active",
            nullable = false
    )
    private boolean active;

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
            refId = RefIdGenerator.generate("PLAN");
        }

        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }

        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}
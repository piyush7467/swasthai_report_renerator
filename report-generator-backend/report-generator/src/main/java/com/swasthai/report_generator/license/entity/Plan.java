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
            name = "max_lab_staff",
            nullable = false
    )
    @Builder.Default
    private Integer maxLabStaff = 3;

    @Column(
            name = "max_reports_per_month",
            nullable = false
    )
    @Builder.Default
    private Integer maxReportsPerMonth = 100;

    @Column(
            name = "max_reports_per_day",
            nullable = false
    )
    @Builder.Default
    private Integer maxReportsPerDay = 25;

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

        if (maxLabStaff == null || maxLabStaff <= 0) {
            maxLabStaff = 3;
        }

        if (maxReportsPerMonth == null || maxReportsPerMonth < 0) {
            maxReportsPerMonth = 100;
        }

        if (maxReportsPerDay == null || maxReportsPerDay < 0) {
            maxReportsPerDay = 25;
        }

        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}
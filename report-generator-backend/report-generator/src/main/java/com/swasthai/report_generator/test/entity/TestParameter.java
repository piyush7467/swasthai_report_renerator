package com.swasthai.report_generator.test.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "test_parameters", uniqueConstraints = {

        @UniqueConstraint(name = "uk_test_parameter_ref_id", columnNames = "ref_id"),

        @UniqueConstraint(name = "uk_test_parameter_test_code", columnNames = {
                "test_id",
                "code"
        })
}, indexes = {

        @Index(name = "idx_test_parameter_test", columnList = "test_id"),

        @Index(name = "idx_test_parameter_status", columnList = "status"),

        @Index(name = "idx_test_parameter_display_order", columnList = "test_id,display_order"),

        @Index(name = "idx_test_parameter_ref_id", columnList = "ref_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestParameter {

    // IDENTITY

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Public immutable reference ID.
     * Never expose the internal UUID through APIs.
     */
    @Column(name = "ref_id", nullable = false, unique = true, updatable = false, length = 30)
    private String refId;

    /**
     * Parent master test.
     *
     * Example:
     *
     * CBC
     * ├── Hemoglobin
     * ├── WBC
     * └── Platelets
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false, foreignKey = @ForeignKey(name = "fk_test_parameter_test"))
    private Test test;

    // PARAMETER IDENTITY

    /**
     * Machine-readable parameter code.
     *
     * Examples:
     * HB
     * WBC
     * RBC
     */
    @Column(nullable = false, length = 50)
    private String code;

    /**
     * Human-readable parameter name.
     *
     * Example:
     * Hemoglobin
     */
    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    // DATA CONFIGURATION

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private TestParameterDataType dataType;

    /**
     * Measurement unit.
     *
     * Examples:
     * g/dL
     * mg/dL
     * cells/µL
     * %
     *
     * Nullable because TEXT / BOOLEAN parameters may not need units.
     */
    @Column(length = 50)
    private String unit;

    /**
     * Indicates whether this parameter must have a result
     * when the test is reported.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean required = true;

    /**
     * Controls parameter order in reports.
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;

    // NUMERIC REFERENCE RANGE

    /**
     * Lower normal/reference boundary.
     *
     * Used primarily for INTEGER / DECIMAL parameters.
     */
    @Column(name = "reference_min", precision = 19, scale = 6)
    private BigDecimal referenceMin;

    /**
     * Upper normal/reference boundary.
     */
    @Column(name = "reference_max", precision = 19, scale = 6)
    private BigDecimal referenceMax;

    // CRITICAL RANGE

    /**
     * Values below this threshold can be considered critical-low.
     */
    @Column(name = "critical_low", precision = 19, scale = 6)
    private BigDecimal criticalLow;

    /**
     * Values above this threshold can be considered critical-high.
     */
    @Column(name = "critical_high", precision = 19, scale = 6)
    private BigDecimal criticalHigh;

    // REPORTING

    /**
     * Optional text shown alongside the parameter in the report.
     */
    @Column(name = "report_description", length = 500)
    private String reportDescription;

    /**
     * Optional guidance for the reporting/interpretation layer.
     *
     * This is configuration, not an automatic diagnosis.
     */
    @Column(name = "interpretation_guidance", columnDefinition = "TEXT")
    private String interpretationGuidance;

    // LIFECYCLE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TestParameterStatus status = TestParameterStatus.ACTIVE;

    /**
     * Manual configuration version.
     *
     * This is useful for tracking changes to master configuration.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // JPA CALLBACKS

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (refId == null || refId.isBlank()) {
            refId = RefIdGenerator.generate("PARAM");
        }

        normalize();
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = Instant.now();

        normalize();
    }

    // NORMALIZATION

    private void normalize() {

        if (code != null) {
            code = code.trim().toUpperCase();
        }

        if (name != null) {
            name = name.trim();
        }

        if (description != null) {
            description = description.trim();
        }

        if (unit != null) {
            unit = unit.trim();
        }

        if (reportDescription != null) {
            reportDescription = reportDescription.trim();
        }
    }
}
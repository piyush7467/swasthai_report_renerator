package com.swasthai.report_generator.test.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_test_ref_id", columnNames = "ref_id"),
        @UniqueConstraint(name = "uk_test_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_test_name", columnNames = "name")
}, indexes = {
        @Index(name = "idx_test_category", columnList = "category_id"),
        @Index(name = "idx_test_status", columnList = "status"),
        @Index(name = "idx_test_type", columnList = "test_type"),
        @Index(name = "idx_test_sample_type", columnList = "sample_type"),
        @Index(name = "idx_test_name", columnList = "name"),
        @Index(name = "idx_test_ref_id", columnList = "ref_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Test {

    // Internal Identity

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Public Identity

    @Column(name = "ref_id", nullable = false, unique = true, updatable = false, length = 30)
    private String refId;

    // Classification

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_test_category"))
    private TestCategory category;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "short_name", length = 75)
    private String shortName;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false, length = 20)
    @Builder.Default
    private TestType testType = TestType.INDIVIDUAL;

    @Column(length = 1000)
    private String description;

    // Sample Information

    @Enumerated(EnumType.STRING)
    @Column(name = "sample_type", nullable = false, length = 30)
    private SampleType sampleType;

    @Column(name = "custom_sample_type", length = 100)
    private String customSampleType;

    @Column(name = "specimen_container", length = 150)
    private String specimenContainer;

    @Column(name = "sample_volume", precision = 10, scale = 2)
    private BigDecimal sampleVolume;

    @Column(name = "sample_volume_unit", length = 20)
    private String sampleVolumeUnit;

    @Column(name = "fasting_required", nullable = false)
    @Builder.Default
    private boolean fastingRequired = false;

    @Column(name = "patient_preparation", length = 1000)
    private String patientPreparation;

    @Column(name = "collection_instructions", length = 1500)
    private String collectionInstructions;

    // Processing

    @Column(name = "turnaround_time_hours")
    private Integer turnaroundTimeHours;

    @Column(name = "priority_supported", nullable = false)
    @Builder.Default
    private boolean prioritySupported = false;

    @Column(name = "outsourced", nullable = false)
    @Builder.Default
    private boolean outsourced = false;

    @Column(name = "laboratory_instructions", length = 1500)
    private String laboratoryInstructions;

    // Reporting

    @Column(name = "report_section", length = 100)
    private String reportSection;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "report_description", length = 1000)
    private String reportDescription;

    @Column(name = "interpretation_guidance", length = 2000)
    private String interpretationGuidance;

    // Commercial

    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "billing_code", length = 50)
    private String billingCode;

    // Lifecycle

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TestStatus status = TestStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    // Audit timestamps

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // JPA Lifecycle

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (refId == null || refId.isBlank()) {
            refId = RefIdGenerator.generate("TEST");
        }

        normalizeFields();
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = Instant.now();

        normalizeFields();
    }

    // Normalization

    private void normalizeFields() {

        if (code != null) {
            code = code.trim().toUpperCase();
        }

        if (name != null) {
            name = name.trim();
        }

        if (shortName != null) {
            shortName = shortName.trim();
        }

        if (description != null) {
            description = description.trim();

            if (description.isBlank()) {
                description = null;
            }
        }

        if (customSampleType != null) {
            customSampleType = customSampleType.trim();

            if (customSampleType.isBlank()) {
                customSampleType = null;
            }
        }

        if (specimenContainer != null) {
            specimenContainer = specimenContainer.trim();
        }

        if (sampleVolumeUnit != null) {
            sampleVolumeUnit = sampleVolumeUnit.trim();
        }

        if (patientPreparation != null) {
            patientPreparation = patientPreparation.trim();
        }

        if (collectionInstructions != null) {
            collectionInstructions = collectionInstructions.trim();
        }

        if (laboratoryInstructions != null) {
            laboratoryInstructions = laboratoryInstructions.trim();
        }

        if (reportSection != null) {
            reportSection = reportSection.trim();
        }

        if (reportDescription != null) {
            reportDescription = reportDescription.trim();
        }

        if (interpretationGuidance != null) {
            interpretationGuidance = interpretationGuidance.trim();
        }

        if (billingCode != null) {
            billingCode = billingCode.trim().toUpperCase();
        }

        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
    }
}
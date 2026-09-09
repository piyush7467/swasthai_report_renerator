package com.swasthai.report_generator.test.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.report.entity.ReportTestResult;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "test_parameter_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_test_parameter_result_ref_id",
                        columnNames = "ref_id"
                ),
                @UniqueConstraint(
                        name = "uk_ptr_test_parameter",
                        columnNames = {
                                "patient_test_result_id",
                                "test_parameter_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_rtr_test_parameter",
                        columnNames = {
                                "report_test_result_id",
                                "test_parameter_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_tpr_patient_test_result",
                        columnList = "patient_test_result_id"
                ),
                @Index(
                        name = "idx_tpr_report_test_result",
                        columnList = "report_test_result_id"
                ),
                @Index(
                        name = "idx_tpr_test_parameter",
                        columnList = "test_parameter_id"
                ),
                @Index(
                        name = "idx_tpr_ref_id",
                        columnList = "ref_id"
                ),
                @Index(
                        name = "idx_tpr_flag",
                        columnList = "flag"
                ),
                @Index(
                        name = "idx_tpr_parameter_code",
                        columnList = "parameter_code"
                ),
                @Index(
                        name = "idx_tpr_display_order",
                        columnList = "patient_test_result_id,display_order"
                ),
                @Index(
                        name = "idx_tpr_rtr_display_order",
                        columnList = "report_test_result_id,display_order"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestParameterResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "ref_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 30
    )
    private String refId;

    /**
     * Optional legacy parent (for V3 backwards compatibility).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "patient_test_result_id",
            foreignKey = @ForeignKey(
                    name = "fk_test_parameter_result_ptr"
            )
    )
    private PatientTestResult patientTestResult;

    /**
     * Parent ReportTestResult (for modern V4+ multi-test report workflow).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "report_test_result_id",
            foreignKey = @ForeignKey(
                    name = "fk_test_parameter_result_rtr"
            )
    )
    private ReportTestResult reportTestResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "test_parameter_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_test_parameter_result_param"
            )
    )
    private TestParameter testParameter;

    // --------------------------------------------------
    // Result values
    // --------------------------------------------------

    @Column(name = "value", length = 500)
    private String value;

    @Column(
            name = "numeric_value",
            precision = 19,
            scale = 6
    )
    private BigDecimal numericValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag", length = 20)
    private ResultFlag flag;

    // --------------------------------------------------
    // Historical parameter snapshot
    // --------------------------------------------------

    @Column(
            name = "parameter_code",
            nullable = false,
            length = 50
    )
    private String parameterCode;

    @Column(
            name = "parameter_name",
            nullable = false,
            length = 150
    )
    private String parameterName;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "data_type",
            nullable = false,
            length = 20
    )
    private TestParameterDataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "input_type",
            nullable = false,
            length = 20
    )
    private ParameterInputType inputType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "calculation_type",
            nullable = false,
            length = 50
    )
    @Builder.Default
    private CalculationType calculationType = CalculationType.NONE;

    @Column(
            name = "calculation_version",
            length = 50
    )
    private String calculationVersion;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(
            name = "reference_min",
            precision = 19,
            scale = 6
    )
    private BigDecimal referenceMin;

    @Column(
            name = "reference_max",
            precision = 19,
            scale = 6
    )
    private BigDecimal referenceMax;

    @Column(
            name = "critical_low",
            precision = 19,
            scale = 6
    )
    private BigDecimal criticalLow;

    @Column(
            name = "critical_high",
            precision = 19,
            scale = 6
    )
    private BigDecimal criticalHigh;

    @Column(
            name = "display_order",
            nullable = false
    )
    @Builder.Default
    private Integer displayOrder = 1;

    // --------------------------------------------------
    // Timestamps
    // --------------------------------------------------

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

    // --------------------------------------------------
    // Lifecycle
    // --------------------------------------------------

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (refId == null || refId.isBlank()) {
            refId = RefIdGenerator.generate("TPR");
        }

        if (calculationType == null) {
            calculationType = CalculationType.NONE;
        }

        if (displayOrder == null) {
            displayOrder = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
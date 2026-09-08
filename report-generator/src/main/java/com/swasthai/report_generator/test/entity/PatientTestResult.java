package com.swasthai.report_generator.test.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "patient_test_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_patient_test_result_ref_id",
                        columnNames = "ref_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_patient_test_result_organization",
                        columnList = "organization_id"
                ),
                @Index(
                        name = "idx_patient_test_result_test",
                        columnList = "test_id"
                ),
                @Index(
                        name = "idx_patient_test_result_patient",
                        columnList = "patient_ref_id"
                ),
                @Index(
                        name = "idx_patient_test_result_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_patient_test_result_performed_at",
                        columnList = "performed_at"
                ),
                @Index(
                        name = "idx_patient_test_result_ref_id",
                        columnList = "ref_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Public immutable reference ID.
     * Never use this as an authorization mechanism by itself.
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
     * Organization that owns this test result.
     *
     * For ORG_ADMIN/LAB_STAFF this must always come
     * from the authenticated user's organization.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_patient_test_result_organization"
            )
    )
    private Organization organization;

    /**
     * Master test definition.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "test_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_patient_test_result_test"
            )
    )
    private Test test;

    /**
     * Parameter results belonging to this test result.
     *
     * PatientTestResult is the aggregate root.
     * Its parameter results are created, updated and removed
     * through this relationship.
     */
    @OneToMany(
            mappedBy = "patientTestResult",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<TestParameterResult> parameterResults =
            new ArrayList<>();

    /**
     * Reference to the patient owned by the external
     * SwasthAI system.
     *
     * Report Generator does not own the patient master record.
     */
    @Column(
            name = "patient_ref_id",
            nullable = false,
            length = 50
    )
    private String patientRefId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private PatientTestResultStatus status =
            PatientTestResultStatus.DRAFT;

    /**
     * Version of the result.
     *
     * Used to support future result correction/versioning.
     */
    @Column(
            name = "result_version",
            nullable = false
    )
    @Builder.Default
    private Integer resultVersion = 1;

    /**
     * Time at which the test was performed/processed.
     */
    @Column(name = "performed_at")
    private Instant performedAt;

    /**
     * Time at which the result was finalized.
     */
    @Column(name = "finalized_at")
    private Instant finalizedAt;

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

    /**
     * Adds a parameter result to this test result
     * and synchronizes the owning side of the relationship.
     */
    public void addParameterResult(TestParameterResult parameterResult) {

        parameterResults.add(parameterResult);
        parameterResult.setPatientTestResult(this);
    }

    /**
     * Removes a parameter result from this test result
     * and synchronizes the owning side of the relationship.
     */
    public void removeParameterResult(TestParameterResult parameterResult) {

        parameterResults.remove(parameterResult);
        parameterResult.setPatientTestResult(null);
    }

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (refId == null || refId.isBlank()) {
            refId = RefIdGenerator.generate("PTR");
        }

        if (status == null) {
            status = PatientTestResultStatus.DRAFT;
        }

        if (resultVersion == null) {
            resultVersion = 1;
        }

        if (parameterResults == null) {
            parameterResults = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = Instant.now();
    }
}
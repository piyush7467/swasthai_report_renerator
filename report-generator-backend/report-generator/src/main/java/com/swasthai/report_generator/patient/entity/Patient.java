package com.swasthai.report_generator.patient.entity;

import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "patients",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_patient_ref_id",
                        columnNames = "ref_id"
                ),
                @UniqueConstraint(
                        name = "uk_patient_org_patient_code",
                        columnNames = {
                                "organization_id",
                                "patient_code"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_patient_organization",
                        columnList = "organization_id"
                ),
                @Index(
                        name = "idx_patient_org_name",
                        columnList = "organization_id, name"
                ),
                @Index(
                        name = "idx_patient_org_phone",
                        columnList = "organization_id, phone"
                ),
                @Index(
                        name = "idx_patient_org_email",
                        columnList = "organization_id, email"
                ),
                @Index(
                        name = "idx_patient_org_deleted_at",
                        columnList = "organization_id, deleted_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    /*
     * Internal database identifier.
     *
     * Never expose through the API.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Public immutable reference identifier.
     */
    @Column(
            name = "ref_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 30
    )
    private String refId;

    /*
     * Organization/tenant.
     *
     * Immutable from patient update APIs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_patient_organization"
            )
    )
    private Organization organization;

    /*
     * Human-friendly organization-scoped patient code.
     *
     * Example:
     * PAT-000001
     *
     * Immutable after creation.
     */
    @Column(
            name = "patient_code",
            nullable = false,
            length = 30,
            updatable = false
    )
    private String patientCode;

    /*
     * Salutation used for patient/report presentation.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private Salutation salutation;

    /*
     * Patient display name.
     */
    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    /*
     * Indicates whether exact DOB is known.
     *
     * true:
     *     dateOfBirth must be present.
     *
     * false:
     *     dateOfBirth must be null and manual age
     *     must be provided.
     */
    @Column(
            name = "date_of_birth_known",
            nullable = false
    )
    private boolean dateOfBirthKnown;

    /*
     * Exact date of birth when known.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /*
     * Approximate age when exact DOB is unknown.
     *
     * Examples:
     * 10 MONTHS
     * 2 WEEKS
     * 5 DAYS
     * 3 YEARS
     */
    @Column(name = "age_value")
    private Integer ageValue;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "age_unit",
            length = 10
    )
    private AgeUnit ageUnit;

    /*
     * Gender.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private Gender gender;

    /*
     * Optional phone number.
     */
    @Column(length = 30)
    private String phone;

    /*
     * Optional email.
     */
    @Column(length = 150)
    private String email;

    /*
     * Optional address.
     */
    @Column(length = 500)
    private String address;

    /*
     * Optional weight.
     *
     * Stored in kilograms.
     *
     * Example:
     * 10.250 kg
     */
    @Column(
            precision = 6,
            scale = 3
    )
    private BigDecimal weightKg;

    /*
     * Optimistic locking.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /*
     * Soft deletion timestamp.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /*
     * User who deleted the patient.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "deleted_by",
            foreignKey = @ForeignKey(
                    name = "fk_patient_deleted_by"
            )
    )
    private User deletedBy;

    /*
     * Creation timestamp.
     */
    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    /*
     * Last modification timestamp.
     */
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

        normalizeFields();
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = Instant.now();

        normalizeFields();
    }

    private void normalizeFields() {

        if (name != null) {
            name = name.trim();
        }

        if (phone != null) {
            phone = phone.trim();
        }

        if (email != null) {
            email = email
                    .trim()
                    .toLowerCase();
        }

        if (address != null) {
            address = address.trim();
        }

        if (patientCode != null) {
            patientCode = patientCode
                    .trim()
                    .toUpperCase();
        }
    }

    private String generateRefId() {
        return com.swasthai.report_generator.common.util.RefIdGenerator
                .generate("PAT");
    }
}
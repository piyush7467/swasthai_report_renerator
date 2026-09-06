package com.swasthai.report_generator.patient.entity;

import com.swasthai.report_generator.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;

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
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Public identifier.
     * Never expose the internal UUID.
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
     * Organization to which this patient belongs.
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
     * Human-friendly patient number generated
     * by the organization.
     *
     * Example:
     * PAT-000001
     */
    @Column(
            name = "patient_code",
            nullable = false,
            length = 30
    )
    private String patientCode;

    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    @Column(
            nullable = false
    )
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private Gender gender;

    @Column(
            length = 30
    )
    private String phone;

    @Column(
            length = 150
    )
    private String email;

    @Column(
            length = 500
    )
    private String address;

    /*
     * Used for optimistic locking.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(
            nullable = false,
            updatable = false
    )
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
            email = email.trim().toLowerCase();
        }

        if (address != null) {
            address = address.trim();
        }

        if (patientCode != null) {
            patientCode = patientCode.trim().toUpperCase();
        }
    }

    private String generateRefId() {
        return com.swasthai.report_generator.common.util.RefIdGenerator.generate("PAT");
    }
}
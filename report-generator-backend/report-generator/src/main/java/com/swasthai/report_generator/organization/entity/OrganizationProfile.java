package com.swasthai.report_generator.organization.entity;

import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.enums.SignatureVerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "organization_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_organization_profiles_organization",
                        columnNames = "organization_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * One profile belongs to exactly one organization.
     */
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_organization_profiles_organization"
            )
    )
    private Organization organization;

    /*
     * Organization branding
     *
     * This stores a reference/key to the logo in file storage.
     * The actual file is NOT stored in PostgreSQL.
     */
    @Column(
            name = "logo_storage_key",
            length = 500
    )
    private String logoStorageKey;

    /*
     * Organization address
     */
    @Column(
            name = "address_line1",
            length = 200
    )
    private String addressLine1;

    @Column(
            name = "address_line2",
            length = 200
    )
    private String addressLine2;

    @Column(
            name = "city",
            length = 100
    )
    private String city;

    @Column(
            name = "state",
            length = 100
    )
    private String state;

    @Column(
            name = "postal_code",
            length = 20
    )
    private String postalCode;

    @Column(
            name = "country",
            length = 100
    )
    private String country;

    /*
     * Organization contact information
     */
    @Column(
            name = "phone",
            length = 30
    )
    private String phone;

    @Column(
            name = "alternate_phone",
            length = 30
    )
    private String alternatePhone;

    @Column(
            name = "email",
            length = 150
    )
    private String email;

    @Column(
            name = "website",
            length = 255
    )
    private String website;

    /*
     * Organization-level report signature.
     *
     * The actual signature image is stored in file storage.
     */
    @Column(
            name = "signature_storage_key",
            length = 500
    )
    private String signatureStorageKey;

    /*
     * RefId of the current authorized ORG_ADMIN whose signature
     * is being used for organization reports.
     *
     * This is NOT a foreign key intentionally because the user
     * may later become inactive or change status while historical
     * reports must remain valid.
     *
     * Finalized reports snapshot the owner's name/email.
     */
    @Column(
            name = "signature_owner_ref_id",
            length = 50
    )
    private String signatureOwnerRefId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "signature_verification_status",
            length = 30
    )
    private SignatureVerificationStatus signatureVerificationStatus;

    @Column(
            name = "signature_verified_at"
    )
    private Instant signatureVerifiedAt;

    @Column(
            name = "signature_verified_by",
            length = 150
    )
    private String signatureVerifiedBy;

    @Column(
            name = "signature_rejection_reason",
            length = 500
    )
    private String signatureRejectionReason;

    public SignatureVerificationStatus getSignatureVerificationStatus() {
        if (signatureVerificationStatus == null) {
            return signatureStorageKey != null
                    ? SignatureVerificationStatus.APPROVED
                    : SignatureVerificationStatus.NOT_CONFIGURED;
        }
        return signatureVerificationStatus;
    }

    /*
     * Optional report footer configured by the organization.
     */
    @Column(
            name = "report_footer_text",
            length = 1000
    )
    private String reportFooterText;

    /*
     * Optional report disclaimer configured by the organization.
     */
    @Column(
            name = "report_disclaimer",
            length = 2000
    )
    private String reportDisclaimer;

    /*
     * Optimistic locking.
     *
     * Prevents concurrent profile updates from silently
     * overwriting one another.
     */
    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    /*
     * Audit timestamps.
     */
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

    /*
     * Entity lifecycle.
     */
    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (version == null) {
            version = 0L;
        }

        normalize();
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = Instant.now();

        normalize();
    }

    /*
     * Normalize optional string fields.
     *
     * Blank strings are converted to null.
     * This keeps the database clean and makes optional
     * fields easier to handle in the PDF renderer.
     */
    private void normalize() {

        logoStorageKey = normalizeNullable(logoStorageKey);

        addressLine1 = normalizeNullable(addressLine1);
        addressLine2 = normalizeNullable(addressLine2);
        city = normalizeNullable(city);
        state = normalizeNullable(state);
        postalCode = normalizeNullable(postalCode);
        country = normalizeNullable(country);

        phone = normalizeNullable(phone);
        alternatePhone = normalizeNullable(alternatePhone);
        email = normalizeNullable(email);
        website = normalizeNullable(website);

        signatureStorageKey = normalizeNullable(signatureStorageKey);
        signatureOwnerRefId = normalizeNullable(signatureOwnerRefId);

        reportFooterText = normalizeNullable(reportFooterText);
        reportDisclaimer = normalizeNullable(reportDisclaimer);
    }

    private String normalizeNullable(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }
}
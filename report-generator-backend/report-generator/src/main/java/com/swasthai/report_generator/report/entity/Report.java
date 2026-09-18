package com.swasthai.report_generator.report.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reports", uniqueConstraints = {
                @UniqueConstraint(name = "uk_reports_ref_id", columnNames = "ref_id")
},

                indexes = {
                                @Index(name = "idx_reports_organization", columnList = "organization_id"),
                                @Index(name = "idx_reports_patient", columnList = "patient_ref_id"),
                                @Index(name = "idx_reports_status", columnList = "status"),
                                @Index(name = "idx_reports_created_by", columnList = "created_by"),
                                @Index(name = "idx_reports_finalized_by", columnList = "finalized_by"),
                                @Index(name = "idx_reports_created_at", columnList = "created_at"),
                                @Index(name = "idx_reports_ref_id", columnList = "ref_id"),
                                @Index(name = "idx_reports_org_created", columnList = "organization_id, created_at DESC")
                })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @Column(name = "ref_id", nullable = false, unique = true, updatable = false, length = 30)
        private String refId;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "organization_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reports_organization"))
        private Organization organization;

        @Column(name = "patient_ref_id", nullable = false, length = 50)
        private String patientRefId;

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 20)
        @Builder.Default
        private ReportStatus status = ReportStatus.DRAFT;

        @Column(name = "report_version", nullable = false)
        @Builder.Default
        private Integer reportVersion = 1;

        @Column(name = "include_organization_header", nullable = false)
        @Builder.Default
        private Boolean includeOrganizationHeader = false;

        @Version
        @Column(name = "lock_version", nullable = false)
        @Builder.Default
        private Long lockVersion = 0L;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "fk_reports_created_by"))
        private User createdBy;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "finalized_by", foreignKey = @ForeignKey(name = "fk_reports_finalized_by"))
        private User finalizedBy;

        @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderBy("displayOrder ASC")
        @Builder.Default
        private List<ReportTestResult> tests = new ArrayList<>();

        @Column(name = "finalized_at")
        private Instant finalizedAt;

        @Column(name = "created_at", nullable = false, updatable = false)
        private Instant createdAt;

        @Column(name = "updated_at", nullable = false)
        private Instant updatedAt;

        public void addTest(ReportTestResult reportTest) {
                tests.add(reportTest);
                reportTest.setReport(this);
        }

        public void removeTest(ReportTestResult reportTest) {
                tests.remove(reportTest);
                reportTest.setReport(null);
        }

        @PrePersist
        protected void onCreate() {

                Instant now = Instant.now();

                createdAt = now;
                updatedAt = now;

                if (refId == null || refId.isBlank()) {
                        refId = RefIdGenerator.generate("RPT");
                }

                if (status == null) {
                        status = ReportStatus.DRAFT;
                }

                if (reportVersion == null) {
                        reportVersion = 1;
                }

                if (includeOrganizationHeader == null) {
                        includeOrganizationHeader = false;
                }

                if (lockVersion == null) {
                        lockVersion = 0L;
                }

                if (tests == null) {
                        tests = new ArrayList<>();
                }
        }

        // ============================================================
        // Historical Patient Snapshot
        // ============================================================

        @Column(name = "patient_name", length = 150)
        private String patientName;

        @Column(name = "patient_salutation", length = 20)
        private String patientSalutation;

        @Column(name = "patient_code", length = 30)
        private String patientCode;

        @Column(name = "patient_gender", length = 20)
        private String patientGender;

        @Column(name = "patient_date_of_birth_known")
        private Boolean patientDateOfBirthKnown;

        @Column(name = "patient_date_of_birth")
        private LocalDate patientDateOfBirth;

        @Column(name = "patient_age_at_reporting_value")
        private Integer patientAgeAtReportingValue;

        @Column(name = "patient_age_at_reporting_unit", length = 10)
        private String patientAgeAtReportingUnit;

        @Column(name = "patient_phone", length = 30)
        private String patientPhone;

        @Column(name = "patient_email", length = 150)
        private String patientEmail;

        @Column(name = "patient_address", length = 500)
        private String patientAddress;

        @Column(name = "patient_weight_kg", precision = 6, scale = 3)
        private BigDecimal patientWeightKg;

        // ============================================================
        // Historical Organization Snapshot
        // ============================================================

        @Column(name = "organization_name", length = 150)
        private String organizationName;

        @Column(name = "organization_code", length = 50)
        private String organizationCode;

        // ============================================================
        // Historical Creator Snapshot
        // ============================================================

        @Column(name = "created_by_name", length = 150)
        private String createdByName;

        @Column(name = "created_by_email", length = 255)
        private String createdByEmail;

        // ============================================================
        // Historical Finalizer Snapshot
        // ============================================================

        @Column(name = "finalized_by_name", length = 150)
        private String finalizedByName;

        @Column(name = "finalized_by_email", length = 255)
        private String finalizedByEmail;

        // ============================================================
        // Historical Organization Profile Snapshot
        // ============================================================

        @Column(name = "organization_address_line1", length = 200)
        private String organizationAddressLine1;

        @Column(name = "organization_address_line2", length = 200)
        private String organizationAddressLine2;

        @Column(name = "organization_city", length = 100)
        private String organizationCity;

        @Column(name = "organization_state", length = 100)
        private String organizationState;

        @Column(name = "organization_postal_code", length = 20)
        private String organizationPostalCode;

        @Column(name = "organization_country", length = 100)
        private String organizationCountry;

        @Column(name = "organization_phone", length = 30)
        private String organizationPhone;

        @Column(name = "organization_alternate_phone", length = 30)
        private String organizationAlternatePhone;

        @Column(name = "organization_email", length = 150)
        private String organizationEmail;

        @Column(name = "organization_website", length = 255)
        private String organizationWebsite;

        @Column(name = "organization_logo_storage_key", length = 500)
        private String organizationLogoStorageKey;

        @Column(name = "organization_signature_storage_key", length = 500)
        private String organizationSignatureStorageKey;

        @Column(name = "organization_signature_owner_name", length = 150)
        private String organizationSignatureOwnerName;

        @Column(name = "organization_signature_owner_email", length = 255)
        private String organizationSignatureOwnerEmail;

        @Column(name = "organization_report_footer_text", length = 1000)
        private String organizationReportFooterText;

        @Column(name = "organization_report_disclaimer", length = 2000)
        private String organizationReportDisclaimer;

        @PreUpdate
        protected void onUpdate() {

                updatedAt = Instant.now();
        }

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "deleted_by", foreignKey = @ForeignKey(name = "fk_reports_deleted_by"))
        private User deletedBy;

        @Column(name = "deleted_at")
        private Instant deletedAt;

}
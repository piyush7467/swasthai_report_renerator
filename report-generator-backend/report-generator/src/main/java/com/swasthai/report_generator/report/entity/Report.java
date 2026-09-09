package com.swasthai.report_generator.report.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reports_ref_id",
                        columnNames = "ref_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_reports_organization",
                        columnList = "organization_id"
                ),
                @Index(
                        name = "idx_reports_patient",
                        columnList = "patient_ref_id"
                ),
                @Index(
                        name = "idx_reports_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_reports_created_by",
                        columnList = "created_by"
                ),
                @Index(
                        name = "idx_reports_finalized_by",
                        columnList = "finalized_by"
                ),
                @Index(
                        name = "idx_reports_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_reports_ref_id",
                        columnList = "ref_id"
                ),
                @Index(
                        name = "idx_reports_org_created",
                        columnList = "organization_id, created_at DESC"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_reports_organization"
            )
    )
    private Organization organization;

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
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(
            name = "report_version",
            nullable = false
    )
    @Builder.Default
    private Integer reportVersion = 1;

    @Version
    @Column(
            name = "lock_version",
            nullable = false
    )
    @Builder.Default
    private Long lockVersion = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_reports_created_by"
            )
    )
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "finalized_by",
            foreignKey = @ForeignKey(
                    name = "fk_reports_finalized_by"
            )
    )
    private User finalizedBy;

    @OneToMany(
            mappedBy = "report",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<ReportTestResult> tests = new ArrayList<>();

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

        if (lockVersion == null) {
            lockVersion = 0L;
        }

        if (tests == null) {
            tests = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
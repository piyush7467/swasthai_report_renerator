package com.swasthai.report_generator.report.entity;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.test.entity.Test;
import com.swasthai.report_generator.test.entity.TestParameterResult;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "report_test_results", uniqueConstraints = {
                @UniqueConstraint(name = "uk_report_test_results_ref_id", columnNames = "ref_id"),
                @UniqueConstraint(name = "uk_report_test_results_report_test", columnNames = { "report_id", "test_id" })
}, indexes = {
                @Index(name = "idx_report_test_results_report", columnList = "report_id"),
                @Index(name = "idx_report_test_results_test", columnList = "test_id"),
                @Index(name = "idx_report_test_results_ref_id", columnList = "ref_id"),
                @Index(name = "idx_report_test_results_order", columnList = "report_id,display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTestResult {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @Column(name = "ref_id", nullable = false, unique = true, updatable = false, length = 30)
        private String refId;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "report_id", nullable = false, foreignKey = @ForeignKey(name = "fk_report_test_results_report"))
        private Report report;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "test_id", nullable = false, foreignKey = @ForeignKey(name = "fk_report_test_results_test"))
        private Test test;

        @Column(name = "display_order", nullable = false)
        private Integer displayOrder;

        @Column(name = "test_version", nullable = false)
        private Integer testVersion;

        @OneToMany(mappedBy = "reportTestResult", cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private List<TestParameterResult> parameterResults = new ArrayList<>();

        @Column(name = "created_at", nullable = false, updatable = false)
        private Instant createdAt;

        @Column(name = "updated_at", nullable = false)
        private Instant updatedAt;

        public void addParameterResult(
                        TestParameterResult parameterResult) {
                parameterResults.add(parameterResult);
                parameterResult.setReportTestResult(this);
        }

        public void removeParameterResult(
                        TestParameterResult parameterResult) {
                parameterResults.remove(parameterResult);
                parameterResult.setReportTestResult(null);
        }

        // ============================================================
        // Historical Test Snapshot
        // ============================================================

        @Column(name = "test_code", length = 50)
        private String testCode;

        @Column(name = "test_name", length = 150)
        private String testName;

        @Column(name = "test_short_name", length = 75)
        private String testShortName;

        @Column(name = "sample_type", length = 30)
        private String sampleType;

        @Column(name = "custom_sample_type", length = 100)
        private String customSampleType;

        @Column(name = "specimen_container", length = 150)
        private String specimenContainer;

        @Column(name = "report_section", length = 100)
        private String reportSection;

        @PrePersist
        protected void onCreate() {

                Instant now = Instant.now();

                createdAt = now;
                updatedAt = now;

                if (refId == null || refId.isBlank()) {
                        refId = RefIdGenerator.generate("RTR");
                }
        }

        @PreUpdate
        protected void onUpdate() {
                updatedAt = Instant.now();
        }
}
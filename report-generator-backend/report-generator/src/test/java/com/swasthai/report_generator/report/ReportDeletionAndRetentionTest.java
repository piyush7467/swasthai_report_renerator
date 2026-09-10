package com.swasthai.report_generator.report;

import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.config.ReportRetentionProperties;
import com.swasthai.report_generator.report.dto.request.*;
import com.swasthai.report_generator.report.dto.response.BulkDeleteReportsResponse;
import com.swasthai.report_generator.report.dto.response.DeleteReportResponse;
import com.swasthai.report_generator.report.dto.response.ReportResponse;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.scheduler.ReportPurgeScheduler;
import com.swasthai.report_generator.report.service.ReportService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ReportDeletionAndRetentionTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportPurgeScheduler reportPurgeScheduler;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ReportRetentionProperties retentionProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Organization orgA;
    private Organization orgB;

    private User orgAdminA;
    private User labStaffA;
    private User orgAdminB;
    private User superAdmin;

    private Patient patientA;
    private Patient patientB;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        orgA = organizationRepository.save(Organization.builder()
                .name("Alpha Diagnostics " + suffix)
                .code("ALPHA-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(Organization.builder()
                .name("Beta Diagnostics " + suffix)
                .code("BETA-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgAdminA = userRepository.save(User.builder()
                .email("admin-a-" + suffix + "@alpha.com")
                .name("Admin Alpha")
                .passwordHash("hashed")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build());

        labStaffA = userRepository.save(User.builder()
                .email("staff-a-" + suffix + "@alpha.com")
                .name("Staff Alpha")
                .passwordHash("hashed")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build());

        orgAdminB = userRepository.save(User.builder()
                .email("admin-b-" + suffix + "@beta.com")
                .name("Admin Beta")
                .passwordHash("hashed")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(orgB)
                .build());

        superAdmin = userRepository.findByEmailIgnoreCase("admin@swasthai.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("superadmin-" + suffix + "@swasthai.com")
                        .name("Super Admin")
                        .passwordHash("hashed")
                        .role(Role.SUPER_ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build()));

        patientA = patientRepository.save(Patient.builder()
                .organization(orgA)
                .name("Patient Alpha")
                .patientCode("PA-" + suffix)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .build());

        patientB = patientRepository.save(Patient.builder()
                .organization(orgB)
                .name("Patient Beta")
                .patientCode("PB-" + suffix)
                .dateOfBirth(LocalDate.of(1992, 2, 2))
                .gender(Gender.FEMALE)
                .build());
    }

    private void authenticateUser(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        auth.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void backdateReportCreatedAt(String reportRefId, Instant createdAt) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            entityManager.flush();
        }
        jdbcTemplate.update(
                "UPDATE reports SET created_at = ? WHERE ref_id = ?",
                Timestamp.from(createdAt),
                reportRefId
        );
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            entityManager.clear();
        }
    }

    private void backdateReportDeletedAt(String reportRefId, Instant deletedAt) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            entityManager.flush();
        }
        jdbcTemplate.update(
                "UPDATE reports SET deleted_at = ? WHERE ref_id = ?",
                Timestamp.from(deletedAt),
                reportRefId
        );
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            entityManager.clear();
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        try {
            if (orgA != null && orgB != null) {
                jdbcTemplate.update("DELETE FROM test_parameter_results WHERE report_test_result_id IN (SELECT id FROM report_test_results WHERE report_id IN (SELECT id FROM reports WHERE organization_id IN (?, ?)))", orgA.getId(), orgB.getId());
                jdbcTemplate.update("DELETE FROM report_test_results WHERE report_id IN (SELECT id FROM reports WHERE organization_id IN (?, ?))", orgA.getId(), orgB.getId());
                jdbcTemplate.update("DELETE FROM reports WHERE organization_id IN (?, ?)", orgA.getId(), orgB.getId());
                jdbcTemplate.update("DELETE FROM patients WHERE organization_id IN (?, ?)", orgA.getId(), orgB.getId());
                jdbcTemplate.update("DELETE FROM users WHERE organization_id IN (?, ?)", orgA.getId(), orgB.getId());
                jdbcTemplate.update("DELETE FROM organizations WHERE id IN (?, ?)", orgA.getId(), orgB.getId());
            }
        } catch (Exception ignored) {
        }
    }

    // ==========================================
    // 1. SINGLE REPORT SOFT-DELETE
    // ==========================================

    @Test
    @Transactional
    @DisplayName("Single Report Soft-Delete: Success when ORG_ADMIN deletes an eligible report")
    void testSingleReportSoftDelete_Success() {
        authenticateUser(labStaffA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientA.getRefId()));

        // Backdate report created_at to pass deleteAfterDays retention requirement
        Instant eligibleTime = Instant.now().minus(retentionProperties.getDeleteAfterDays() + 1, ChronoUnit.DAYS);
        backdateReportCreatedAt(report.refId(), eligibleTime);

        // Perform deletion as ORG_ADMIN
        authenticateUser(orgAdminA);
        DeleteReportResponse deleteResponse = reportService.deleteReport(report.refId());

        assertThat(deleteResponse).isNotNull();
        assertThat(deleteResponse.reportRefId()).isEqualTo(report.refId());
        assertThat(deleteResponse.deletedAt()).isNotNull();

        // Verify entity in repository has soft-delete fields set
        Report entity = reportRepository.findByRefId(report.refId()).orElseThrow();
        assertThat(entity.getDeletedAt()).isNotNull();
        assertThat(entity.getDeletedBy()).isNotNull();
        assertThat(entity.getDeletedBy().getId()).isEqualTo(orgAdminA.getId());

        // Verify soft-deleted report is not accessible via getReport
        assertThatThrownBy(() -> reportService.getReport(report.refId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report not found");

        // Verify soft-deleted report is excluded from getMyReports
        var reportsPage = reportService.getMyReports(null, PageRequest.of(0, 10));
        assertThat(reportsPage.getContent().stream().noneMatch(r -> r.refId().equals(report.refId()))).isTrue();
    }

    @Test
    @DisplayName("Single Report Soft-Delete: Fails safely on concurrent optimistic lock version conflict")
    void testSingleReportSoftDelete_OptimisticLockingConflict() {
        authenticateUser(labStaffA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientA.getRefId()));

        Instant eligibleTime = Instant.now().minus(retentionProperties.getDeleteAfterDays() + 1, ChronoUnit.DAYS);
        backdateReportCreatedAt(report.refId(), eligibleTime);

        // Load entity into current persistence context with current lock_version
        Report loadedReport = reportRepository.findByRefId(report.refId()).orElseThrow();

        // Simulate concurrent transaction incrementing lock_version in DB
        jdbcTemplate.update(
                "UPDATE reports SET lock_version = lock_version + 1 WHERE ref_id = ?",
                report.refId()
        );

        // Attempting to delete using the stale entity triggers optimistic locking failure
        loadedReport.setDeletedAt(Instant.now());
        loadedReport.setDeletedBy(orgAdminA);

        assertThatThrownBy(() -> reportRepository.saveAndFlush(loadedReport))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Single Report Soft-Delete: Ineligible when report has not reached deleteAfterDays retention cutoff")
    void testSingleReportSoftDelete_IneligibleBeforeRetentionCutoff() {
        authenticateUser(labStaffA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientA.getRefId()));

        // When deleteAfterDays > 0, freshly created report cannot be deleted
        if (retentionProperties.getDeleteAfterDays() > 0) {
            authenticateUser(orgAdminA);
            assertThatThrownBy(() -> reportService.deleteReport(report.refId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Report has not reached the deletion eligibility period");

            // Verify report is NOT deleted
            Report entity = reportRepository.findByRefId(report.refId()).orElseThrow();
            assertThat(entity.getDeletedAt()).isNull();
            assertThat(entity.getDeletedBy()).isNull();
        }
    }

    @Test
    @DisplayName("Single Report Soft-Delete: Rejected with 403 Forbidden for LAB_STAFF")
    void testSingleReportSoftDelete_ForbiddenForLabStaff() {
        authenticateUser(labStaffA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientA.getRefId()));

        Instant eligibleTime = Instant.now().minus(retentionProperties.getDeleteAfterDays() + 1, ChronoUnit.DAYS);
        backdateReportCreatedAt(report.refId(), eligibleTime);

        assertThatThrownBy(() -> reportService.deleteReport(report.refId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only organization administrators can delete reports");
    }

    @Test
    @DisplayName("Single Report Soft-Delete: Rejected with 403 Forbidden for SUPER_ADMIN")
    void testSingleReportSoftDelete_ForbiddenForSuperAdmin() {
        authenticateUser(labStaffA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientA.getRefId()));

        Instant eligibleTime = Instant.now().minus(retentionProperties.getDeleteAfterDays() + 1, ChronoUnit.DAYS);
        backdateReportCreatedAt(report.refId(), eligibleTime);

        authenticateUser(superAdmin);
        assertThatThrownBy(() -> reportService.deleteReport(report.refId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only organization administrators can delete reports");
    }

    @Test
    @DisplayName("Single Report Soft-Delete: Cross-Organization attempt returns 404 without leaking existence")
    void testSingleReportSoftDelete_CrossOrganizationReturnsNotFound() {
        authenticateUser(labStaffA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientA.getRefId()));

        Instant eligibleTime = Instant.now().minus(retentionProperties.getDeleteAfterDays() + 1, ChronoUnit.DAYS);
        backdateReportCreatedAt(report.refId(), eligibleTime);

        // Org Admin B attempts to delete Org A's report
        authenticateUser(orgAdminB);
        assertThatThrownBy(() -> reportService.deleteReport(report.refId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report not found");

        // Verify report was NOT deleted
        Report entity = reportRepository.findByRefId(report.refId()).orElseThrow();
        assertThat(entity.getDeletedAt()).isNull();
    }

    // ==========================================
    // 2. SELECTED BULK SOFT-DELETE
    // ==========================================

    @Test
    @DisplayName("Bulk Soft-Delete: ORG_ADMIN deletes multiple reports, skipping ineligible and foreign-org reports")
    void testBulkSoftDelete_SelectedReports() {
        authenticateUser(labStaffA);
        ReportResponse reportA1 = reportService.createReport(new CreateReportRequest(patientA.getRefId()));
        ReportResponse reportA2 = reportService.createReport(new CreateReportRequest(patientA.getRefId()));
        ReportResponse reportA3_ineligible = reportService.createReport(new CreateReportRequest(patientA.getRefId()));

        // Org B report
        authenticateUser(orgAdminB);
        ReportResponse reportB = reportService.createReport(new CreateReportRequest(patientB.getRefId()));

        // Backdate A1 and A2 and B to be eligible
        Instant eligibleTime = Instant.now().minus(retentionProperties.getDeleteAfterDays() + 1, ChronoUnit.DAYS);
        backdateReportCreatedAt(reportA1.refId(), eligibleTime);
        backdateReportCreatedAt(reportA2.refId(), eligibleTime);
        backdateReportCreatedAt(reportB.refId(), eligibleTime);

        // Admin A calls bulk delete with A1, A2, duplicate A1, ineligible A3, foreign B, and non-existent refId
        authenticateUser(orgAdminA);
        BulkDeleteReportsRequest request = new BulkDeleteReportsRequest(List.of(
                reportA1.refId(),
                reportA2.refId(),
                reportA1.refId(), // duplicate
                reportA3_ineligible.refId(),
                reportB.refId(),
                "RPT_NON_EXISTENT"
        ));

        BulkDeleteReportsResponse response = reportService.deleteReports(request);

        // Unique requested refIds = 5 (A1, A2, A3_ineligible, B, NON_EXISTENT)
        // Eligible deleted = 2 (A1, A2)
        // Skipped = 5 - 2 = 3
        assertThat(response.deletedCount()).isEqualTo(2);
        assertThat(response.skippedCount()).isEqualTo(3);

        // Verify A1 and A2 soft deleted
        Report entityA1 = reportRepository.findByRefId(reportA1.refId()).orElseThrow();
        Report entityA2 = reportRepository.findByRefId(reportA2.refId()).orElseThrow();
        assertThat(entityA1.getDeletedAt()).isNotNull();
        assertThat(entityA1.getDeletedBy().getId()).isEqualTo(orgAdminA.getId());
        assertThat(entityA2.getDeletedAt()).isNotNull();

        // Verify foreign Org B report was untouched
        Report entityB = reportRepository.findByRefId(reportB.refId()).orElseThrow();
        assertThat(entityB.getDeletedAt()).isNull();

        // Verify ineligible A3 was untouched
        Report entityA3 = reportRepository.findByRefId(reportA3_ineligible.refId()).orElseThrow();
        assertThat(entityA3.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("Bulk Soft-Delete: Chunks large requests across multiple infrastructure batches")
    void testBulkSoftDelete_MultiBatchProcessing() {
        int originalBatchSize = retentionProperties.getBulkBatchSize();
        retentionProperties.setBulkBatchSize(2);

        try {
            authenticateUser(labStaffA);
            List<String> refIds = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                ReportResponse r = reportService.createReport(new CreateReportRequest(patientA.getRefId()));
                refIds.add(r.refId());
                Instant eligibleTime = Instant.now().minus(retentionProperties.getDeleteAfterDays() + 1, ChronoUnit.DAYS);
                backdateReportCreatedAt(r.refId(), eligibleTime);
            }

            // Add duplicates and whitespace
            List<String> requestIds = new ArrayList<>(refIds);
            requestIds.add("  " + refIds.get(0) + "  ");
            requestIds.add(refIds.get(1));

            authenticateUser(orgAdminA);
            BulkDeleteReportsResponse response = reportService.deleteReports(new BulkDeleteReportsRequest(requestIds));

            assertThat(response.deletedCount()).isEqualTo(5);
            assertThat(response.skippedCount()).isEqualTo(0);

            for (String refId : refIds) {
                Report entity = reportRepository.findByRefId(refId).orElseThrow();
                assertThat(entity.getDeletedAt()).isNotNull();
            }
        } finally {
            retentionProperties.setBulkBatchSize(originalBatchSize);
        }
    }

    @Test
    @DisplayName("Bulk Soft-Delete: Forbidden for LAB_STAFF")
    void testBulkSoftDelete_ForbiddenForLabStaff() {
        authenticateUser(labStaffA);
        BulkDeleteReportsRequest request = new BulkDeleteReportsRequest(List.of("RPT_123"));

        assertThatThrownBy(() -> reportService.deleteReports(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only organization administrators can delete reports");
    }

    // ==========================================
    // 3. DATE-RANGE BULK SOFT-DELETE
    // ==========================================

    @Test
    @DisplayName("Date-Range Soft-Delete: Deletes eligible reports in date range, bounded by retention cutoff")
    void testDeleteReportsByDateRange() {
        authenticateUser(labStaffA);
        ReportResponse reportOld = reportService.createReport(new CreateReportRequest(patientA.getRefId()));
        ReportResponse reportTarget = reportService.createReport(new CreateReportRequest(patientA.getRefId()));
        ReportResponse reportNew = reportService.createReport(new CreateReportRequest(patientA.getRefId()));

        ZoneId zone = ZoneId.of(retentionProperties.getTimeZone());
        LocalDate targetDate = LocalDate.now(zone).minusDays(retentionProperties.getDeleteAfterDays() + 3);

        Instant targetInstant = targetDate.atTime(12, 0).atZone(zone).toInstant();
        Instant oldInstant = targetDate.minusDays(5).atTime(12, 0).atZone(zone).toInstant();
        Instant newInstant = Instant.now();

        backdateReportCreatedAt(reportOld.refId(), oldInstant);
        backdateReportCreatedAt(reportTarget.refId(), targetInstant);
        backdateReportCreatedAt(reportNew.refId(), newInstant);

        authenticateUser(orgAdminA);
        DeleteReportsByDateRangeRequest request = new DeleteReportsByDateRangeRequest(targetDate, targetDate);

        BulkDeleteReportsResponse response = reportService.deleteReportsByDateRange(request);

        assertThat(response.deletedCount()).isEqualTo(1);

        Report entityTarget = reportRepository.findByRefId(reportTarget.refId()).orElseThrow();
        assertThat(entityTarget.getDeletedAt()).isNotNull();

        Report entityOld = reportRepository.findByRefId(reportOld.refId()).orElseThrow();
        assertThat(entityOld.getDeletedAt()).isNull();

        Report entityNew = reportRepository.findByRefId(reportNew.refId()).orElseThrow();
        assertThat(entityNew.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("Date-Range Soft-Delete: Chunks across batches and correctly counts young reports as skipped")
    void testDeleteReportsByDateRange_MultiBatchAndSkippedCount() {
        int originalBatchSize = retentionProperties.getBulkBatchSize();
        retentionProperties.setBulkBatchSize(2);

        try {
            authenticateUser(labStaffA);
            ZoneId zone = ZoneId.of(retentionProperties.getTimeZone());
            LocalDate targetDate = LocalDate.now(zone).minusDays(retentionProperties.getDeleteAfterDays() + 3);

            // 3 eligible reports on targetDate
            List<String> eligibleRefIds = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                ReportResponse r = reportService.createReport(new CreateReportRequest(patientA.getRefId()));
                eligibleRefIds.add(r.refId());
                backdateReportCreatedAt(r.refId(), targetDate.atTime(10 + i, 0).atZone(zone).toInstant());
            }

            // 2 young reports on LocalDate.now()
            List<String> youngRefIds = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                ReportResponse r = reportService.createReport(new CreateReportRequest(patientA.getRefId()));
                youngRefIds.add(r.refId());
            }

            authenticateUser(orgAdminA);
            DeleteReportsByDateRangeRequest request = new DeleteReportsByDateRangeRequest(
                    targetDate,
                    LocalDate.now(zone)
            );

            BulkDeleteReportsResponse response = reportService.deleteReportsByDateRange(request);

            assertThat(response.deletedCount()).isEqualTo(3);
            assertThat(response.skippedCount()).isGreaterThanOrEqualTo(2);

            for (String refId : eligibleRefIds) {
                Report entity = reportRepository.findByRefId(refId).orElseThrow();
                assertThat(entity.getDeletedAt()).isNotNull();
            }

            for (String refId : youngRefIds) {
                Report entity = reportRepository.findByRefId(refId).orElseThrow();
                assertThat(entity.getDeletedAt()).isNull();
            }
        } finally {
            retentionProperties.setBulkBatchSize(originalBatchSize);
        }
    }

    @Test
    @DisplayName("Date-Range Soft-Delete: Rejects invalid date range where from is after to")
    void testDeleteReportsByDateRange_InvalidRange() {
        authenticateUser(orgAdminA);
        DeleteReportsByDateRangeRequest request = new DeleteReportsByDateRangeRequest(
                LocalDate.now().plusDays(2),
                LocalDate.now()
        );

        assertThatThrownBy(() -> reportService.deleteReportsByDateRange(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("From date cannot be after To date");
    }

    // ==========================================
    // 4. AUTOMATED HARD PURGE
    // ==========================================

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("Automated Purge: Hard deletes expired soft-deleted reports and cascades without affecting master data")
    void testPurgeExpiredReports() {
        authenticateUser(labStaffA);
        ReportResponse reportExpired = reportService.createReport(new CreateReportRequest(patientA.getRefId()));
        ReportResponse reportRecentDeleted = reportService.createReport(new CreateReportRequest(patientA.getRefId()));
        ReportResponse reportActive = reportService.createReport(new CreateReportRequest(patientA.getRefId()));

        // Mark reportExpired as soft-deleted older than purgeAfterDays
        Instant expiredDeletedAt = Instant.now().minus(retentionProperties.getPurgeAfterDays() + 2, ChronoUnit.DAYS);
        backdateReportCreatedAt(reportExpired.refId(), expiredDeletedAt.minus(5, ChronoUnit.DAYS));
        backdateReportDeletedAt(reportExpired.refId(), expiredDeletedAt);

        // Mark reportRecentDeleted as soft-deleted just 1 day ago (not expired for purge)
        Instant recentDeletedAt = Instant.now().minus(1, ChronoUnit.DAYS);
        backdateReportCreatedAt(reportRecentDeleted.refId(), recentDeletedAt.minus(5, ChronoUnit.DAYS));
        backdateReportDeletedAt(reportRecentDeleted.refId(), recentDeletedAt);

        // reportActive has deleted_at = NULL

        // Execute purge
        int purgedCount = reportService.purgeExpiredReports();
        assertThat(purgedCount).isGreaterThanOrEqualTo(1);

        // Verify expired report is PERMANENTLY deleted from DB
        Optional<Report> purgedEntity = reportRepository.findByRefId(reportExpired.refId());
        assertThat(purgedEntity).isEmpty();

        // Verify recent soft-deleted report is NOT purged
        Optional<Report> recentEntity = reportRepository.findByRefId(reportRecentDeleted.refId());
        assertThat(recentEntity).isPresent();

        // Verify active report is NOT purged
        Optional<Report> activeEntity = reportRepository.findByRefId(reportActive.refId());
        assertThat(activeEntity).isPresent();

        // Verify master data (Patient, User, Organization) is completely intact!
        assertThat(patientRepository.findById(patientA.getId())).isPresent();
        assertThat(userRepository.findById(orgAdminA.getId())).isPresent();
        assertThat(organizationRepository.findById(orgA.getId())).isPresent();
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("Automated Purge: Processes multiple purge batches in independent transactions")
    void testPurgeExpiredReports_MultiBatchProcessing() {
        int originalBatchSize = retentionProperties.getPurgeBatchSize();
        retentionProperties.setPurgeBatchSize(2);

        try {
            authenticateUser(labStaffA);
            Instant expiredDeletedAt = Instant.now().minus(retentionProperties.getPurgeAfterDays() + 2, ChronoUnit.DAYS);

            List<String> expiredRefIds = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                ReportResponse r = reportService.createReport(new CreateReportRequest(patientA.getRefId()));
                expiredRefIds.add(r.refId());
                backdateReportCreatedAt(r.refId(), expiredDeletedAt.minus(5, ChronoUnit.DAYS));
                backdateReportDeletedAt(r.refId(), expiredDeletedAt);
            }

            int purgedCount = reportService.purgeExpiredReports();
            assertThat(purgedCount).isGreaterThanOrEqualTo(5);

            for (String refId : expiredRefIds) {
                assertThat(reportRepository.findByRefId(refId)).isEmpty();
            }
        } finally {
            retentionProperties.setPurgeBatchSize(originalBatchSize);
        }
    }

    @Test
    @DisplayName("Automated Purge: Scheduler execution runs safely with concurrency guard")
    void testPurgeScheduler_Execution() {
        reportPurgeScheduler.purgeExpiredReports();
    }

    // ==========================================
    // 5. SOFT-DELETED INVISIBILITY ACROSS ALL APIS
    // ==========================================

    @Test
    @DisplayName("Invisibility: Soft-deleted report cannot be accessed, modified, or finalized by any API")
    void testSoftDeletedReportInvisibility_AcrossAllOperations() {
        authenticateUser(labStaffA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientA.getRefId()));

        Instant eligibleTime = Instant.now().minus(retentionProperties.getDeleteAfterDays() + 1, ChronoUnit.DAYS);
        backdateReportCreatedAt(report.refId(), eligibleTime);

        // Soft delete the report
        authenticateUser(orgAdminA);
        reportService.deleteReport(report.refId());

        // 1. GET report -> 404
        assertThatThrownBy(() -> reportService.getReport(report.refId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // 2. GET my reports -> excluded
        var page = reportService.getMyReports(null, PageRequest.of(0, 10));
        assertThat(page.getContent().stream().noneMatch(r -> r.refId().equals(report.refId()))).isTrue();

        // 3. Add test -> 404
        assertThatThrownBy(() -> reportService.addTest(report.refId(), new AddReportTestRequest("TEST_X", 0L)))
                .isInstanceOf(ResourceNotFoundException.class);

        // 4. Remove test -> 404
        assertThatThrownBy(() -> reportService.removeTest(report.refId(), "RTR_X"))
                .isInstanceOf(ResourceNotFoundException.class);

        // 5. Update parameters -> 404
        assertThatThrownBy(() -> reportService.updateParameterValues(
                report.refId(),
                "RTR_X",
                new UpdateReportParametersRequest(0L, List.of(new com.swasthai.report_generator.test.dto.request.TestParameterResultInput("PRM_X", null, "10")))))
                .isInstanceOf(ResourceNotFoundException.class);

        // 6. Reorder tests -> 404
        assertThatThrownBy(() -> reportService.reorderTests(
                report.refId(),
                new ReorderReportTestsRequest(0L, List.of(new TestOrderItemInput("RTR_X", 1)))))
                .isInstanceOf(ResourceNotFoundException.class);

        // 7. Finalize report -> 404
        assertThatThrownBy(() -> reportService.finalizeReport(report.refId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

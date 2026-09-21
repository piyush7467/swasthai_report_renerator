package com.swasthai.report_generator.admin;

import com.swasthai.report_generator.admin.analytics.dto.AdminOverviewStatsResponse;
import com.swasthai.report_generator.admin.analytics.dto.OrganizationReportActivityResponse;
import com.swasthai.report_generator.admin.analytics.dto.ReportTrendPointResponse;
import com.swasthai.report_generator.admin.analytics.dto.TestUsageStatsResponse;
import com.swasthai.report_generator.admin.analytics.service.AdminAnalyticsService;
import com.swasthai.report_generator.admin.audit.dto.SecurityAuditLogResponse;
import com.swasthai.report_generator.admin.audit.service.AdminAuditService;
import com.swasthai.report_generator.common.response.PageResponse;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.entity.Salutation;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.entity.ReportTestResult;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.security.audit.entity.SecurityAuditLog;
import com.swasthai.report_generator.security.audit.repository.SecurityAuditLogRepository;
import com.swasthai.report_generator.test.entity.TestCategory;
import com.swasthai.report_generator.test.entity.TestCategoryStatus;
import com.swasthai.report_generator.test.entity.TestStatus;
import com.swasthai.report_generator.test.entity.TestType;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AdminAnalyticsServiceTest {

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    @Autowired
    private AdminAuditService adminAuditService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private TestCategoryRepository testCategoryRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private SecurityAuditLogRepository securityAuditLogRepository;

    private Organization testOrg;
    private User testUser;
    private Patient testPatient;
    private com.swasthai.report_generator.test.entity.Test clinicalTest;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        testOrg = organizationRepository.save(Organization.builder()
                .name("Analytics Service Lab " + suffix)
                .code("ASL-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        testUser = userRepository.save(User.builder()
                .email("analyst-" + suffix + "@lab.com")
                .name("Analyst " + suffix)
                .passwordHash("secret")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(testOrg)
                .build());

        testPatient = patientRepository.save(Patient.builder()
                .organization(testOrg)
                .salutation(Salutation.MR)
                .name("John Doe")
                .patientCode("PT-" + suffix)
                .dateOfBirthKnown(true)
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .phone("9998887771")
                .build());

        TestCategory cat = testCategoryRepository.save(TestCategory.builder()
                .code("CAT-" + suffix)
                .name("Hematology " + suffix)
                .status(TestCategoryStatus.ACTIVE)
                .build());

        clinicalTest = testRepository.save(com.swasthai.report_generator.test.entity.Test.builder()
                .category(cat)
                .code("CBC-" + suffix)
                .name("Complete Blood Count " + suffix)
                .shortName("CBC")
                .testType(TestType.PANEL)
                .sampleType(com.swasthai.report_generator.test.entity.SampleType.WHOLE_BLOOD)
                .basePrice(new BigDecimal("500.00"))
                .currency("INR")
                .status(TestStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Overview stats correctly calculates active reports and excludes soft-deleted reports")
    @WithMockUser(username = "admin@swasthai.com", roles = {"SUPER_ADMIN"})
    void testOverviewStatsCalculation() {
        // Create 1 active finalized report
        Report r1 = reportRepository.save(Report.builder()
                .organization(testOrg)
                .patientRefId(testPatient.getRefId())
                .status(ReportStatus.FINALIZED)
                .createdBy(testUser)
                .finalizedBy(testUser)
                .finalizedAt(Instant.now())
                .build());

        // Create 1 active draft report
        Report r2 = reportRepository.save(Report.builder()
                .organization(testOrg)
                .patientRefId(testPatient.getRefId())
                .status(ReportStatus.DRAFT)
                .createdBy(testUser)
                .build());

        // Create 1 soft-deleted report
        Report r3 = reportRepository.save(Report.builder()
                .organization(testOrg)
                .patientRefId(testPatient.getRefId())
                .status(ReportStatus.DRAFT)
                .createdBy(testUser)
                .deletedBy(testUser)
                .deletedAt(Instant.now())
                .build());

        AdminOverviewStatsResponse stats = adminAnalyticsService.getOverviewStats();

        assertThat(stats.getTotalReports()).isGreaterThanOrEqualTo(2);
        assertThat(stats.getFinalizedReports()).isGreaterThanOrEqualTo(1);
        assertThat(stats.getDraftReports()).isGreaterThanOrEqualTo(1);
        assertThat(stats.getTotalDeletedReports()).isGreaterThanOrEqualTo(1);
        assertThat(stats.getDeletedReportsToday()).isGreaterThanOrEqualTo(1);
        assertThat(stats.getActiveOrganizationsCount()).isGreaterThanOrEqualTo(1);
        assertThat(stats.getOrganizationsWithReportsCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Report trend returns a continuous series for the specified number of days")
    @WithMockUser(username = "admin@swasthai.com", roles = {"SUPER_ADMIN"})
    void testReportTrendContinuousSeries() {
        List<ReportTrendPointResponse> trend = adminAnalyticsService.getReportTrend(7, null, null, null);

        assertThat(trend).hasSize(7);
        for (ReportTrendPointResponse point : trend) {
            assertThat(point.getDate()).isNotNull();
            assertThat(point.getTotalCount()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("Organization report activity returns paginated statistics")
    @WithMockUser(username = "admin@swasthai.com", roles = {"SUPER_ADMIN"})
    void testOrganizationReportActivity() {
        PageResponse<OrganizationReportActivityResponse> response = adminAnalyticsService.getOrganizationReportActivity(
                testOrg.getName(),
                0,
                10,
                "totalReports",
                "desc"
        );

        assertThat(response.getContent()).isNotEmpty();
        OrganizationReportActivityResponse orgStat = response.getContent().get(0);
        assertThat(orgStat.getOrganizationRefId()).isEqualTo(testOrg.getRefId());
        assertThat(orgStat.getOrganizationName()).isEqualTo(testOrg.getName());
    }

    @Test
    @DisplayName("Test usage aggregates utilization counts")
    @WithMockUser(username = "admin@swasthai.com", roles = {"SUPER_ADMIN"})
    void testTestUsageStats() {
        Report report = reportRepository.save(Report.builder()
                .organization(testOrg)
                .patientRefId(testPatient.getRefId())
                .status(ReportStatus.FINALIZED)
                .createdBy(testUser)
                .build());

        ReportTestResult rtr = ReportTestResult.builder()
                .report(report)
                .test(clinicalTest)
                .displayOrder(1)
                .testVersion(1)
                .testCode(clinicalTest.getCode())
                .testName(clinicalTest.getName())
                .build();
        report.addTest(rtr);
        reportRepository.save(report);

        List<TestUsageStatsResponse> usage = adminAnalyticsService.getTestUsage(10, null);

        assertThat(usage).isNotEmpty();
        boolean found = usage.stream().anyMatch(u -> u.getTestRefId().equals(clinicalTest.getRefId()));
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("Security audit service returns paginated and filtered logs")
    @WithMockUser(username = "admin@swasthai.com", roles = {"SUPER_ADMIN"})
    void testSecurityAuditService() {
        securityAuditLogRepository.save(SecurityAuditLog.builder()
                .actor(testUser)
                .actorEmail(testUser.getEmail())
                .action("BREAK_GLASS_REPORT_ACCESS")
                .targetOrganizationRefId(testOrg.getRefId())
                .targetReportRefId("RPT-TEST-123")
                .justification("Emergency patient verification")
                .success(true)
                .createdAt(Instant.now())
                .build());

        PageResponse<SecurityAuditLogResponse> logs = adminAuditService.getSecurityAuditLogs(
                "BREAK_GLASS_REPORT_ACCESS",
                testOrg.getRefId(),
                true,
                null,
                null,
                0,
                10,
                "createdAt",
                "desc"
        );

        assertThat(logs.getContent()).isNotEmpty();
        SecurityAuditLogResponse first = logs.getContent().get(0);
        assertThat(first.getAction()).isEqualTo("BREAK_GLASS_REPORT_ACCESS");
        assertThat(first.getTargetOrganizationRefId()).isEqualTo(testOrg.getRefId());
        assertThat(first.getTargetOrganizationName()).isEqualTo(testOrg.getName());
        assertThat(first.isSuccess()).isTrue();
    }
}

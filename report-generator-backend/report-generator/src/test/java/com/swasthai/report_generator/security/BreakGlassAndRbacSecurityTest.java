package com.swasthai.report_generator.security;

import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.license.entity.License;
import com.swasthai.report_generator.license.entity.LicenseStatus;
import com.swasthai.report_generator.license.entity.Plan;
import com.swasthai.report_generator.license.repository.LicenseRepository;
import com.swasthai.report_generator.license.repository.PlanRepository;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.entity.Salutation;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.dto.request.BreakGlassAccessRequest;
import com.swasthai.report_generator.report.dto.request.CreateReportRequest;
import com.swasthai.report_generator.report.dto.response.ReportResponse;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.service.ReportService;
import com.swasthai.report_generator.security.audit.entity.SecurityAuditLog;
import com.swasthai.report_generator.security.audit.repository.SecurityAuditLogRepository;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BreakGlassAndRbacSecurityTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private SecurityAuditLogRepository auditLogRepository;

    private Organization testOrg;
    private Organization otherOrg;

    private User superAdminUser;
    private User orgAdminUser;
    private User labStaffUser;

    private Patient testPatient;
    private Report testReport;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        testOrg = organizationRepository.save(Organization.builder()
                .name("Target Lab " + suffix)
                .code("TGT-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        otherOrg = organizationRepository.save(Organization.builder()
                .name("Other Lab " + suffix)
                .code("OTH-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        Plan plan = planRepository.save(Plan.builder()
                .code("PLAN-BG-" + suffix)
                .name("Plan BG")
                .annualPrice(new BigDecimal("1000.00"))
                .currency("INR")
                .active(true)
                .build());

        licenseRepository.save(License.builder()
                .organization(testOrg)
                .plan(plan)
                .status(LicenseStatus.ACTIVE)
                .startedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().plusSeconds(86400 * 365))
                .build());

        superAdminUser = userRepository.findByEmailIgnoreCase("admin@swasthai.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("super-" + suffix + "@admin.com")
                        .name("Global Admin")
                        .passwordHash("pwd")
                        .role(Role.SUPER_ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build()));

        orgAdminUser = userRepository.save(User.builder()
                .email("admin-" + suffix + "@target.com")
                .name("Org Admin")
                .passwordHash("pwd")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(testOrg)
                .build());

        labStaffUser = userRepository.save(User.builder()
                .email("staff-" + suffix + "@target.com")
                .name("Lab Staff")
                .passwordHash("pwd")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(testOrg)
                .build());

        testPatient = patientRepository.save(Patient.builder()
                .organization(testOrg)
                .salutation(Salutation.MR)
                .name("Patient Test")
                .patientCode("PT-" + suffix)
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .phone("9123456780")
                .build());

        testReport = reportRepository.save(Report.builder()
                .organization(testOrg)
                .patientRefId(testPatient.getRefId())
                .status(ReportStatus.FINALIZED)
                .createdBy(labStaffUser)
                .finalizedBy(labStaffUser)
                .finalizedAt(Instant.now())
                .reportVersion(1)
                .lockVersion(0L)
                .build());
    }

    private void auth(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getRefId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        auth.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("SEC-RBAC-001: SUPER_ADMIN is blocked from directly calling getReport without break-glass")
    void testSuperAdmin_DirectGetReportBlocked() {
        auth(superAdminUser);

        assertThatThrownBy(() -> reportService.getReport(testReport.getRefId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Direct cross-tenant clinical report access is prohibited for SUPER_ADMIN");
    }

    @Test
    @DisplayName("SEC-RBAC-001: SUPER_ADMIN can access report via audited break-glass procedure")
    void testSuperAdmin_BreakGlassSuccessAndAuditLogged() {
        auth(superAdminUser);

        String justification = "Support Ticket #4928: Investigating patient diagnostic export issue.";
        BreakGlassAccessRequest request = new BreakGlassAccessRequest(testOrg.getRefId(), justification);

        ReportResponse response = reportService.breakGlassAccess(testReport.getRefId(), request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.refId()).isEqualTo(testReport.getRefId());

        // Verify audit log
        List<SecurityAuditLog> logs = auditLogRepository.findByTargetReportRefIdOrderByCreatedAtDesc(testReport.getRefId());
        assertThat(logs).isNotEmpty();
        SecurityAuditLog log = logs.get(0);
        assertThat(log.isSuccess()).isTrue();
        assertThat(log.getActor().getEmail()).isEqualTo(superAdminUser.getEmail());
        assertThat(log.getTargetOrganizationRefId()).isEqualTo(testOrg.getRefId());
        assertThat(log.getJustification()).isEqualTo(justification);
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("SEC-RBAC-001: Break-glass with mismatched organization is logged as failure and rejected")
    void testSuperAdmin_BreakGlassMismatchedOrgRejectedAndLogged() {
        auth(superAdminUser);

        String justification = "Unauthorized cross-org probe attempt";
        BreakGlassAccessRequest request = new BreakGlassAccessRequest(otherOrg.getRefId(), justification);

        assertThatThrownBy(() -> reportService.breakGlassAccess(testReport.getRefId(), request, "10.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class);

        // Verify failure was logged
        List<SecurityAuditLog> logs = auditLogRepository.findByTargetReportRefIdOrderByCreatedAtDesc(testReport.getRefId());
        assertThat(logs).isNotEmpty();
        SecurityAuditLog log = logs.get(0);
        assertThat(log.isSuccess()).isFalse();
        assertThat(log.getFailureReason()).contains("Target report not found in specified organization");
    }

    @Test
    @DisplayName("SEC-RBAC-001: ORG_ADMIN cannot invoke break-glass endpoint")
    void testOrgAdmin_CannotInvokeBreakGlass() {
        auth(orgAdminUser);

        BreakGlassAccessRequest request = new BreakGlassAccessRequest(testOrg.getRefId(), "Trying break glass as org admin");

        assertThatThrownBy(() -> reportService.breakGlassAccess(testReport.getRefId(), request, "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only SUPER_ADMIN can perform break-glass");
    }

    @Test
    @DisplayName("SEC-RBAC-001: LAB_STAFF cannot invoke break-glass endpoint")
    void testLabStaff_CannotInvokeBreakGlass() {
        auth(labStaffUser);

        BreakGlassAccessRequest request = new BreakGlassAccessRequest(testOrg.getRefId(), "Trying break glass as lab staff");

        assertThatThrownBy(() -> reportService.breakGlassAccess(testReport.getRefId(), request, "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only SUPER_ADMIN can perform break-glass");
    }

    @Test
    @DisplayName("SEC-RBAC-002: SUPER_ADMIN cannot create reports (service enforces active organization invariant)")
    void testSuperAdmin_CannotCreateReport() {
        auth(superAdminUser);

        assertThatThrownBy(() -> reportService.createReport(new CreateReportRequest(testPatient.getRefId())))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Organization is inactive or not found");
    }
}
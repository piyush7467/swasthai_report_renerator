package com.swasthai.report_generator.security;

import com.swasthai.report_generator.license.dto.request.ActivateLicenseRequest;
import com.swasthai.report_generator.license.dto.request.RenewLicenseRequest;
import com.swasthai.report_generator.license.entity.License;
import com.swasthai.report_generator.license.entity.LicenseStatus;
import com.swasthai.report_generator.license.entity.Plan;
import com.swasthai.report_generator.license.repository.LicenseRepository;
import com.swasthai.report_generator.license.repository.PlanRepository;
import com.swasthai.report_generator.license.service.LicenseGuard;
import com.swasthai.report_generator.license.service.LicenseService;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.entity.Salutation;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.dto.request.AddReportTestRequest;
import com.swasthai.report_generator.report.dto.request.CreateReportRequest;
import com.swasthai.report_generator.report.dto.response.ReportResponse;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.service.ReportService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LicenseSecurityTest {

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private LicenseGuard licenseGuard;

    @Autowired
    private ReportService reportService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    private Organization licensedOrg;
    private Organization unlicensedOrg;
    private Organization expiredOrg;

    private User licensedStaff;
    private User unlicensedStaff;
    private User expiredStaff;
    private User superAdminUser;

    private Patient licensedPatient;
    private Patient expiredPatient;

    private Plan activePlan;
    private Plan inactivePlan;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Plans
        activePlan = planRepository.save(Plan.builder()
                .code("ACT-PLAN-" + suffix)
                .name("Active Plan " + suffix)
                .annualPrice(new BigDecimal("10000.00"))
                .currency("INR")
                .active(true)
                .build());

        inactivePlan = planRepository.save(Plan.builder()
                .code("INACT-PLAN-" + suffix)
                .name("Inactive Plan " + suffix)
                .annualPrice(new BigDecimal("20000.00"))
                .currency("INR")
                .active(false)
                .build());

        // Organizations
        licensedOrg = organizationRepository.save(Organization.builder()
                .name("Licensed Lab " + suffix)
                .code("LIC-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        unlicensedOrg = organizationRepository.save(Organization.builder()
                .name("Unlicensed Lab " + suffix)
                .code("UNLIC-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        expiredOrg = organizationRepository.save(Organization.builder()
                .name("Expired Lab " + suffix)
                .code("EXP-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        // Licenses
        licenseRepository.save(License.builder()
                .organization(licensedOrg)
                .plan(activePlan)
                .status(LicenseStatus.ACTIVE)
                .startedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().plus(300, ChronoUnit.DAYS))
                .build());

        licenseRepository.save(License.builder()
                .organization(expiredOrg)
                .plan(activePlan)
                .status(LicenseStatus.EXPIRED)
                .startedAt(Instant.now().minus(400, ChronoUnit.DAYS))
                .expiresAt(Instant.now().minus(35, ChronoUnit.DAYS))
                .build());

        // Users
        licensedStaff = userRepository.save(User.builder()
                .email("staff-" + suffix + "@lic.com")
                .name("Lic Staff")
                .passwordHash("pwd")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(licensedOrg)
                .build());

        unlicensedStaff = userRepository.save(User.builder()
                .email("staff-" + suffix + "@unlic.com")
                .name("Unlic Staff")
                .passwordHash("pwd")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(unlicensedOrg)
                .build());

        expiredStaff = userRepository.save(User.builder()
                .email("staff-" + suffix + "@exp.com")
                .name("Exp Staff")
                .passwordHash("pwd")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(expiredOrg)
                .build());

        superAdminUser = userRepository.findByEmailIgnoreCase("admin@swasthai.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("admin-" + suffix + "@platform.com")
                        .name("Super Admin")
                        .passwordHash("pwd")
                        .role(Role.SUPER_ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build()));

        // Patients
        licensedPatient = patientRepository.save(Patient.builder()
                .organization(licensedOrg)
                .salutation(Salutation.MR)
                .name("Patient Lic")
                .patientCode("P-LIC-" + suffix)
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1992, 5, 12))
                .gender(Gender.MALE)
                .phone("9876543210")
                .build());

        expiredPatient = patientRepository.save(Patient.builder()
                .organization(expiredOrg)
                .salutation(Salutation.MRS)
                .name("Patient Exp")
                .patientCode("P-EXP-" + suffix)
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1988, 8, 20))
                .gender(Gender.FEMALE)
                .phone("9876543211")
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
    @DisplayName("SEC-LIC-001: Unlicensed organization is blocked from creating reports")
    void testUnlicensedOrganization_ReportCreationBlocked() {
        auth(unlicensedStaff);

        assertThatThrownBy(() -> reportService.createReport(new CreateReportRequest("PAT-DUMMY")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("An active license is required");
    }

    @Test
    @DisplayName("SEC-LIC-001: Expired organization is blocked from creating reports")
    void testExpiredOrganization_ReportCreationBlocked() {
        auth(expiredStaff);

        assertThatThrownBy(() -> reportService.createReport(new CreateReportRequest(expiredPatient.getRefId())))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("license has expired");
    }

    @Test
    @DisplayName("SEC-LIC-001: Expired organization cannot modify or finalize existing draft reports")
    void testExpiredOrganization_DraftProcessingBlocked() {
        // Create draft directly in DB for expiredOrg to simulate a pre-existing draft
        Report draft = reportRepository.save(Report.builder()
                .organization(expiredOrg)
                .patientRefId(expiredPatient.getRefId())
                .status(ReportStatus.DRAFT)
                .createdBy(expiredStaff)
                .reportVersion(1)
                .lockVersion(0L)
                .build());

        auth(expiredStaff);

        // Modifying draft should be blocked
        assertThatThrownBy(() -> reportService.addTest(draft.getRefId(), new AddReportTestRequest("TEST-REF", null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("license has expired");

        // Finalizing draft should be blocked
        assertThatThrownBy(() -> reportService.finalizeReport(draft.getRefId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("license has expired");
    }

    @Test
    @DisplayName("SEC-LIC-001: Expired organization CAN still read historical finalized reports")
    void testExpiredOrganization_HistoricalReportReadAllowed() {
        // Create finalized report in DB for expiredOrg
        Report finalizedReport = reportRepository.save(Report.builder()
                .organization(expiredOrg)
                .patientRefId(expiredPatient.getRefId())
                .status(ReportStatus.FINALIZED)
                .createdBy(expiredStaff)
                .finalizedBy(expiredStaff)
                .finalizedAt(Instant.now().minus(50, ChronoUnit.DAYS))
                .reportVersion(1)
                .lockVersion(0L)
                .build());

        auth(expiredStaff);

        // Historical read MUST succeed
        ReportResponse response = reportService.getReport(finalizedReport.getRefId());
        assertThat(response).isNotNull();
        assertThat(response.refId()).isEqualTo(finalizedReport.getRefId());
        assertThat(response.status()).isEqualTo(ReportStatus.FINALIZED);
    }

    @Test
    @DisplayName("SEC-LIC-001: Active license allows report creation and processing")
    void testActiveLicense_ReportCreationAndProcessingAllowed() {
        auth(licensedStaff);

        ReportResponse report = reportService.createReport(new CreateReportRequest(licensedPatient.getRefId()));
        assertThat(report).isNotNull();
        assertThat(report.status()).isEqualTo(ReportStatus.DRAFT);
    }

    @Test
    @DisplayName("SEC-LIC-001: Inactive plan cannot be selected for new activation or renewal")
    void testInactivePlan_CannotActivateOrRenew() {
        auth(superAdminUser);

        // Activation with inactive plan rejected
        assertThatThrownBy(() -> licenseService.activateLicense(
                unlicensedOrg.getRefId(),
                new ActivateLicenseRequest(inactivePlan.getRefId(), "TX-12345")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Selected plan is inactive");

        // Renewal with inactive plan rejected
        assertThatThrownBy(() -> licenseService.renewLicense(
                licensedOrg.getRefId(),
                new RenewLicenseRequest(inactivePlan.getRefId(), "TX-67890")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Selected plan is inactive");
    }

    @Test
    @DisplayName("SEC-LIC-001: Active paid license remains valid even if plan is later deactivated")
    void testActivePaidLicense_SurvivesPlanDeactivation() {
        // Deactivate the activePlan in catalog
        activePlan.setActive(false);
        planRepository.saveAndFlush(activePlan);

        // Organization's existing paid license must still allow processing until its expiry
        auth(licensedStaff);
        ReportResponse report = reportService.createReport(new CreateReportRequest(licensedPatient.getRefId()));
        assertThat(report).isNotNull();
        assertThat(report.status()).isEqualTo(ReportStatus.DRAFT);
    }

    @Test
    @DisplayName("SEC-LIC-001: Non-SUPER_ADMIN users cannot activate or renew licenses")
    void testNonSuperAdmin_CannotManageLicenses() {
        auth(licensedStaff);

        assertThatThrownBy(() -> licenseService.activateLicense(
                unlicensedOrg.getRefId(),
                new ActivateLicenseRequest(activePlan.getRefId(), "TX-123")
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only SUPER_ADMIN can manage licenses");

        assertThatThrownBy(() -> licenseService.renewLicense(
                licensedOrg.getRefId(),
                new RenewLicenseRequest(activePlan.getRefId(), "TX-123")
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only SUPER_ADMIN can manage licenses");
    }
}
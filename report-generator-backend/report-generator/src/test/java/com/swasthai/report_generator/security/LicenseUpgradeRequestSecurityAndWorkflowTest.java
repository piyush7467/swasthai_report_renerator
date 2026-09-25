package com.swasthai.report_generator.security;

import com.swasthai.report_generator.common.exception.BadRequestException;
import com.swasthai.report_generator.common.exception.ConflictException;
import com.swasthai.report_generator.common.exception.ForbiddenException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.license.dto.request.CreateUpgradeRequest;
import com.swasthai.report_generator.license.dto.request.UpdateUpgradeRequestStatusRequest;
import com.swasthai.report_generator.license.dto.response.AvailablePlanResponse;
import com.swasthai.report_generator.license.dto.response.OrganizationLicenseOverviewResponse;
import com.swasthai.report_generator.license.dto.response.PlanUpgradeRequestResponse;
import com.swasthai.report_generator.license.entity.License;
import com.swasthai.report_generator.license.entity.LicenseStatus;
import com.swasthai.report_generator.license.entity.Plan;
import com.swasthai.report_generator.license.entity.UpgradeRequestStatus;
import com.swasthai.report_generator.license.repository.LicenseRepository;
import com.swasthai.report_generator.license.repository.PlanRepository;
import com.swasthai.report_generator.license.repository.PlanUpgradeRequestRepository;
import com.swasthai.report_generator.license.service.PlanUpgradeRequestService;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LicenseUpgradeRequestSecurityAndWorkflowTest {

    @Autowired
    private PlanUpgradeRequestService planUpgradeRequestService;

    @Autowired
    private PlanUpgradeRequestRepository planUpgradeRequestRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAuditLogRepository securityAuditLogRepository;

    private Organization orgA;
    private Organization orgB;

    private User orgAdminA;
    private User orgAdminB;
    private User labStaffA;
    private User superAdmin;

    private Plan starterPlan;
    private Plan proPlan;
    private Plan enterprisePlan;
    private Plan inactivePlan;

    private License licenseA;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Plans
        starterPlan = planRepository.save(Plan.builder()
                .code("STARTER-" + suffix)
                .name("Starter Plan " + suffix)
                .annualPrice(new BigDecimal("9990.00"))
                .currency("INR")
                .maxLabStaff(3)
                .active(true)
                .build());

        proPlan = planRepository.save(Plan.builder()
                .code("PRO-" + suffix)
                .name("Pro Plan " + suffix)
                .annualPrice(new BigDecimal("29990.00"))
                .currency("INR")
                .maxLabStaff(10)
                .active(true)
                .build());

        enterprisePlan = planRepository.save(Plan.builder()
                .code("ENT-" + suffix)
                .name("Enterprise Plan " + suffix)
                .annualPrice(new BigDecimal("99990.00"))
                .currency("INR")
                .maxLabStaff(50)
                .active(true)
                .build());

        inactivePlan = planRepository.save(Plan.builder()
                .code("LEGACY-" + suffix)
                .name("Legacy Plan " + suffix)
                .annualPrice(new BigDecimal("5000.00"))
                .currency("INR")
                .maxLabStaff(2)
                .active(false)
                .build());

        // Organizations
        orgA = organizationRepository.save(Organization.builder()
                .name("Diagnostic Center A " + suffix)
                .code("ORGA-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(Organization.builder()
                .name("Diagnostic Center B " + suffix)
                .code("ORGB-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        // Users
        orgAdminA = userRepository.save(User.builder()
                .email("adminA-" + suffix + "@labA.com")
                .name("Admin Org A")
                .passwordHash("pwd")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build());

        orgAdminB = userRepository.save(User.builder()
                .email("adminB-" + suffix + "@labB.com")
                .name("Admin Org B")
                .passwordHash("pwd")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(orgB)
                .build());

        labStaffA = userRepository.save(User.builder()
                .email("staffA-" + suffix + "@labA.com")
                .name("Staff A")
                .passwordHash("pwd")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build());

        superAdmin = userRepository.findByEmailIgnoreCase("admin@swasthai.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("superadmin-" + suffix + "@swasthai.com")
                        .name("Platform Super Admin")
                        .passwordHash("pwd")
                        .role(Role.SUPER_ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build()));

        // Licenses
        licenseA = licenseRepository.save(License.builder()
                .organization(orgA)
                .plan(starterPlan)
                .status(LicenseStatus.ACTIVE)
                .startedAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .expiresAt(Instant.now().plus(335, ChronoUnit.DAYS))
                .build());

        licenseRepository.save(License.builder()
                .organization(orgB)
                .plan(proPlan)
                .status(LicenseStatus.ACTIVE)
                .startedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .expiresAt(Instant.now().plus(355, ChronoUnit.DAYS))
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
    @DisplayName("UPG-001: Org Admin can view own license overview with accurate metrics")
    void testGetOrganizationLicenseOverview_Success() {
        auth(orgAdminA);

        OrganizationLicenseOverviewResponse overview = planUpgradeRequestService.getOrganizationLicenseOverview();

        assertThat(overview).isNotNull();
        assertThat(overview.license()).isNotNull();
        assertThat(overview.license().planName()).isEqualTo(starterPlan.getName());
        assertThat(overview.license().maxLabStaff()).isEqualTo(3);
        assertThat(overview.staffUsage()).isNotNull();
        assertThat(overview.staffUsage().activeStaff()).isEqualTo(1); // labStaffA
        assertThat(overview.staffUsage().remainingSlots()).isEqualTo(2);
        assertThat(overview.staffUsage().limitReached()).isFalse();
        assertThat(overview.staffUsage().overLimit()).isFalse();
        assertThat(overview.daysRemaining()).isGreaterThan(300);
        assertThat(overview.expiryStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("UPG-002: Lab staff cannot view license overview")
    void testGetOrganizationLicenseOverview_ForbiddenForStaff() {
        auth(labStaffA);

        assertThatThrownBy(() -> planUpgradeRequestService.getOrganizationLicenseOverview())
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only ORG_ADMIN");
    }

    @Test
    @DisplayName("UPG-003: Org Admin can view available plans for upgrade")
    void testGetAvailablePlansForUpgrade_Success() {
        auth(orgAdminA);

        List<AvailablePlanResponse> plans = planUpgradeRequestService.getAvailablePlansForUpgrade();

        assertThat(plans).isNotEmpty();
        // Inactive plan should NOT be included
        assertThat(plans).noneMatch(p -> p.refId().equals(inactivePlan.getRefId()));

        // Starter plan should be marked currentPlan: true
        AvailablePlanResponse starter = plans.stream()
                .filter(p -> p.refId().equals(starterPlan.getRefId()))
                .findFirst().orElseThrow();
        assertThat(starter.currentPlan()).isTrue();
        assertThat(starter.upgrade()).isFalse();

        // Pro and Enterprise should be marked upgrade: true
        AvailablePlanResponse pro = plans.stream()
                .filter(p -> p.refId().equals(proPlan.getRefId()))
                .findFirst().orElseThrow();
        assertThat(pro.currentPlan()).isFalse();
        assertThat(pro.upgrade()).isTrue();
    }

    @Test
    @DisplayName("UPG-004: Org Admin creates upgrade request with server-side validation and audit log")
    void testCreateUpgradeRequest_Success() {
        auth(orgAdminA);

        CreateUpgradeRequest request = new CreateUpgradeRequest(
                proPlan.getRefId(),
                "Scaling laboratory capacity",
                "Dr. John Doe",
                "johndoe@laba.com",
                "+91 9876543210",
                "Need 10 seats for upcoming branch expansion"
        );

        PlanUpgradeRequestResponse response = planUpgradeRequestService.createUpgradeRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.refId()).startsWith("UPG-");
        assertThat(response.organizationRefId()).isEqualTo(orgA.getRefId());
        assertThat(response.currentPlanName()).isEqualTo(starterPlan.getName());
        assertThat(response.requestedPlanName()).isEqualTo(proPlan.getName());
        assertThat(response.currentActiveStaffCount()).isEqualTo(1);
        assertThat(response.requestedStaffCapacity()).isEqualTo(10);
        assertThat(response.status()).isEqualTo(UpgradeRequestStatus.PENDING);
        assertThat(response.contactEmail()).isEqualTo("johndoe@laba.com");

        // Verify audit log created
        boolean auditExists = securityAuditLogRepository.findAll().stream()
                .anyMatch(log -> "LICENSE_UPGRADE_REQUESTED".equals(log.getAction())
                        && orgA.getRefId().equals(log.getTargetOrganizationRefId()));
        assertThat(auditExists).isTrue();
    }

    @Test
    @DisplayName("UPG-005: Duplicate pending upgrade request for same plan is rejected with ConflictException")
    void testCreateUpgradeRequest_DuplicatePending_Rejected() {
        auth(orgAdminA);

        CreateUpgradeRequest request = new CreateUpgradeRequest(
                proPlan.getRefId(),
                "First request",
                "Dr. John",
                "johndoe@laba.com",
                "9876543210",
                null
        );

        planUpgradeRequestService.createUpgradeRequest(request);

        // Attempt second pending request for the same plan
        assertThatThrownBy(() -> planUpgradeRequestService.createUpgradeRequest(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has a pending upgrade request");
    }

    @Test
    @DisplayName("UPG-006: Requesting current plan is rejected")
    void testCreateUpgradeRequest_SamePlan_Rejected() {
        auth(orgAdminA);

        CreateUpgradeRequest request = new CreateUpgradeRequest(
                starterPlan.getRefId(),
                "Duplicate plan",
                "Dr. John",
                "johndoe@laba.com",
                "9876543210",
                null
        );

        assertThatThrownBy(() -> planUpgradeRequestService.createUpgradeRequest(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already on the");
    }

    @Test
    @DisplayName("UPG-007: Requesting inactive plan is rejected")
    void testCreateUpgradeRequest_InactivePlan_Rejected() {
        auth(orgAdminA);

        CreateUpgradeRequest request = new CreateUpgradeRequest(
                inactivePlan.getRefId(),
                "Old plan",
                "Dr. John",
                "johndoe@laba.com",
                "9876543210",
                null
        );

        assertThatThrownBy(() -> planUpgradeRequestService.createUpgradeRequest(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not currently available");
    }

    @Test
    @DisplayName("UPG-008: Tenant isolation - Org Admin B cannot access Org Admin A's upgrade request (IDOR prevention)")
    void testTenantIsolation_CrossTenantAccessReturns404() {
        auth(orgAdminA);

        CreateUpgradeRequest request = new CreateUpgradeRequest(
                proPlan.getRefId(),
                "Org A request",
                "Dr. John",
                "johndoe@laba.com",
                "9876543210",
                null
        );
        PlanUpgradeRequestResponse created = planUpgradeRequestService.createUpgradeRequest(request);

        // Org Admin B attempts to access Org A's request
        auth(orgAdminB);

        assertThatThrownBy(() -> planUpgradeRequestService.getMyUpgradeRequestDetails(created.refId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Upgrade request not found");
    }

    @Test
    @DisplayName("UPG-009: Org Admin can cancel a pending upgrade request")
    void testCancelMyUpgradeRequest_Success() {
        auth(orgAdminA);

        CreateUpgradeRequest request = new CreateUpgradeRequest(
                enterprisePlan.getRefId(),
                "Cancel test",
                "Dr. John",
                "johndoe@laba.com",
                "9876543210",
                null
        );
        PlanUpgradeRequestResponse created = planUpgradeRequestService.createUpgradeRequest(request);

        PlanUpgradeRequestResponse cancelled = planUpgradeRequestService.cancelMyUpgradeRequest(created.refId());

        assertThat(cancelled.status()).isEqualTo(UpgradeRequestStatus.CANCELLED);

        // Attempting to cancel again should fail
        assertThatThrownBy(() -> planUpgradeRequestService.cancelMyUpgradeRequest(created.refId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only pending upgrade requests can be cancelled");
    }

    @Test
    @DisplayName("UPG-010: Super Admin workflow: PENDING -> CONTACTED -> APPROVED")
    void testSuperAdminWorkflow_PendingToContactedToApproved() {
        auth(orgAdminA);
        CreateUpgradeRequest createReq = new CreateUpgradeRequest(
                proPlan.getRefId(),
                "Workflow test",
                "Dr. John",
                "johndoe@laba.com",
                "9876543210",
                null
        );
        PlanUpgradeRequestResponse created = planUpgradeRequestService.createUpgradeRequest(createReq);

        // Switch to Super Admin
        auth(superAdmin);

        // 1. PENDING -> CONTACTED
        UpdateUpgradeRequestStatusRequest contactReq = new UpdateUpgradeRequestStatusRequest(
                UpgradeRequestStatus.CONTACTED,
                "Called the lab director Dr. John Doe on phone",
                null
        );
        PlanUpgradeRequestResponse contacted = planUpgradeRequestService.updateUpgradeRequestStatus(created.refId(), contactReq);
        assertThat(contacted.status()).isEqualTo(UpgradeRequestStatus.CONTACTED);
        assertThat(contacted.adminNotes()).isEqualTo("Called the lab director Dr. John Doe on phone");
        assertThat(contacted.reviewedByEmail()).isEqualTo(superAdmin.getEmail());

        // 2. CONTACTED -> APPROVED
        UpdateUpgradeRequestStatusRequest approveReq = new UpdateUpgradeRequestStatusRequest(
                UpgradeRequestStatus.APPROVED,
                "Payment verified offline via bank transfer receipt #TXN-98213",
                null
        );
        PlanUpgradeRequestResponse approved = planUpgradeRequestService.updateUpgradeRequestStatus(created.refId(), approveReq);
        assertThat(approved.status()).isEqualTo(UpgradeRequestStatus.APPROVED);

        License updatedLic = licenseRepository.findWithPlanByOrganizationId(orgA.getId()).orElseThrow();
        assertThat(updatedLic.getPlan().getId()).isEqualTo(proPlan.getId());
        assertThat(updatedLic.getPlan().getName()).isEqualTo(proPlan.getName());

        // Terminal state cannot be transitioned again
        assertThatThrownBy(() -> planUpgradeRequestService.updateUpgradeRequestStatus(created.refId(), contactReq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change status of a request that is already APPROVED");
    }

    @Test
    @DisplayName("UPG-011: Super Admin rejection requires rejection reason")
    void testSuperAdminRejection_RequiresReason() {
        auth(orgAdminA);
        CreateUpgradeRequest createReq = new CreateUpgradeRequest(
                enterprisePlan.getRefId(),
                "Rejection test",
                "Dr. John",
                "johndoe@laba.com",
                "9876543210",
                null
        );
        PlanUpgradeRequestResponse created = planUpgradeRequestService.createUpgradeRequest(createReq);

        auth(superAdmin);

        // Rejection without reason fails
        UpdateUpgradeRequestStatusRequest badRejectReq = new UpdateUpgradeRequestStatusRequest(
                UpgradeRequestStatus.REJECTED,
                "Notes",
                ""
        );
        assertThatThrownBy(() -> planUpgradeRequestService.updateUpgradeRequestStatus(created.refId(), badRejectReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("rejection reason is required");

        // Rejection with reason succeeds
        UpdateUpgradeRequestStatusRequest goodRejectReq = new UpdateUpgradeRequestStatusRequest(
                UpgradeRequestStatus.REJECTED,
                "Declined after review",
                "Organization does not currently meet enterprise criteria"
        );
        PlanUpgradeRequestResponse rejected = planUpgradeRequestService.updateUpgradeRequestStatus(created.refId(), goodRejectReq);
        assertThat(rejected.status()).isEqualTo(UpgradeRequestStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Organization does not currently meet enterprise criteria");
    }

    @Test
    @DisplayName("UPG-012: Invalid transitions like PENDING directly to APPROVED are blocked")
    void testInvalidStatusTransition_Blocked() {
        auth(orgAdminA);
        CreateUpgradeRequest createReq = new CreateUpgradeRequest(
                proPlan.getRefId(),
                "Invalid transition test",
                "Dr. John",
                "johndoe@laba.com",
                "9876543210",
                null
        );
        PlanUpgradeRequestResponse created = planUpgradeRequestService.createUpgradeRequest(createReq);

        auth(superAdmin);

        // PENDING -> APPROVED directly is blocked (must contact first)
        UpdateUpgradeRequestStatusRequest invalidReq = new UpdateUpgradeRequestStatusRequest(
                UpgradeRequestStatus.APPROVED,
                "Direct approval",
                null
        );
        assertThatThrownBy(() -> planUpgradeRequestService.updateUpgradeRequestStatus(created.refId(), invalidReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("UPG-013: Org Admin cannot access Super Admin upgrade request management APIs")
    void testSuperAdminApis_ForbiddenForOrgAdmin() {
        auth(orgAdminA);

        assertThatThrownBy(() -> planUpgradeRequestService.getAllUpgradeRequests(0, 10, null, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only SUPER_ADMIN");

        assertThatThrownBy(() -> planUpgradeRequestService.getUpgradeRequestDetails("UPG-ANY"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only SUPER_ADMIN");

        assertThatThrownBy(() -> planUpgradeRequestService.updateUpgradeRequestStatus("UPG-ANY", new UpdateUpgradeRequestStatusRequest(UpgradeRequestStatus.APPROVED, null, null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only SUPER_ADMIN");
    }

    @Test
    @DisplayName("UPG-014: Expired license reflects EXPIRING_SOON / EXPIRED status in overview")
    void testExpiredLicense_ReflectedInOverview() {
        // Change licenseA to expiring in 5 days
        licenseA.setExpiresAt(Instant.now().plus(5, ChronoUnit.DAYS));
        licenseRepository.saveAndFlush(licenseA);

        auth(orgAdminA);
        OrganizationLicenseOverviewResponse overview = planUpgradeRequestService.getOrganizationLicenseOverview();
        assertThat(overview.expiryStatus()).isEqualTo("EXPIRING_SOON");
        assertThat(overview.daysRemaining()).isLessThanOrEqualTo(5);

        // Change licenseA to expired in the past
        licenseA.setStatus(LicenseStatus.EXPIRED);
        licenseA.setExpiresAt(Instant.now().minus(2, ChronoUnit.DAYS));
        licenseRepository.saveAndFlush(licenseA);

        OrganizationLicenseOverviewResponse expiredOverview = planUpgradeRequestService.getOrganizationLicenseOverview();
        assertThat(expiredOverview.expiryStatus()).isEqualTo("EXPIRED");
        assertThat(expiredOverview.daysRemaining()).isEqualTo(0);
        assertThat(expiredOverview.license().currentlyUsable()).isFalse();
    }
}

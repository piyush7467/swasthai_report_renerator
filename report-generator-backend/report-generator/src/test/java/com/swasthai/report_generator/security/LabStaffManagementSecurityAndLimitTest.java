package com.swasthai.report_generator.security;

import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.common.exception.BadRequestException;
import com.swasthai.report_generator.common.exception.ConflictException;
import com.swasthai.report_generator.common.exception.ForbiddenException;
import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.license.entity.License;
import com.swasthai.report_generator.license.entity.LicenseStatus;
import com.swasthai.report_generator.license.entity.Plan;
import com.swasthai.report_generator.license.repository.LicenseRepository;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.user.dto.request.CreateLabStaffRequest;
import com.swasthai.report_generator.user.dto.request.UpdateUserStatusRequest;
import com.swasthai.report_generator.user.dto.response.LabStaffDetailsResponse;
import com.swasthai.report_generator.user.dto.response.LabStaffSummaryResponse;
import com.swasthai.report_generator.user.dto.response.UserResponse;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import com.swasthai.report_generator.user.service.LabStaffService;
import com.swasthai.report_generator.user.service.impl.LabStaffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabStaffManagementSecurityAndLimitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LicenseRepository licenseRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private com.swasthai.report_generator.auth.repository.RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private com.swasthai.report_generator.security.audit.service.AuditLogService auditLogService;

    private LabStaffService labStaffService;

    private Organization orgA;
    private Organization orgB;
    private User orgAdminA;
    private Plan starterPlan;
    private License activeLicenseA;

    @BeforeEach
    void setUp() {
        labStaffService = new LabStaffServiceImpl(
                userRepository,
                licenseRepository,
                reportRepository,
                refreshTokenRepository,
                passwordEncoder,
                currentUserService,
                auditLogService
        );

        orgA = Organization.builder()
                .id(UUID.randomUUID())
                .refId("ORG-AAA")
                .name("Alpha Diagnostics")
                .status(OrganizationStatus.ACTIVE)
                .build();

        orgB = Organization.builder()
                .id(UUID.randomUUID())
                .refId("ORG-BBB")
                .name("Beta Diagnostics")
                .status(OrganizationStatus.ACTIVE)
                .build();

        orgAdminA = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-ADMIN-A")
                .name("Admin Alpha")
                .email("admin@alpha.com")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build();

        starterPlan = Plan.builder()
                .id(UUID.randomUUID())
                .refId("PLAN-STARTER")
                .code("STARTER")
                .name("Starter Plan")
                .maxLabStaff(2)
                .active(true)
                .build();

        activeLicenseA = License.builder()
                .id(UUID.randomUUID())
                .refId("LIC-A")
                .organization(orgA)
                .plan(starterPlan)
                .status(LicenseStatus.ACTIVE)
                .startedAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .expiresAt(Instant.now().plus(335, ChronoUnit.DAYS))
                .build();
    }

    @Test
    @DisplayName("ORG_ADMIN creates lab staff successfully within plan limits")
    void createLabStaff_Success() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);
        when(userRepository.existsByEmailIgnoreCase("tech1@alpha.com")).thenReturn(false);
        when(licenseRepository.findByOrganizationIdForUpdate(orgA.getId())).thenReturn(Optional.of(activeLicenseA));
        when(userRepository.countByOrganization_IdAndRoleAndStatus(orgA.getId(), Role.LAB_STAFF, UserStatus.ACTIVE)).thenReturn(1L);
        when(passwordEncoder.encode("StrongPassword123")).thenReturn("hashed_pw");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setRefId("USR-STAFF-1");
            u.setCreatedAt(Instant.now());
            u.setUpdatedAt(Instant.now());
            return u;
        });

        CreateLabStaffRequest request = CreateLabStaffRequest.builder()
                .name("Lab Technician 1")
                .email("tech1@alpha.com")
                .password("StrongPassword123")
                .confirmPassword("StrongPassword123")
                .build();

        UserResponse response = labStaffService.createLabStaff(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Lab Technician 1");
        assertThat(response.getEmail()).isEqualTo("tech1@alpha.com");
        assertThat(response.getRole()).isEqualTo(Role.LAB_STAFF);
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.getOrganizationRefId()).isEqualTo(orgA.getRefId());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed_pw");
        assertThat(saved.getOrganization()).isEqualTo(orgA);
    }

    @Test
    @DisplayName("createLabStaff rejects duplicate email with 409 Conflict")
    void createLabStaff_DuplicateEmail_ThrowsConflict() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);
        when(userRepository.existsByEmailIgnoreCase("existing@alpha.com")).thenReturn(true);

        CreateLabStaffRequest request = CreateLabStaffRequest.builder()
                .name("Duplicate User")
                .email("existing@alpha.com")
                .password("StrongPassword123")
                .confirmPassword("StrongPassword123")
                .build();

        assertThatThrownBy(() -> labStaffService.createLabStaff(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLabStaff rejects mismatched password and confirm password")
    void createLabStaff_PasswordMismatch_ThrowsBadRequest() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);

        CreateLabStaffRequest request = CreateLabStaffRequest.builder()
                .name("Mismatch User")
                .email("user@alpha.com")
                .password("PasswordABC123")
                .confirmPassword("PasswordXYZ999")
                .build();

        assertThatThrownBy(() -> labStaffService.createLabStaff(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Password and confirm password do not match");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLabStaff strictly enforces plan maxLabStaff limit with 409 Conflict")
    void createLabStaff_LimitReached_ThrowsConflict() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);
        when(userRepository.existsByEmailIgnoreCase("newstaff@alpha.com")).thenReturn(false);
        when(licenseRepository.findByOrganizationIdForUpdate(orgA.getId())).thenReturn(Optional.of(activeLicenseA));
        // Current active staff is 2, and maxLabStaff is 2
        when(userRepository.countByOrganization_IdAndRoleAndStatus(orgA.getId(), Role.LAB_STAFF, UserStatus.ACTIVE)).thenReturn(2L);

        CreateLabStaffRequest request = CreateLabStaffRequest.builder()
                .name("Over Limit Staff")
                .email("newstaff@alpha.com")
                .password("StrongPassword123")
                .confirmPassword("StrongPassword123")
                .build();

        assertThatThrownBy(() -> labStaffService.createLabStaff(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Lab staff limit reached for this organization (2 staff max)");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLabStaff rejects when organization has expired license")
    void createLabStaff_ExpiredLicense_ThrowsForbidden() {
        License expiredLicense = License.builder()
                .id(UUID.randomUUID())
                .refId("LIC-EXPIRED")
                .organization(orgA)
                .plan(starterPlan)
                .status(LicenseStatus.EXPIRED)
                .startedAt(Instant.now().minus(400, ChronoUnit.DAYS))
                .expiresAt(Instant.now().minus(35, ChronoUnit.DAYS))
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);
        when(userRepository.existsByEmailIgnoreCase("newstaff@alpha.com")).thenReturn(false);
        when(licenseRepository.findByOrganizationIdForUpdate(orgA.getId())).thenReturn(Optional.of(expiredLicense));

        CreateLabStaffRequest request = CreateLabStaffRequest.builder()
                .name("Staff")
                .email("newstaff@alpha.com")
                .password("StrongPassword123")
                .confirmPassword("StrongPassword123")
                .build();

        assertThatThrownBy(() -> labStaffService.createLabStaff(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("expired or is inactive");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getLabStaffDetails enforces tenant isolation and returns 404 for cross-tenant staff")
    void getLabStaffDetails_CrossTenant_ThrowsNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);
        // Searching in Org A for a staff member of Org B returns empty
        when(userRepository.findByRefIdAndOrganization_Id("USR-STAFF-B", orgA.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labStaffService.getLabStaffDetails("USR-STAFF-B"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Staff member not found");
    }

    @Test
    @DisplayName("getLabStaffDetails returns real report metrics")
    void getLabStaffDetails_ReturnsMetrics() {
        User staffA = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-STAFF-A1")
                .name("Technician Alpha")
                .email("tech@alpha.com")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);
        when(userRepository.findByRefIdAndOrganization_Id("USR-STAFF-A1", orgA.getId())).thenReturn(Optional.of(staffA));
        when(reportRepository.countByCreatedBy_IdAndDeletedAtIsNull(staffA.getId())).thenReturn(42L);
        when(reportRepository.countByFinalizedBy_IdAndDeletedAtIsNull(staffA.getId())).thenReturn(18L);

        LabStaffDetailsResponse details = labStaffService.getLabStaffDetails("USR-STAFF-A1");

        assertThat(details).isNotNull();
        assertThat(details.getRefId()).isEqualTo("USR-STAFF-A1");
        assertThat(details.getReportsCreated()).isEqualTo(42L);
        assertThat(details.getReportsFinalized()).isEqualTo(18L);
    }

    @Test
    @DisplayName("updateLabStaffStatus enforces plan limit when reactivating inactive staff member")
    void updateLabStaffStatus_ActivatingBeyondLimit_ThrowsConflict() {
        User inactiveStaff = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-INACTIVE")
                .name("Old Tech")
                .email("old@alpha.com")
                .role(Role.LAB_STAFF)
                .status(UserStatus.INACTIVE)
                .organization(orgA)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);
        when(userRepository.findByRefIdAndOrganization_Id("USR-INACTIVE", orgA.getId())).thenReturn(Optional.of(inactiveStaff));
        when(licenseRepository.findByOrganizationIdForUpdate(orgA.getId())).thenReturn(Optional.of(activeLicenseA));
        // All 2 slots are currently occupied by other staff
        when(userRepository.countByOrganization_IdAndRoleAndStatus(orgA.getId(), Role.LAB_STAFF, UserStatus.ACTIVE)).thenReturn(2L);

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.ACTIVE);

        assertThatThrownBy(() -> labStaffService.updateLabStaffStatus("USR-INACTIVE", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot activate staff member. Lab staff limit reached");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateLabStaffStatus deactivates staff member and saves successfully")
    void updateLabStaffStatus_Deactivation_Success() {
        User activeStaff = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-ACTIVE")
                .name("Leaving Tech")
                .email("leaving@alpha.com")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);
        when(userRepository.findByRefIdAndOrganization_Id("USR-ACTIVE", orgA.getId())).thenReturn(Optional.of(activeStaff));
        when(userRepository.save(activeStaff)).thenReturn(activeStaff);

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.INACTIVE);

        UserResponse response = labStaffService.updateLabStaffStatus("USR-ACTIVE", request);

        assertThat(response).isNotNull();
        assertThat(activeStaff.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(userRepository).save(activeStaff);
    }

    @Test
    @DisplayName("deleteLabStaff rejects deletion with Conflict when user has historical reports")
    void deleteLabStaff_WithReports_ThrowsConflict() {
        User staff = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-ACTIVE")
                .name("Reporting Tech")
                .role(Role.LAB_STAFF)
                .organization(orgA)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);
        when(userRepository.findByRefIdAndOrganization_Id("USR-ACTIVE", orgA.getId())).thenReturn(Optional.of(staff));
        when(reportRepository.countByCreatedBy_IdAndDeletedAtIsNull(staff.getId())).thenReturn(5L);

        assertThatThrownBy(() -> labStaffService.deleteLabStaff("USR-ACTIVE"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cannot be permanently deleted because they have created or finalized diagnostic reports");

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getLabStaffSummary calculates staff usage and remaining slots correctly")
    void getLabStaffSummary_CalculatesAccurately() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);
        when(licenseRepository.findWithPlanByOrganizationId(orgA.getId())).thenReturn(Optional.of(activeLicenseA));
        when(userRepository.countByOrganization_IdAndRole(orgA.getId(), Role.LAB_STAFF)).thenReturn(3L);
        when(userRepository.countByOrganization_IdAndRoleAndStatus(orgA.getId(), Role.LAB_STAFF, UserStatus.ACTIVE)).thenReturn(1L);

        LabStaffSummaryResponse summary = labStaffService.getLabStaffSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalStaff()).isEqualTo(3L);
        assertThat(summary.getActiveStaff()).isEqualTo(1L);
        assertThat(summary.getMaxLabStaff()).isEqualTo(2);
        assertThat(summary.getRemainingSlots()).isEqualTo(1);
        assertThat(summary.isLimitReached()).isFalse();
        assertThat(summary.getPlanCode()).isEqualTo("STARTER");
    }

    @Test
    @DisplayName("Deactivate staff succeeds: sets status INACTIVE, records inactiveAt/deactivatedBy, and revokes refresh tokens immediately")
    void deactivateLabStaff_success_revokesRefreshTokensAndSetsMetadata() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);

        User activeStaff = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-STAFF-1")
                .name("Technician One")
                .email("tech1@alpha.com")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build();

        when(userRepository.findByRefIdAndOrganization_Id("USR-STAFF-1", orgA.getId())).thenReturn(Optional.of(activeStaff));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = labStaffService.deactivateLabStaff("USR-STAFF-1");

        assertThat(response.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(response.getInactiveAt()).isNotNull();
        assertThat(response.getDeactivatedByName()).isEqualTo("Admin Alpha");
        assertThat(response.getEligibleForCleanupAt()).isNotNull();
        // 10 days in future
        assertThat(response.getEligibleForCleanupAt()).isAfter(Instant.now().plus(9, ChronoUnit.DAYS));

        // Verify refresh tokens were immediately revoked
        verify(refreshTokenRepository, times(1)).revokeAllActiveByUserId(eq(activeStaff.getId()), any(Instant.class));
        verify(userRepository, times(1)).save(activeStaff);
    }

    @Test
    @DisplayName("Deactivate staff fails when staff member is already INACTIVE")
    void deactivateLabStaff_fails_whenAlreadyInactive() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);

        User inactiveStaff = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-STAFF-INACTIVE")
                .name("Inactive Tech")
                .email("inactive@alpha.com")
                .role(Role.LAB_STAFF)
                .status(UserStatus.INACTIVE)
                .organization(orgA)
                .build();

        when(userRepository.findByRefIdAndOrganization_Id("USR-STAFF-INACTIVE", orgA.getId())).thenReturn(Optional.of(inactiveStaff));

        assertThatThrownBy(() -> labStaffService.deactivateLabStaff("USR-STAFF-INACTIVE"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already inactive");

        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any(), any());
    }

    @Test
    @DisplayName("IDOR prevention: Org Admin cannot deactivate staff belonging to another organization")
    void deactivateLabStaff_fails_whenTargetBelongsToAnotherOrganization() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);

        when(userRepository.findByRefIdAndOrganization_Id("USR-STAFF-B", orgA.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labStaffService.deactivateLabStaff("USR-STAFF-B"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any(), any());
    }

    @Test
    @DisplayName("Plan downgrade does not automatically deactivate staff; correctly reflects over-limit state")
    void planDowngrade_doesNotDeactivateStaff_showsOverLimitInSummary() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);

        // Downgraded license with maxLabStaff = 1
        Plan downgradedPlan = Plan.builder()
                .code("MINIMAL")
                .name("Minimal Plan")
                .maxLabStaff(1)
                .build();

        License downgradedLicense = License.builder()
                .organization(orgA)
                .plan(downgradedPlan)
                .status(LicenseStatus.ACTIVE)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        when(licenseRepository.findWithPlanByOrganizationId(orgA.getId())).thenReturn(Optional.of(downgradedLicense));
        // Organization currently has 3 active staff
        when(userRepository.countByOrganization_IdAndRole(orgA.getId(), Role.LAB_STAFF)).thenReturn(3L);
        when(userRepository.countByOrganization_IdAndRoleAndStatus(orgA.getId(), Role.LAB_STAFF, UserStatus.ACTIVE)).thenReturn(3L);

        LabStaffSummaryResponse summary = labStaffService.getLabStaffSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.getActiveStaff()).isEqualTo(3L);
        assertThat(summary.getMaxLabStaff()).isEqualTo(1);
        assertThat(summary.getRemainingSlots()).isEqualTo(0);
        assertThat(summary.isLimitReached()).isTrue();
    }

    @Test
    @DisplayName("SUPER_ADMIN is forbidden from managing lab staff directly through organization-admin endpoints")
    void superAdmin_cannotManageLabStaff_throwsForbidden() {
        User superAdmin = User.builder()
                .id(UUID.randomUUID())
                .name("Super Admin")
                .email("superadmin@swasthai.com")
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(superAdmin);

        CreateLabStaffRequest request = CreateLabStaffRequest.builder()
                .name("Test Staff")
                .email("test@swasthai.com")
                .password("Password123")
                .confirmPassword("Password123")
                .build();

        assertThatThrownBy(() -> labStaffService.createLabStaff(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only ORG_ADMIN can manage lab staff");

        assertThatThrownBy(() -> labStaffService.deactivateLabStaff("USR-ANY"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only ORG_ADMIN can manage lab staff");
    }

    @Test
    @DisplayName("Audit log is recorded when a staff member is created")
    void createLabStaff_recordsSecurityAuditLog() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);
        when(userRepository.existsByEmailIgnoreCase("newtech@alpha.com")).thenReturn(false);
        when(licenseRepository.findByOrganizationIdForUpdate(orgA.getId())).thenReturn(Optional.of(activeLicenseA));
        when(userRepository.countByOrganization_IdAndRoleAndStatus(orgA.getId(), Role.LAB_STAFF, UserStatus.ACTIVE)).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("hashed_pw");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setRefId("USR-NEW-1");
            return u;
        });

        CreateLabStaffRequest request = CreateLabStaffRequest.builder()
                .name("New Tech")
                .email("newtech@alpha.com")
                .password("StrongPass123")
                .confirmPassword("StrongPass123")
                .build();

        labStaffService.createLabStaff(request);

        verify(auditLogService, times(1)).recordStaffAction(
                eq(orgAdminA),
                eq(orgA),
                any(User.class),
                eq("LAB_STAFF_CREATED"),
                contains("Created active lab staff account"),
                eq(true),
                isNull(),
                any()
        );
    }

    @Test
    @DisplayName("Audit log is recorded when a staff member is deactivated")
    void deactivateLabStaff_recordsSecurityAuditLog() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);

        User activeStaff = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-STAFF-1")
                .name("Technician One")
                .email("tech1@alpha.com")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build();

        when(userRepository.findByRefIdAndOrganization_Id("USR-STAFF-1", orgA.getId())).thenReturn(Optional.of(activeStaff));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        labStaffService.deactivateLabStaff("USR-STAFF-1");

        verify(auditLogService, times(1)).recordStaffAction(
                eq(orgAdminA),
                eq(orgA),
                eq(activeStaff),
                eq("LAB_STAFF_DEACTIVATED"),
                contains("Deactivated lab staff account"),
                eq(true),
                isNull(),
                any()
        );
    }

    @Test
    @DisplayName("Audit log is recorded when a staff member is reactivated")
    void reactivateLabStaff_recordsSecurityAuditLog() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminA);

        User inactiveStaff = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-STAFF-1")
                .name("Technician One")
                .email("tech1@alpha.com")
                .role(Role.LAB_STAFF)
                .status(UserStatus.INACTIVE)
                .organization(orgA)
                .build();

        when(userRepository.findByRefIdAndOrganization_Id("USR-STAFF-1", orgA.getId())).thenReturn(Optional.of(inactiveStaff));
        when(licenseRepository.findByOrganizationIdForUpdate(orgA.getId())).thenReturn(Optional.of(activeLicenseA));
        when(userRepository.countByOrganization_IdAndRoleAndStatus(orgA.getId(), Role.LAB_STAFF, UserStatus.ACTIVE)).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .status(UserStatus.ACTIVE)
                .build();

        labStaffService.updateLabStaffStatus("USR-STAFF-1", request);

        verify(auditLogService, times(1)).recordStaffAction(
                eq(orgAdminA),
                eq(orgA),
                eq(inactiveStaff),
                eq("LAB_STAFF_REACTIVATED"),
                contains("Reactivated lab staff account"),
                eq(true),
                isNull(),
                any()
        );
    }
}

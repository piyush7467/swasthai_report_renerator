package com.swasthai.report_generator.security;

import com.swasthai.report_generator.auth.service.CurrentOrganizationService;
import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.auth.service.impl.CurrentOrganizationServiceImpl;
import com.swasthai.report_generator.common.exception.ForbiddenException;
import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.organization.service.OrganizationSequenceService;
import com.swasthai.report_generator.patient.dto.response.PatientResponse;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.entity.Salutation;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.patient.service.PatientService;
import com.swasthai.report_generator.patient.service.impl.PatientServiceImpl;
import com.swasthai.report_generator.user.dto.request.CreateUserRequest;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.repository.UserRepository;
import com.swasthai.report_generator.user.service.UserService;
import com.swasthai.report_generator.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantIsolationAndRbacTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private com.swasthai.report_generator.report.repository.ReportRepository reportRepository;

    @Mock
    private CurrentOrganizationService currentOrganizationService;

    @Mock
    private OrganizationSequenceService organizationSequenceService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    private PatientService patientService;
    private UserService userService;
    private CurrentOrganizationService currentOrgServiceImpl;

    private Organization orgA;
    private Organization orgB;

    @BeforeEach
    void setUp() {

        patientService = new PatientServiceImpl(
                patientRepository,
                reportRepository,
                currentOrganizationService,
                currentUserService,
                organizationSequenceService
        );

        userService = new UserServiceImpl(
                userRepository,
                organizationRepository,
                passwordEncoder,
                currentUserService
        );

        currentOrgServiceImpl =
                new CurrentOrganizationServiceImpl(
                        currentUserService
                );

        orgA = Organization.builder()
                .id(UUID.randomUUID())
                .name("Alpha Diagnostics")
                .code("ALPHA")
                .status(OrganizationStatus.ACTIVE)
                .build();

        orgA.setRefId("ORG-ALPHA-01");

        orgB = Organization.builder()
                .id(UUID.randomUUID())
                .name("Beta Labs")
                .code("BETA")
                .status(OrganizationStatus.ACTIVE)
                .build();

        orgB.setRefId("ORG-BETA-001");
    }

    // ============================================================
    // TENANT ISOLATION
    // ============================================================

    @Test
    @DisplayName(
            "Tenant Isolation: User in Org A querying Patient belonging "
                    + "to Org B receives 404 ResourceNotFoundException"
    )
    void testTenantIsolation_OrgACannotQueryOrgBPatient() {

        when(currentOrganizationService.getCurrentOrganization())
                .thenReturn(orgA);

        when(
                patientRepository
                        .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                                "PAT-999",
                                orgA.getId()
                        )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> patientService.getPatient("PAT-999")
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found");

        verify(patientRepository)
                .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                        "PAT-999",
                        orgA.getId()
                );

        verify(patientRepository, never())
                .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                        "PAT-999",
                        orgB.getId()
                );
    }

    @Test
    @DisplayName(
            "Tenant Isolation: User in Org A can successfully access "
                    + "Patient belonging to Org A"
    )
    void testTenantIsolation_OrgACanQueryOwnPatient() {

        when(currentOrganizationService.getCurrentOrganization())
                .thenReturn(orgA);

        Patient patientA = Patient.builder()
                .id(UUID.randomUUID())
                .organization(orgA)
                .patientCode("PAT-ALPHA-0001")
                .salutation(Salutation.MR)
                .name("John Doe")
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .build();

        patientA.setRefId("PAT-001");

        when(
                patientRepository
                        .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                                "PAT-001",
                                orgA.getId()
                        )
        ).thenReturn(Optional.of(patientA));

        PatientResponse response =
                patientService.getPatient("PAT-001");

        assertThat(response).isNotNull();
        assertThat(response.getRefId())
                .isEqualTo("PAT-001");

        assertThat(response.getName())
                .isEqualTo("John Doe");

        assertThat(response.getOrganizationRefId())
                .isEqualTo("ORG-ALPHA-01");
    }

    @Test
    @DisplayName(
            "Tenant Isolation: Paginated patient queries are strictly "
                    + "scoped to caller's organization ID"
    )
    void testTenantIsolation_PaginatedListingStrictlyTenantScoped() {

        when(currentOrganizationService.getCurrentOrganization())
                .thenReturn(orgA);

        Patient patientA = Patient.builder()
                .id(UUID.randomUUID())
                .organization(orgA)
                .patientCode("PAT-ALPHA-0001")
                .salutation(Salutation.MR)
                .name("John Doe")
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .build();

        patientA.setRefId("PAT-001");

        Page<Patient> page =
                new PageImpl<>(List.of(patientA));

        when(
                patientRepository
                        .findAllByOrganization_IdAndDeletedAtIsNull(
                                eq(orgA.getId()),
                                any(Pageable.class)
                        )
        ).thenReturn(page);

        var result =
                patientService.getPatients(
                        0,
                        10,
                        "createdAt",
                        "desc",
                        null
                );

        assertThat(result.getContent())
                .hasSize(1);

        ArgumentCaptor<UUID> orgIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        verify(patientRepository)
                .findAllByOrganization_IdAndDeletedAtIsNull(
                        orgIdCaptor.capture(),
                        any(Pageable.class)
                );

        assertThat(orgIdCaptor.getValue())
                .isEqualTo(orgA.getId());
    }

    // ============================================================
    // SUPER ADMIN ORGANIZATION ISOLATION
    // ============================================================

    @Test
    @DisplayName(
            "Tenant Security: SUPER_ADMIN has no organization and "
                    + "cannot call organization-scoped services"
    )
    void testSuperAdmin_CannotAccessOrgScopedServices() {

        User superAdmin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@swasthai.com")
                .role(Role.SUPER_ADMIN)
                .organization(null)
                .build();

        when(currentUserService.getCurrentUser())
                .thenReturn(superAdmin);

        assertThatThrownBy(
                () -> currentOrgServiceImpl.getCurrentOrganization()
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "SUPER_ADMIN is not associated with an organization"
                );
    }

    // ============================================================
    // USER RBAC
    // ============================================================

    @Test
    @DisplayName(
            "RBAC: ORG_ADMIN cannot create another ORG_ADMIN "
                    + "(only SUPER_ADMIN can)"
    )
    void testRbac_OrgAdminCannotCreateOrgAdmin() {

        User orgAdmin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@alpha.com")
                .role(Role.ORG_ADMIN)
                .organization(orgA)
                .build();

        when(currentUserService.getCurrentUser())
                .thenReturn(orgAdmin);

        when(
                userRepository
                        .existsByEmailIgnoreCase(
                                "newadmin@alpha.com"
                        )
        ).thenReturn(false);

        CreateUserRequest request =
                CreateUserRequest.builder()
                        .name("New Admin")
                        .email("newadmin@alpha.com")
                        .password("SecurePass123!")
                        .role(Role.ORG_ADMIN)
                        .organizationRefId(orgA.getRefId())
                        .build();

        assertThatThrownBy(
                () -> userService.createUser(request)
        )
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining(
                        "Only SUPER_ADMIN can create an ORG_ADMIN"
                );
    }

    @Test
    @DisplayName(
            "RBAC: LAB_STAFF cannot create any users"
    )
    void testRbac_LabStaffCannotCreateUsers() {

        User labStaff = User.builder()
                .id(UUID.randomUUID())
                .email("staff@alpha.com")
                .role(Role.LAB_STAFF)
                .organization(orgA)
                .build();

        when(currentUserService.getCurrentUser())
                .thenReturn(labStaff);

        when(
                userRepository
                        .existsByEmailIgnoreCase(
                                "tech@alpha.com"
                        )
        ).thenReturn(false);

        CreateUserRequest request =
                CreateUserRequest.builder()
                        .name("New Tech")
                        .email("tech@alpha.com")
                        .password("SecurePass123!")
                        .role(Role.LAB_STAFF)
                        .build();

        assertThatThrownBy(
                () -> userService.createUser(request)
        )
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining(
                        "LAB_STAFF cannot create users"
                );
    }

    @Test
    @DisplayName(
            "RBAC: Only one SUPER_ADMIN is allowed in the entire system"
    )
    void testRbac_SingleSuperAdminConstraint() {

        User superAdmin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@swasthai.com")
                .role(Role.SUPER_ADMIN)
                .organization(null)
                .build();

        when(currentUserService.getCurrentUser())
                .thenReturn(superAdmin);

        when(
                userRepository
                        .existsByEmailIgnoreCase(
                                "admin2@swasthai.com"
                        )
        ).thenReturn(false);

        CreateUserRequest request =
                CreateUserRequest.builder()
                        .name("Second Super Admin")
                        .email("admin2@swasthai.com")
                        .password("SecurePass123!")
                        .role(Role.SUPER_ADMIN)
                        .build();

        assertThatThrownBy(
                () -> userService.createUser(request)
        )
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining(
                        "Only one SUPER_ADMIN is allowed"
                );
    }
}
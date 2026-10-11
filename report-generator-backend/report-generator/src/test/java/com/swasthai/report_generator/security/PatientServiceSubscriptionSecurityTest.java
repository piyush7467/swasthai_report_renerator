package com.swasthai.report_generator.security;

import com.swasthai.report_generator.auth.service.CurrentOrganizationService;
import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.license.service.LicenseGuard;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.service.OrganizationSequenceService;
import com.swasthai.report_generator.patient.dto.request.CreatePatientRequest;
import com.swasthai.report_generator.patient.dto.request.UpdatePatientRequest;
import com.swasthai.report_generator.patient.dto.response.PatientResponse;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.entity.Salutation;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.patient.service.PatientService;
import com.swasthai.report_generator.patient.service.impl.PatientServiceImpl;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceSubscriptionSecurityTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private CurrentOrganizationService currentOrganizationService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private OrganizationSequenceService organizationSequenceService;

    @Mock
    private LicenseGuard licenseGuard;

    private PatientService patientService;

    private Organization activeOrg;
    private User orgAdminUser;

    @BeforeEach
    void setUp() {
        patientService = new PatientServiceImpl(
                patientRepository,
                reportRepository,
                currentOrganizationService,
                currentUserService,
                organizationSequenceService,
                licenseGuard
        );

        activeOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Alpha Diagnostics")
                .code("ALPHA")
                .status(OrganizationStatus.ACTIVE)
                .build();

        orgAdminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@alpha.com")
                .name("Admin Alpha")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(activeOrg)
                .build();
    }

    @Test
    @DisplayName("SEC-SUB-001: Unlicensed organization is blocked from creating patients; zero sequence consumed, zero entities saved")
    void testUnlicensedOrganization_PatientCreationBlocked() {
        when(currentOrganizationService.getCurrentOrganization()).thenReturn(activeOrg);
        doThrow(new AccessDeniedException("An active license is required to perform operational workflows."))
                .when(licenseGuard).requireActiveLicenseForOrganization(activeOrg.getId());

        CreatePatientRequest req = CreatePatientRequest.builder()
                .name("John Doe")
                .salutation(Salutation.MR)
                .gender(Gender.MALE)
                .phone("9876543210")
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .build();

        assertThatThrownBy(() -> patientService.createPatient(req))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("An active license is required");

        // Verify sequence generation was NEVER invoked
        verify(organizationSequenceService, never()).generatePatientCode(any());
        // Verify patient repository save was NEVER invoked
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("SEC-SUB-002: Expired organization is blocked from creating patients")
    void testExpiredOrganization_PatientCreationBlocked() {
        when(currentOrganizationService.getCurrentOrganization()).thenReturn(activeOrg);
        doThrow(new AccessDeniedException("Your organization license has expired. Please renew the license to continue."))
                .when(licenseGuard).requireActiveLicenseForOrganization(activeOrg.getId());

        CreatePatientRequest req = CreatePatientRequest.builder()
                .name("Jane Doe")
                .salutation(Salutation.MRS)
                .gender(Gender.FEMALE)
                .phone("9876543211")
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1992, 8, 15))
                .build();

        assertThatThrownBy(() -> patientService.createPatient(req))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("license has expired");

        verify(organizationSequenceService, never()).generatePatientCode(any());
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("SEC-SUB-003: Active licensed organization successfully creates patient")
    void testActiveLicensedOrganization_PatientCreationSuccess() {
        when(currentOrganizationService.getCurrentOrganization()).thenReturn(activeOrg);
        doNothing().when(licenseGuard).requireActiveLicenseForOrganization(activeOrg.getId());
        when(organizationSequenceService.generatePatientCode(activeOrg)).thenReturn("P-ALPHA-001");
        when(patientRepository.existsByPatientCodeAndOrganization_Id("P-ALPHA-001", activeOrg.getId())).thenReturn(false);

        Patient saved = Patient.builder()
                .id(UUID.randomUUID())
                .organization(activeOrg)
                .patientCode("P-ALPHA-001")
                .name("John Doe")
                .salutation(Salutation.MR)
                .gender(Gender.MALE)
                .phone("9876543210")
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .build();
        when(patientRepository.save(any(Patient.class))).thenReturn(saved);

        CreatePatientRequest req = CreatePatientRequest.builder()
                .name("John Doe")
                .salutation(Salutation.MR)
                .gender(Gender.MALE)
                .phone("9876543210")
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .build();

        PatientResponse response = patientService.createPatient(req);

        assertThat(response).isNotNull();
        assertThat(response.getPatientCode()).isEqualTo("P-ALPHA-001");
        assertThat(response.getName()).isEqualTo("John Doe");

        verify(licenseGuard).requireActiveLicenseForOrganization(activeOrg.getId());
        verify(organizationSequenceService).generatePatientCode(activeOrg);
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("SEC-SUB-004: Unlicensed/expired organization is blocked from updating patients")
    void testExpiredOrganization_PatientUpdateBlocked() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminUser);
        when(currentOrganizationService.getCurrentOrganization()).thenReturn(activeOrg);
        doThrow(new AccessDeniedException("Your organization license has expired. Please renew the license to continue."))
                .when(licenseGuard).requireActiveLicenseForOrganization(activeOrg.getId());

        UpdatePatientRequest req = UpdatePatientRequest.builder()
                .name("Updated Name")
                .salutation(Salutation.MR)
                .gender(Gender.MALE)
                .phone("9876543210")
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .build();

        assertThatThrownBy(() -> patientService.updatePatient("PAT-001", req))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("license has expired");

        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("SEC-SUB-005: Unlicensed/expired organization is blocked from deleting patients")
    void testExpiredOrganization_PatientDeleteBlocked() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminUser);
        when(currentOrganizationService.getCurrentOrganization()).thenReturn(activeOrg);
        doThrow(new AccessDeniedException("An active license is required to perform operational workflows."))
                .when(licenseGuard).requireActiveLicenseForOrganization(activeOrg.getId());

        assertThatThrownBy(() -> patientService.deletePatient("PAT-001"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("An active license is required");

        verify(patientRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("SEC-SUB-006: Unlicensed/expired organization is blocked from restoring patients")
    void testExpiredOrganization_PatientRestoreBlocked() {
        when(currentUserService.getCurrentUser()).thenReturn(orgAdminUser);
        when(currentOrganizationService.getCurrentOrganization()).thenReturn(activeOrg);
        doThrow(new AccessDeniedException("Your organization license has expired. Please renew the license to continue."))
                .when(licenseGuard).requireActiveLicenseForOrganization(activeOrg.getId());

        assertThatThrownBy(() -> patientService.restorePatient("PAT-001"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("license has expired");

        verify(patientRepository, never()).save(any());
    }
}

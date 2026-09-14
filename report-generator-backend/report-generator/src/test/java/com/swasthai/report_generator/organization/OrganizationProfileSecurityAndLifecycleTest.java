package com.swasthai.report_generator.organization;

import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.license.entity.License;
import com.swasthai.report_generator.license.entity.LicenseStatus;
import com.swasthai.report_generator.license.entity.Plan;
import com.swasthai.report_generator.license.repository.LicenseRepository;
import com.swasthai.report_generator.license.repository.PlanRepository;
import com.swasthai.report_generator.organization.dto.request.CreateOrganizationRequest;
import com.swasthai.report_generator.organization.dto.request.UpdateOrganizationProfileRequest;
import com.swasthai.report_generator.organization.dto.response.OrganizationProfileResponse;
import com.swasthai.report_generator.organization.dto.response.OrganizationResponse;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationProfile;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationProfileRepository;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.organization.service.OrganizationProfileService;
import com.swasthai.report_generator.organization.service.OrganizationService;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.entity.Salutation;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.dto.request.AddReportTestRequest;
import com.swasthai.report_generator.report.dto.request.CreateReportRequest;
import com.swasthai.report_generator.report.dto.request.UpdateReportParametersRequest;
import com.swasthai.report_generator.report.dto.response.ReportResponse;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.pdf.ReportPdfData;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.service.ReportService;
import com.swasthai.report_generator.storage.FileStorageService;
import com.swasthai.report_generator.test.dto.request.TestParameterResultInput;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.repository.OrganizationTestRepository;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrganizationProfileSecurityAndLifecycleTest {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationProfileService organizationProfileService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationProfileRepository organizationProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TestCategoryRepository testCategoryRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestParameterRepository testParameterRepository;

    @Autowired
    private OrganizationTestRepository organizationTestRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    private Organization orgA;
    private Organization orgB;
    private User superAdminUser;
    private User orgAdminA;
    private User labStaffA;
    private User orgAdminB;

    private com.swasthai.report_generator.test.entity.Test cbcTest;
    private TestParameter paramHgb;
    private Patient patientOrgA;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Setup Organizations
        orgA = organizationRepository.save(Organization.builder()
                .name("Alpha Diagnostics " + suffix)
                .code("ALP-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(Organization.builder()
                .name("Beta Labs " + suffix)
                .code("BET-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        // Setup Profiles
        organizationProfileRepository.save(OrganizationProfile.builder()
                .organization(orgA)
                .addressLine1("100 Alpha Boulevard")
                .city("Metropolis")
                .state("StateA")
                .country("CountryA")
                .phone("+91-1111111111")
                .email("contact@alpha.com")
                .reportFooterText("Alpha Footer Text")
                .reportDisclaimer("Alpha Disclaimer")
                .build());

        organizationProfileRepository.save(OrganizationProfile.builder()
                .organization(orgB)
                .addressLine1("200 Beta Avenue")
                .city("Gotham")
                .state("StateB")
                .country("CountryB")
                .phone("+91-2222222222")
                .email("contact@beta.com")
                .build());

        // Users
        superAdminUser = userRepository.findByEmailIgnoreCase("admin@swasthai.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("super-" + suffix + "@admin.com")
                        .name("Master Admin")
                        .passwordHash("pwd")
                        .role(Role.SUPER_ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build()));

        orgAdminA = userRepository.save(User.builder()
                .email("admin-a-" + suffix + "@alpha.com")
                .name("Dr. Alice OrgAdmin")
                .passwordHash("pwd")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build());

        labStaffA = userRepository.save(User.builder()
                .email("staff-a-" + suffix + "@alpha.com")
                .name("Bob Staff")
                .passwordHash("pwd")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build());

        orgAdminB = userRepository.save(User.builder()
                .email("admin-b-" + suffix + "@beta.com")
                .name("Dr. Bruce OrgAdmin")
                .passwordHash("pwd")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(orgB)
                .build());

        // Plan & License
        Plan plan = planRepository.findByCodeIgnoreCase("STARTER")
                .orElseGet(() -> planRepository.save(Plan.builder()
                        .code("STARTER")
                        .name("Starter Plan")
                        .annualPrice(new BigDecimal("9990.00"))
                        .currency("INR")
                        .active(true)
                        .build()));

        licenseRepository.save(License.builder()
                .organization(orgA)
                .plan(plan)
                .status(LicenseStatus.ACTIVE)
                .startedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().plusSeconds(86400 * 365))
                .build());

        // Patient
        patientOrgA = patientRepository.save(Patient.builder()
                .organization(orgA)
                .salutation(Salutation.MR)
                .name("John Patient")
                .patientCode("PA-" + suffix)
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .build());

        // Catalog
        TestCategory category = testCategoryRepository.save(TestCategory.builder()
                .name("General " + suffix)
                .code("GEN-" + suffix)
                .build());

        cbcTest = testRepository.save(com.swasthai.report_generator.test.entity.Test.builder()
                .name("Blood Test " + suffix)
                .code("BLD-" + suffix)
                .shortName("BLD")
                .category(category)
                .sampleType(SampleType.WHOLE_BLOOD)
                .specimenContainer("EDTA Vacutainer")
                .reportSection("HEMATOLOGY")
                .status(TestStatus.ACTIVE)
                .version(1)
                .build());

        paramHgb = testParameterRepository.save(TestParameter.builder()
                .test(cbcTest)
                .code("HGB")
                .name("Hemoglobin")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .unit("g/dL")
                .referenceMin(new BigDecimal("12.0"))
                .referenceMax(new BigDecimal("16.0"))
                .displayOrder(1)
                .required(true)
                .status(TestParameterStatus.ACTIVE)
                .build());

        organizationTestRepository.save(OrganizationTest.builder()
                .organization(orgA)
                .test(cbcTest)
                .status(OrganizationTestStatus.ACTIVE)
                .build());
    }

    private void authenticateUser(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getRefId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        auth.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockMultipartFile createMockPng(String name) {
        byte[] minimalPng = new byte[]{
                (byte) 137, 80, 78, 71, 13, 10, 26, 10, // PNG signature
                0, 0, 0, 13, 73, 72, 68, 82,            // IHDR chunk header
                0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0,  // 1x1 pixel RGBA
                31, 21, -60, -119,                      // CRC
                0, 0, 0, 10, 73, 68, 65, 84,            // IDAT chunk
                120, -100, 99, 0, 1, 0, 0, 5, 0, 1,     // compressed data
                13, 10, 45, -75,                        // CRC
                0, 0, 0, 0, 73, 69, 78, 68,             // IEND chunk
                -82, 66, 96, -126                       // CRC
        };
        return new MockMultipartFile(name, "test.png", "image/png", minimalPng);
    }

    @Test
    @DisplayName("1. Organization creation automatically provisions an empty OrganizationProfile in same transaction")
    void testOrganizationCreation_AutomaticallyCreatesProfile() {
        authenticateUser(superAdminUser);

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        CreateOrganizationRequest createReq = new CreateOrganizationRequest();
        createReq.setName("Automatic Profile Org " + suffix);
        createReq.setCode("APO-" + suffix);

        OrganizationResponse newOrg = organizationService.createOrganization(createReq);

        Organization orgEntity = organizationRepository.findByRefId(newOrg.getRefId()).orElseThrow();
        OrganizationProfile profile = organizationProfileRepository.findByOrganization_Id(orgEntity.getId()).orElse(null);

        assertThat(profile).isNotNull();
        assertThat(profile.getOrganization().getId()).isEqualTo(orgEntity.getId());
        assertThat(profile.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("2. Missing organization profile is lazily healed without 500 error")
    void testMissingProfile_LazilyHealed() {
        authenticateUser(orgAdminA);

        // Delete Org A's profile directly from DB to simulate legacy org
        organizationProfileRepository.deleteAll();

        // getMyProfile should lazily create it rather than throw
        OrganizationProfileResponse profileResponse = organizationProfileService.getMyProfile();
        assertThat(profileResponse).isNotNull();
        assertThat(profileResponse.getOrganizationRefId()).isEqualTo(orgA.getRefId());
    }

    @Test
    @DisplayName("3. ORG_ADMIN can read and update own profile, but LAB_STAFF cannot update")
    void testProfileRbac_OrgAdminCanUpdate_LabStaffForbidden() {
        authenticateUser(orgAdminA);
        OrganizationProfileResponse myProfile = organizationProfileService.getMyProfile();
        assertThat(myProfile.getOrganizationRefId()).isEqualTo(orgA.getRefId());

        UpdateOrganizationProfileRequest updateRequest = UpdateOrganizationProfileRequest.builder()
                .addressLine1("Updated Alpha Street")
                .city("New Metro")
                .phone("+91-9999988888")
                .website("https://alpha-lab.com")
                .reportFooterText("Custom footer")
                .build();

        OrganizationProfileResponse updated = organizationProfileService.updateMyProfile(updateRequest);
        assertThat(updated.getAddressLine1()).isEqualTo("Updated Alpha Street");
        assertThat(updated.getCity()).isEqualTo("New Metro");
        assertThat(updated.getPhone()).isEqualTo("+91-9999988888");
        assertThat(updated.getWebsite()).isEqualTo("https://alpha-lab.com");

        // LAB_STAFF can read
        authenticateUser(labStaffA);
        OrganizationProfileResponse staffRead = organizationProfileService.getMyProfile();
        assertThat(staffRead.getAddressLine1()).isEqualTo("Updated Alpha Street");

        // LAB_STAFF cannot update
        assertThatThrownBy(() -> organizationProfileService.updateMyProfile(updateRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only ORG_ADMIN");
    }

    @Test
    @DisplayName("4. Tenant isolation: ORG_ADMIN cannot access another organization's profile")
    void testTenantIsolation_OrgAdminCannotAccessAnotherOrg() {
        authenticateUser(orgAdminB);

        // OrgAdmin B cannot call SUPER_ADMIN endpoint for Org A
        assertThatThrownBy(() -> organizationProfileService.getProfileByOrganizationRefId(orgA.getRefId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only SUPER_ADMIN");

        assertThatThrownBy(() -> organizationProfileService.updateProfileByOrganizationRefId(orgA.getRefId(), new UpdateOrganizationProfileRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only SUPER_ADMIN");
    }

    @Test
    @DisplayName("5. SUPER_ADMIN can manage profiles for any organization")
    void testSuperAdmin_CanManageAnyProfile() {
        authenticateUser(superAdminUser);

        OrganizationProfileResponse profileA = organizationProfileService.getProfileByOrganizationRefId(orgA.getRefId());
        assertThat(profileA.getOrganizationRefId()).isEqualTo(orgA.getRefId());

        UpdateOrganizationProfileRequest updateRequest = UpdateOrganizationProfileRequest.builder()
                .addressLine1("Super Admin Updated Address")
                .city("Super City")
                .build();

        OrganizationProfileResponse updated = organizationProfileService.updateProfileByOrganizationRefId(orgA.getRefId(), updateRequest);
        assertThat(updated.getAddressLine1()).isEqualTo("Super Admin Updated Address");
    }

    @Test
    @DisplayName("6. Initial ORG_ADMIN signature upload captures signatureOwnerRefId")
    void testOrgAdminSignatureUpload_CapturesOwnerRefId() {
        authenticateUser(orgAdminA);

        MockMultipartFile sigFile = createMockPng("file");
        OrganizationProfileResponse response = organizationProfileService.uploadMySignature(sigFile);

        assertThat(response.isSignatureConfigured()).isTrue();
        assertThat(response.getSignatureOwnerRefId()).isEqualTo(orgAdminA.getRefId());

        OrganizationProfile entity = organizationProfileRepository.findByOrganization_Id(orgA.getId()).orElseThrow();
        assertThat(entity.getSignatureOwnerRefId()).isEqualTo(orgAdminA.getRefId());
        assertThat(entity.getSignatureStorageKey()).isNotNull();
    }

    @Test
    @DisplayName("7. SUPER_ADMIN cannot upload signature if no initial ORG_ADMIN owner exists")
    void testSuperAdmin_CannotUploadSignature_WithoutOwner() {
        authenticateUser(superAdminUser);

        // Ensure Org A has no signature owner
        OrganizationProfile profile = organizationProfileRepository.findByOrganization_Id(orgA.getId()).orElseThrow();
        profile.setSignatureOwnerRefId(null);
        profile.setSignatureStorageKey(null);
        organizationProfileRepository.saveAndFlush(profile);

        MockMultipartFile sigFile = createMockPng("file");
        assertThatThrownBy(() -> organizationProfileService.uploadSignatureForOrganization(orgA.getRefId(), sigFile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot upload signature without an authorized organization signature owner");
    }

    @Test
    @DisplayName("8. SUPER_ADMIN replaces signature while preserving existing signatureOwnerRefId")
    void testSuperAdmin_ReplacesSignature_PreservingOwnerRefId() {
        // First ORG_ADMIN uploads initial signature
        authenticateUser(orgAdminA);
        organizationProfileService.uploadMySignature(createMockPng("file"));

        // Then SUPER_ADMIN replaces signature
        authenticateUser(superAdminUser);
        MockMultipartFile newSig = createMockPng("file");
        OrganizationProfileResponse response = organizationProfileService.uploadSignatureForOrganization(orgA.getRefId(), newSig);

        assertThat(response.isSignatureConfigured()).isTrue();
        assertThat(response.getSignatureOwnerRefId()).isEqualTo(orgAdminA.getRefId());
    }

    @Test
    @DisplayName("9. Replacing or deleting logo keeps the old physical file in storage")
    void testLogoReplacementAndDeletion_KeepsOldPhysicalFiles() {
        authenticateUser(orgAdminA);

        // Upload logo 1
        organizationProfileService.uploadMyLogo(createMockPng("file"));
        OrganizationProfile profile1 = organizationProfileRepository.findByOrganization_Id(orgA.getId()).orElseThrow();
        String logoKey1 = profile1.getLogoStorageKey();
        assertThat(fileStorageService.exists(logoKey1)).isTrue();

        // Upload logo 2 (replaces logo 1 in profile)
        organizationProfileService.uploadMyLogo(createMockPng("file"));
        OrganizationProfile profile2 = organizationProfileRepository.findByOrganization_Id(orgA.getId()).orElseThrow();
        String logoKey2 = profile2.getLogoStorageKey();
        assertThat(logoKey2).isNotEqualTo(logoKey1);

        // Invariant: replacing logo keeps the old file
        assertThat(fileStorageService.exists(logoKey1)).isTrue();
        assertThat(fileStorageService.exists(logoKey2)).isTrue();

        // Delete logo
        organizationProfileService.deleteMyLogo();
        OrganizationProfile profile3 = organizationProfileRepository.findByOrganization_Id(orgA.getId()).orElseThrow();
        assertThat(profile3.getLogoStorageKey()).isNull();

        // Invariant: deleting logo keeps the old files
        assertThat(fileStorageService.exists(logoKey1)).isTrue();
        assertThat(fileStorageService.exists(logoKey2)).isTrue();
    }

    @Test
    @DisplayName("9b. Replacing or deleting signature keeps the old physical file in storage")
    void testSignatureReplacementAndDeletion_KeepsOldPhysicalFiles() {
        authenticateUser(orgAdminA);

        // Upload signature 1
        organizationProfileService.uploadMySignature(createMockPng("file"));
        OrganizationProfile profile1 = organizationProfileRepository.findByOrganization_Id(orgA.getId()).orElseThrow();
        String sigKey1 = profile1.getSignatureStorageKey();
        assertThat(fileStorageService.exists(sigKey1)).isTrue();

        // Upload signature 2 (replaces signature 1 in profile)
        organizationProfileService.uploadMySignature(createMockPng("file"));
        OrganizationProfile profile2 = organizationProfileRepository.findByOrganization_Id(orgA.getId()).orElseThrow();
        String sigKey2 = profile2.getSignatureStorageKey();
        assertThat(sigKey2).isNotEqualTo(sigKey1);

        // Invariant: replacing signature keeps the old file
        assertThat(fileStorageService.exists(sigKey1)).isTrue();
        assertThat(fileStorageService.exists(sigKey2)).isTrue();

        // Delete signature
        organizationProfileService.deleteMySignature();
        OrganizationProfile profile3 = organizationProfileRepository.findByOrganization_Id(orgA.getId()).orElseThrow();
        assertThat(profile3.getSignatureStorageKey()).isNull();

        // Invariant: deleting signature keeps the old files
        assertThat(fileStorageService.exists(sigKey1)).isTrue();
        assertThat(fileStorageService.exists(sigKey2)).isTrue();
    }

    @Test
    @DisplayName("10. Deleting signature clears signatureStorageKey and signatureOwnerRefId")
    void testDeleteSignature_ClearsKeyAndOwner() {
        authenticateUser(orgAdminA);

        organizationProfileService.uploadMySignature(createMockPng("file"));
        OrganizationProfileResponse activeSig = organizationProfileService.getMyProfile();
        assertThat(activeSig.isSignatureConfigured()).isTrue();
        assertThat(activeSig.getSignatureOwnerRefId()).isNotNull();

        organizationProfileService.deleteMySignature();
        OrganizationProfileResponse deletedSig = organizationProfileService.getMyProfile();
        assertThat(deletedSig.isSignatureConfigured()).isFalse();
        assertThat(deletedSig.getSignatureOwnerRefId()).isNull();
    }

    @Test
    @DisplayName("11. Report finalization snapshots profile branding and contact details into Report entity")
    void testFinalization_SnapshotsOrganizationProfile() {
        authenticateUser(orgAdminA);
        organizationProfileService.uploadMyLogo(createMockPng("file"));
        organizationProfileService.uploadMySignature(createMockPng("file"));

        // Create and finalize report
        authenticateUser(labStaffA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String testRefId = report.tests().get(0).refId();

        UpdateReportParametersRequest updateReq = new UpdateReportParametersRequest(
                null,
                List.of(new TestParameterResultInput(paramHgb.getRefId(), "HGB", "14.5"))
        );
        report = reportService.updateParameterValues(report.refId(), testRefId, updateReq);
        ReportResponse finalized = reportService.finalizeReport(report.refId());

        Report entity = reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull(finalized.refId(), orgA.getId()).orElseThrow();
        assertThat(entity.getOrganizationAddressLine1()).isEqualTo("100 Alpha Boulevard");
        assertThat(entity.getOrganizationCity()).isEqualTo("Metropolis");
        assertThat(entity.getOrganizationLogoStorageKey()).isNotNull();
        assertThat(entity.getOrganizationSignatureStorageKey()).isNotNull();
        assertThat(entity.getOrganizationSignatureOwnerName()).isEqualTo(orgAdminA.getName());
        assertThat(entity.getOrganizationSignatureOwnerEmail()).isEqualTo(orgAdminA.getEmail());
        assertThat(entity.getOrganizationReportFooterText()).isEqualTo("Alpha Footer Text");
        assertThat(entity.getOrganizationReportDisclaimer()).isEqualTo("Alpha Disclaimer");
    }

    @Test
    @DisplayName("12. Historical immutability: Altering profile after finalization leaves past reports and PDFs unchanged")
    void testHistoricalImmutability_ReportPreservesOldProfileSnapshot() {
        authenticateUser(orgAdminA);
        organizationProfileService.uploadMyLogo(createMockPng("file"));
        organizationProfileService.uploadMySignature(createMockPng("file"));

        OrganizationProfile profileBefore = organizationProfileRepository.findByOrganization_Id(orgA.getId()).orElseThrow();
        String originalLogoKey = profileBefore.getLogoStorageKey();
        String originalSigKey = profileBefore.getSignatureStorageKey();

        // Finalize Report
        authenticateUser(labStaffA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String testRefId = report.tests().get(0).refId();

        UpdateReportParametersRequest updateReq = new UpdateReportParametersRequest(
                null,
                List.of(new TestParameterResultInput(paramHgb.getRefId(), "HGB", "14.5"))
        );
        report = reportService.updateParameterValues(report.refId(), testRefId, updateReq);
        ReportResponse finalized = reportService.finalizeReport(report.refId());

        // Now modify OrganizationProfile completely
        authenticateUser(orgAdminA);
        organizationProfileService.updateMyProfile(UpdateOrganizationProfileRequest.builder()
                .addressLine1("999 Brand New Boulevard")
                .city("Neo Metropolis")
                .reportFooterText("Brand New Footer")
                .build());
        organizationProfileService.deleteMyLogo();
        organizationProfileService.deleteMySignature();

        // Verify that past report still holds the old snapshots
        Report entity = reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull(finalized.refId(), orgA.getId()).orElseThrow();
        assertThat(entity.getOrganizationAddressLine1()).isEqualTo("100 Alpha Boulevard");
        assertThat(entity.getOrganizationCity()).isEqualTo("Metropolis");
        assertThat(entity.getOrganizationReportFooterText()).isEqualTo("Alpha Footer Text");
        assertThat(entity.getOrganizationLogoStorageKey()).isEqualTo(originalLogoKey);
        assertThat(entity.getOrganizationSignatureStorageKey()).isEqualTo(originalSigKey);
        assertThat(entity.getOrganizationSignatureOwnerName()).isEqualTo(orgAdminA.getName());

        // Verify physical storage files for the historical report still exist
        assertThat(fileStorageService.exists(originalLogoKey)).isTrue();
        assertThat(fileStorageService.exists(originalSigKey)).isTrue();

        // Verify that PDF DTO continues to use snapshotted data
        authenticateUser(labStaffA);
        ReportPdfData pdfData = reportService.getReportPdfData(finalized.refId());
        assertThat(pdfData.organization().addressLine1()).isEqualTo("100 Alpha Boulevard");
        assertThat(pdfData.organization().city()).isEqualTo("Metropolis");
        assertThat(pdfData.organization().reportFooterText()).isEqualTo("Alpha Footer Text");
        assertThat(pdfData.organization().logoStorageKey()).isEqualTo(originalLogoKey);
        assertThat(pdfData.organization().signatureStorageKey()).isEqualTo(originalSigKey);
        assertThat(pdfData.organization().signatureOwnerName()).isEqualTo(orgAdminA.getName());

        // Verify binary PDF renders successfully with historical assets
        byte[] pdfBytes = reportService.generateReportPdf(finalized.refId());
        assertThat(pdfBytes).isNotNull().isNotEmpty();
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");

        // Now upload a brand new signature for the organization
        authenticateUser(orgAdminA);
        organizationProfileService.uploadMySignature(createMockPng("file"));
        OrganizationProfile profileAfterNewSig = organizationProfileRepository.findByOrganization_Id(orgA.getId()).orElseThrow();
        String newSigKey = profileAfterNewSig.getSignatureStorageKey();
        assertThat(newSigKey).isNotEqualTo(originalSigKey);
        assertThat(fileStorageService.exists(originalSigKey)).isTrue();
        assertThat(fileStorageService.exists(newSigKey)).isTrue();

        // Finalized Report A still points to and renders its old signature even after organization changes signature
        authenticateUser(labStaffA);
        ReportPdfData pdfDataAfterOrgChange = reportService.getReportPdfData(finalized.refId());
        assertThat(pdfDataAfterOrgChange.organization().signatureStorageKey()).isEqualTo(originalSigKey);

        byte[] pdfBytesAfterOrgChange = reportService.generateReportPdf(finalized.refId());
        assertThat(pdfBytesAfterOrgChange).isNotNull().isNotEmpty();
        assertThat(new String(pdfBytesAfterOrgChange, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}

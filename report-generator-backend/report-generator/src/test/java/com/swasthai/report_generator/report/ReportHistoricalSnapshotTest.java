package com.swasthai.report_generator.report;

import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
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
import com.swasthai.report_generator.report.entity.ReportTestResult;
import com.swasthai.report_generator.report.pdf.ReportPdfData;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.service.ReportService;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.openpdf.text.pdf.PdfDictionary;
import org.openpdf.text.pdf.PdfName;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
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
class ReportHistoricalSnapshotTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private com.swasthai.report_generator.report.pdf.OpenPdfRenderer openPdfRenderer;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

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
    private com.swasthai.report_generator.license.repository.PlanRepository planRepository;

    @Autowired
    private com.swasthai.report_generator.license.repository.LicenseRepository licenseRepository;

    private Organization orgA;
    private Organization orgB;
    private User labStaffOrgA;
    private User labStaffOrgB;
    private Patient patientOrgA;
    private Patient patientOrgB;
    private com.swasthai.report_generator.license.entity.License licenseOrgA;

    private com.swasthai.report_generator.test.entity.Test cbcTest;
    private TestParameter paramHgb;
    private TestParameter paramRbc;
    private TestParameter paramHct;
    private TestParameter paramMcv;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Setup Organizations
        orgA = organizationRepository.save(Organization.builder()
                .name("Snapshot Lab Alpha " + suffix)
                .code("SNA-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(Organization.builder()
                .name("Snapshot Lab Beta " + suffix)
                .code("SNB-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        // Setup Plans & Licenses
        com.swasthai.report_generator.license.entity.Plan testPlan = planRepository.findByCodeIgnoreCase("STARTER")
                .orElseGet(() -> planRepository.save(com.swasthai.report_generator.license.entity.Plan.builder()
                        .code("STARTER")
                        .name("Starter Plan")
                        .annualPrice(new BigDecimal("9990.00"))
                        .currency("INR")
                        .active(true)
                        .build()));

        licenseOrgA = licenseRepository.save(com.swasthai.report_generator.license.entity.License.builder()
                .organization(orgA)
                .plan(testPlan)
                .status(com.swasthai.report_generator.license.entity.LicenseStatus.ACTIVE)
                .startedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().plusSeconds(86400 * 365))
                .build());

        licenseRepository.save(com.swasthai.report_generator.license.entity.License.builder()
                .organization(orgB)
                .plan(testPlan)
                .status(com.swasthai.report_generator.license.entity.LicenseStatus.ACTIVE)
                .startedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().plusSeconds(86400 * 365))
                .build());

        // Setup Users
        labStaffOrgA = userRepository.save(User.builder()
                .email("snapshot-staff-a-" + suffix + "@alpha.com")
                .name("Dr. Alpha Staff")
                .passwordHash("hashed")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build());

        labStaffOrgB = userRepository.save(User.builder()
                .email("snapshot-staff-b-" + suffix + "@beta.com")
                .name("Dr. Beta Staff")
                .passwordHash("hashed")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgB)
                .build());

        // Setup Patients
        patientOrgA = patientRepository.save(Patient.builder()
                .organization(orgA)
                .salutation(Salutation.MR)
                .name("Alexander Fleming")
                .patientCode("PA-" + suffix)
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .gender(Gender.MALE)
                .phone("+91-9876543210")
                .email("fleming@alpha.com")
                .address("123 Baker Street, London")
                .weightKg(new BigDecimal("72.500"))
                .build());

        patientOrgB = patientRepository.save(Patient.builder()
                .organization(orgB)
                .salutation(Salutation.MS)
                .name("Marie Curie")
                .patientCode("PB-" + suffix)
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1992, 11, 7))
                .gender(Gender.FEMALE)
                .phone("+91-9876543211")
                .email("curie@beta.com")
                .address("456 Rue Curie, Paris")
                .weightKg(new BigDecimal("55.000"))
                .build());

        // Setup Test Category
        TestCategory category = testCategoryRepository.save(TestCategory.builder()
                .name("Diagnostic Hematology " + suffix)
                .code("DH-" + suffix)
                .build());

        // Setup CBC Test
        cbcTest = testRepository.save(com.swasthai.report_generator.test.entity.Test.builder()
                .name("Complete Blood Profile " + suffix)
                .code("CBC-" + suffix)
                .shortName("CBC")
                .category(category)
                .sampleType(SampleType.WHOLE_BLOOD)
                .specimenContainer("EDTA Vacutainer (Lavender Top)")
                .reportSection("HEMATOLOGY")
                .status(TestStatus.ACTIVE)
                .version(1)
                .build());

        // Setup CBC Parameters
        paramHgb = testParameterRepository.save(TestParameter.builder()
                .test(cbcTest)
                .code("HGB")
                .name("Hemoglobin")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .unit("g/dL")
                .referenceMin(new BigDecimal("13.0"))
                .referenceMax(new BigDecimal("17.0"))
                .criticalLow(new BigDecimal("7.0"))
                .criticalHigh(new BigDecimal("20.0"))
                .displayOrder(1)
                .required(true)
                .status(TestParameterStatus.ACTIVE)
                .build());

        paramRbc = testParameterRepository.save(TestParameter.builder()
                .test(cbcTest)
                .code("RBC")
                .name("Red Blood Cells")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .unit("million/uL")
                .referenceMin(new BigDecimal("4.5"))
                .referenceMax(new BigDecimal("5.5"))
                .displayOrder(2)
                .required(true)
                .status(TestParameterStatus.ACTIVE)
                .build());

        paramHct = testParameterRepository.save(TestParameter.builder()
                .test(cbcTest)
                .code("HCT")
                .name("Hematocrit")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .unit("%")
                .referenceMin(new BigDecimal("40.0"))
                .referenceMax(new BigDecimal("50.0"))
                .displayOrder(3)
                .required(true)
                .status(TestParameterStatus.ACTIVE)
                .build());

        paramMcv = testParameterRepository.save(TestParameter.builder()
                .test(cbcTest)
                .code("MCV")
                .name("Mean Corpuscular Volume")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.MCV)
                .unit("fL")
                .referenceMin(new BigDecimal("80.0"))
                .referenceMax(new BigDecimal("100.0"))
                .displayOrder(4)
                .required(true)
                .status(TestParameterStatus.ACTIVE)
                .build());

        // Assign Test to Org A
        organizationTestRepository.save(OrganizationTest.builder()
                .organization(orgA)
                .test(cbcTest)
                .status(OrganizationTestStatus.ACTIVE)
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

    private ReportResponse createPopulatedReportDraft() {
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String testRefId = report.tests().get(0).refId();

        UpdateReportParametersRequest updateReq = new UpdateReportParametersRequest(
                null,
                List.of(
                        new TestParameterResultInput(paramHgb.getRefId(), "HGB", "15.0"),
                        new TestParameterResultInput(paramRbc.getRefId(), "RBC", "5.0"),
                        new TestParameterResultInput(paramHct.getRefId(), "HCT", "45.0")
                )
        );
        return reportService.updateParameterValues(report.refId(), testRefId, updateReq);
    }

    private ReportResponse createAndFinalizeReport() {
        ReportResponse draft = createPopulatedReportDraft();
        return reportService.finalizeReport(draft.refId());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. Draft report creation allows nullable snapshots while initializing creator metadata")
    void testDraftReportCreation_SnapshotsNullable() {
        authenticateUser(labStaffOrgA);

        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        assertThat(report.refId()).isNotNull();
        assertThat(report.status()).isEqualTo(ReportStatus.DRAFT);
        assertThat(report.createdByEmail()).isEqualTo(labStaffOrgA.getEmail());

        Report entity = reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull(report.refId(), orgA.getId()).orElseThrow();
        assertThat(entity.getCreatedByName()).isEqualTo(labStaffOrgA.getName());
        assertThat(entity.getCreatedByEmail()).isEqualTo(labStaffOrgA.getEmail());
        assertThat(entity.getPatientName()).isNull();
        assertThat(entity.getFinalizedByName()).isNull();
    }

    @Test
    @DisplayName("2. Snapshot is completely and authoritatively populated during report finalization")
    void testFinalizeReport_PopulatesCompleteSnapshots() {
        authenticateUser(labStaffOrgA);

        ReportResponse finalized = createAndFinalizeReport();
        assertThat(finalized.status()).isEqualTo(ReportStatus.FINALIZED);
        assertThat(finalized.finalizedByEmail()).isEqualTo(labStaffOrgA.getEmail());

        Report entity = reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull(finalized.refId(), orgA.getId()).orElseThrow();
        // Patient snapshot
        assertThat(entity.getPatientName()).isEqualTo("Alexander Fleming");
        assertThat(entity.getPatientSalutation()).isEqualTo("MR");
        assertThat(entity.getPatientCode()).isEqualTo(patientOrgA.getPatientCode());
        assertThat(entity.getPatientGender()).isEqualTo("MALE");
        assertThat(entity.getPatientDateOfBirthKnown()).isTrue();
        assertThat(entity.getPatientDateOfBirth()).isEqualTo(LocalDate.of(1985, 6, 15));
        assertThat(entity.getPatientAgeAtReportingValue()).isNotNull().isGreaterThan(0);
        assertThat(entity.getPatientAgeAtReportingUnit()).isEqualTo("YEARS");
        assertThat(entity.getPatientPhone()).isEqualTo("+91-9876543210");
        assertThat(entity.getPatientEmail()).isEqualTo("fleming@alpha.com");
        assertThat(entity.getPatientAddress()).isEqualTo("123 Baker Street, London");
        assertThat(entity.getPatientWeightKg()).isEqualByComparingTo(new BigDecimal("72.500"));

        // Organization snapshot
        assertThat(entity.getOrganizationName()).isEqualTo(orgA.getName());
        assertThat(entity.getOrganizationCode()).isEqualTo(orgA.getCode());

        // User snapshots
        assertThat(entity.getCreatedByName()).isEqualTo(labStaffOrgA.getName());
        assertThat(entity.getCreatedByEmail()).isEqualTo(labStaffOrgA.getEmail());
        assertThat(entity.getFinalizedByName()).isEqualTo(labStaffOrgA.getName());
        assertThat(entity.getFinalizedByEmail()).isEqualTo(labStaffOrgA.getEmail());

        // Test metadata snapshot
        assertThat(entity.getTests()).hasSize(1);
        ReportTestResult rtr = entity.getTests().get(0);
        assertThat(rtr.getTestCode()).isEqualTo(cbcTest.getCode());
        assertThat(rtr.getTestName()).isEqualTo(cbcTest.getName());
        assertThat(rtr.getTestShortName()).isEqualTo("CBC");
        assertThat(rtr.getSampleType()).isEqualTo("WHOLE_BLOOD");
        assertThat(rtr.getSpecimenContainer()).isEqualTo("EDTA Vacutainer (Lavender Top)");
        assertThat(rtr.getReportSection()).isEqualTo("HEMATOLOGY");
    }

    @Test
    @DisplayName("3. Finalized report retains patient snapshot even when Patient entity is later modified")
    void testFinalizedReport_RetainsPatientSnapshot_WhenPatientModified() {
        authenticateUser(labStaffOrgA);

        ReportResponse finalized = createAndFinalizeReport();
        String reportRefId = finalized.refId();

        // Mutate the Patient master entity
        patientOrgA.setName("Altered Patient Identity");
        patientOrgA.setPhone("+91-0000000000");
        patientOrgA.setSalutation(Salutation.DR);
        patientOrgA.setAddress("Altered Address");
        patientRepository.saveAndFlush(patientOrgA);

        // Fetch PDF snapshot data
        ReportPdfData pdfData = reportService.getReportPdfData(reportRefId);
        assertThat(pdfData.patient().name()).isEqualTo("Alexander Fleming");
        assertThat(pdfData.patient().phone()).isEqualTo("+91-9876543210");
        assertThat(pdfData.patient().salutation()).isEqualTo("MR");
        assertThat(pdfData.patient().address()).isEqualTo("123 Baker Street, London");
    }

    @Test
    @DisplayName("4. Finalized report retains organization snapshot even when Organization entity is modified")
    void testFinalizedReport_RetainsOrganizationSnapshot_WhenOrganizationModified() {
        authenticateUser(labStaffOrgA);

        ReportResponse finalized = createAndFinalizeReport();
        String reportRefId = finalized.refId();
        String originalOrgName = orgA.getName();
        String originalOrgCode = orgA.getCode();

        // Mutate Organization master entity
        orgA.setName("Completely New Lab Brand Name");
        orgA.setCode("NEWBRAND");
        organizationRepository.saveAndFlush(orgA);

        ReportResponse report = reportService.getReport(reportRefId);
        assertThat(report.organizationName()).isEqualTo(originalOrgName);

        ReportPdfData pdfData = reportService.getReportPdfData(reportRefId);
        assertThat(pdfData.organization().name()).isEqualTo(originalOrgName);
    }

    @Test
    @DisplayName("5. Finalized report retains test metadata snapshot even when Test entity is modified")
    void testFinalizedReport_RetainsTestSnapshot_WhenTestModified() {
        authenticateUser(labStaffOrgA);

        ReportResponse finalized = createAndFinalizeReport();
        String reportRefId = finalized.refId();
        String originalTestName = cbcTest.getName();
        String originalTestCode = cbcTest.getCode();

        // Mutate Test master entity
        cbcTest.setName("Modified CBC Test Name");
        cbcTest.setCode("MODIFIED-CBC");
        cbcTest.setShortName("MOD-CBC");
        testRepository.saveAndFlush(cbcTest);

        ReportResponse report = reportService.getReport(reportRefId);
        assertThat(report.tests().get(0).testName()).isEqualTo(originalTestName);
        assertThat(report.tests().get(0).testCode()).isEqualTo(originalTestCode);

        ReportPdfData pdfData = reportService.getReportPdfData(reportRefId);
        assertThat(pdfData.tests().get(0).testName()).isEqualTo(originalTestName);
        assertThat(pdfData.tests().get(0).testCode()).isEqualTo(originalTestCode);
        assertThat(pdfData.tests().get(0).testShortName()).isEqualTo("CBC");
    }

    @Test
    @DisplayName("6. Finalized report retains parameter metadata snapshot even when TestParameter entity is modified")
    void testFinalizedReport_RetainsParameterMetadata_WhenTestParameterModified() {
        authenticateUser(labStaffOrgA);

        ReportResponse finalized = createAndFinalizeReport();
        String reportRefId = finalized.refId();

        // Mutate parameter metadata on TestParameter entity
        paramHgb.setName("Altered Hemoglobin Title");
        paramHgb.setUnit("mmol/L");
        testParameterRepository.saveAndFlush(paramHgb);

        ReportResponse report = reportService.getReport(reportRefId);
        assertThat(report.tests().get(0).parameters().get(0).parameterName()).isEqualTo("Hemoglobin");
        assertThat(report.tests().get(0).parameters().get(0).unit()).isEqualTo("g/dL");

        ReportPdfData pdfData = reportService.getReportPdfData(reportRefId);
        assertThat(pdfData.tests().get(0).parameters().get(0).parameterName()).isEqualTo("Hemoglobin");
        assertThat(pdfData.tests().get(0).parameters().get(0).unit()).isEqualTo("g/dL");
    }

    @Test
    @DisplayName("7. Stored ResultFlag is preserved authoritatively in database")
    void testExistingResultFlagPreserved() {
        authenticateUser(labStaffOrgA);

        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String testRefId = report.tests().get(0).refId();

        // Enter HGB value 25.0 (criticalHigh is 20.0 -> ResultFlag.CRITICAL_HIGH)
        UpdateReportParametersRequest updateReq = new UpdateReportParametersRequest(
                null,
                List.of(
                        new TestParameterResultInput(paramHgb.getRefId(), "HGB", "25.0"),
                        new TestParameterResultInput(paramRbc.getRefId(), "RBC", "5.0"),
                        new TestParameterResultInput(paramHct.getRefId(), "HCT", "45.0")
                )
        );
        report = reportService.updateParameterValues(report.refId(), testRefId, updateReq);
        ReportResponse finalized = reportService.finalizeReport(report.refId());

        assertThat(finalized.tests().get(0).parameters().get(0).flag()).isEqualTo(ResultFlag.CRITICAL_HIGH);
    }

    @Test
    @DisplayName("8. PDF generation layer reads and renders stored flags without recalculation")
    void testPdfLayerDoesNotRecalculateFlags() {
        authenticateUser(labStaffOrgA);

        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String testRefId = report.tests().get(0).refId();

        // Enter HGB value 22.0 (criticalHigh is 20.0 -> ResultFlag.CRITICAL_HIGH)
        UpdateReportParametersRequest updateReq = new UpdateReportParametersRequest(
                null,
                List.of(
                        new TestParameterResultInput(paramHgb.getRefId(), "HGB", "22.0"),
                        new TestParameterResultInput(paramRbc.getRefId(), "RBC", "5.0"),
                        new TestParameterResultInput(paramHct.getRefId(), "HCT", "45.0")
                )
        );
        report = reportService.updateParameterValues(report.refId(), testRefId, updateReq);
        ReportResponse finalized = reportService.finalizeReport(report.refId());

        // Verify PDF DTO snapshot preserves authoritative flag
        ReportPdfData pdfData = reportService.getReportPdfData(finalized.refId());
        assertThat(pdfData.tests().get(0).parameters().get(0).flag()).isEqualTo(ResultFlag.CRITICAL_HIGH);

        // Verify binary PDF rendering
        byte[] pdfBytes = reportService.generateReportPdf(finalized.refId());
        assertThat(pdfBytes).isNotNull().isNotEmpty();
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5), StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");

        try (PdfReader reader = new PdfReader(pdfBytes)) {
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String pageText = extractor.getTextFromPage(1);
            assertThat(pageText).contains(orgA.getName());
            assertThat(pageText).doesNotContain(orgA.getCode());
            assertThat(pageText).doesNotContain("SwasthAI Report Generator");
            assertThat(pageText).doesNotContain("verified by SwasthAI");
            assertThat(pageText).contains("This report is electronically generated and verified.");
            assertThat(pageText).contains(patientOrgA.getName());
            assertThat(pageText).contains("CRITICAL HIGH");
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("9. Cross-tenant PDF access is strictly blocked")
    void testCrossTenantPdfAccessBlocked() {
        authenticateUser(labStaffOrgA);
        ReportResponse reportA = createAndFinalizeReport();

        // Switch security context to Org B staff
        authenticateUser(labStaffOrgB);

        assertThatThrownBy(() -> reportService.getReportPdfData(reportA.refId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report not found");

        assertThatThrownBy(() -> reportService.generateReportPdf(reportA.refId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report not found");
    }

    @Test
    @DisplayName("10. Expired organization license blocks report finalization")
    void testExpiredLicense_CannotFinalizeDraft() {
        authenticateUser(labStaffOrgA);

        ReportResponse draft = createPopulatedReportDraft();

        // Expire Org A's license
        licenseOrgA.setStartedAt(Instant.now().minusSeconds(86400 * 2));
        licenseOrgA.setExpiresAt(Instant.now().minusSeconds(86400));
        licenseOrgA.setStatus(com.swasthai.report_generator.license.entity.LicenseStatus.EXPIRED);
        licenseRepository.saveAndFlush(licenseOrgA);

        assertThatThrownBy(() -> reportService.finalizeReport(draft.refId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("license has expired");
    }

    @Test
    @DisplayName("11. Existing finalized report remains readable and downloadable after license expires")
    void testExpiredLicense_ExistingFinalizedReportRemainsReadableAndDownloadable() {
        authenticateUser(labStaffOrgA);

        ReportResponse finalized = createAndFinalizeReport();
        String reportRefId = finalized.refId();

        // Expire Org A's license after report is finalized
        licenseOrgA.setStartedAt(Instant.now().minusSeconds(86400 * 2));
        licenseOrgA.setExpiresAt(Instant.now().minusSeconds(86400));
        licenseOrgA.setStatus(com.swasthai.report_generator.license.entity.LicenseStatus.EXPIRED);
        licenseRepository.saveAndFlush(licenseOrgA);

        // Verification: finalized report can still be read and exported
        ReportResponse fetchedReport = reportService.getReport(reportRefId);
        assertThat(fetchedReport).isNotNull();
        assertThat(fetchedReport.status()).isEqualTo(ReportStatus.FINALIZED);

        ReportPdfData pdfData = reportService.getReportPdfData(reportRefId);
        assertThat(pdfData).isNotNull();
        assertThat(pdfData.status()).isEqualTo(ReportStatus.FINALIZED);

        byte[] pdfBytes = reportService.generateReportPdf(reportRefId);
        assertThat(pdfBytes).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("12. Optimistic locking still works when lockVersion is stale")
    void testOptimisticLocking_StaleVersionRejected() {
        authenticateUser(labStaffOrgA);

        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String testRefId = report.tests().get(0).refId();

        // Provide stale lock version (99L instead of current version)
        UpdateReportParametersRequest staleReq = new UpdateReportParametersRequest(
                99L,
                List.of(new TestParameterResultInput(paramHgb.getRefId(), "HGB", "14.0"))
        );

        final String targetReportRefId = report.refId();
        assertThatThrownBy(() -> reportService.updateParameterValues(targetReportRefId, testRefId, staleReq))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                .hasMessageContaining("Report has been modified by another transaction");
    }

    @Test
    @DisplayName("13. Flyway migrations V1 through V10 remain untouched and V11 exists")
    void testFlywayMigrations_IntegrityPreserved() {
        File migrationDir = new File("src/main/resources/db/migration");
        assertThat(migrationDir).isDirectory();

        File[] files = migrationDir.listFiles((dir, name) -> name.endsWith(".sql"));
        assertThat(files).isNotNull();

        List<String> filenames = List.of(files).stream().map(File::getName).toList();

        // Ensure V1-V6 and V8-V10 are present
        assertThat(filenames).contains(
                "V1__init_security_and_constraints.sql",
                "V2__add_test_parameter_calculation_columns.sql",
                "V3__create_patient_test_results_and_test_parameter_results.sql",
                "V4__create_reports_and_report_test_results.sql",
                "V5__add_report_soft_delete.sql",
                "V6__enhance_patients.sql",
                "V8__create_plans_and_licenses.sql",
                "V9__seed_initial_plans.sql",
                "V10__create_security_audit_logs_and_rate_limits.sql",
                "V11__add_report_historical_snapshots.sql"
        );

        // Ensure V7 was NOT created as explicitly instructed
        assertThat(filenames.stream().noneMatch(name -> name.startsWith("V7__"))).isTrue();
    }

    @Test
    @DisplayName("14. Finalized report retains user snapshot even when User entity is modified")
    void testFinalizedReport_RetainsUserSnapshot_WhenUserModified() {
        authenticateUser(labStaffOrgA);

        ReportResponse finalized = createAndFinalizeReport();
        String reportRefId = finalized.refId();
        String originalStaffName = labStaffOrgA.getName();
        String originalStaffEmail = labStaffOrgA.getEmail();

        // Mutate the User entity
        labStaffOrgA.setName("Dr. Altered Staff Name");
        labStaffOrgA.setEmail("altered-email@alpha.com");
        userRepository.saveAndFlush(labStaffOrgA);

        // Fetch report, entity & PDF data
        ReportResponse report = reportService.getReport(reportRefId);
        assertThat(report.createdByEmail()).isEqualTo(originalStaffEmail);
        assertThat(report.finalizedByEmail()).isEqualTo(originalStaffEmail);

        Report entity = reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull(reportRefId, orgA.getId()).orElseThrow();
        assertThat(entity.getCreatedByName()).isEqualTo(originalStaffName);
        assertThat(entity.getCreatedByEmail()).isEqualTo(originalStaffEmail);
        assertThat(entity.getFinalizedByName()).isEqualTo(originalStaffName);
        assertThat(entity.getFinalizedByEmail()).isEqualTo(originalStaffEmail);

        ReportPdfData pdfData = reportService.getReportPdfData(reportRefId);
        assertThat(pdfData.createdBy().name()).isEqualTo(originalStaffName);
        assertThat(pdfData.createdBy().email()).isEqualTo(originalStaffEmail);
        assertThat(pdfData.finalizedBy().name()).isEqualTo(originalStaffName);
        assertThat(pdfData.finalizedBy().email()).isEqualTo(originalStaffEmail);
    }

    @Test
    @DisplayName("15. Soft-deleted report cannot be exported via PDF")
    void testSoftDeletedReport_CannotBeExportedAsPdf() {
        authenticateUser(labStaffOrgA);

        ReportResponse finalized = createAndFinalizeReport();
        String reportRefId = finalized.refId();

        // Soft delete the report directly
        Report report = reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull(reportRefId, orgA.getId()).orElseThrow();
        report.setDeletedAt(Instant.now());
        report.setDeletedBy(labStaffOrgA);
        reportRepository.saveAndFlush(report);

        // Attempting to export PDF or get PDF data must fail with ResourceNotFoundException
        assertThatThrownBy(() -> reportService.getReportPdfData(reportRefId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report not found");

        assertThatThrownBy(() -> reportService.generateReportPdf(reportRefId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report not found");
    }

    @Test
    @DisplayName("16. Newborn patient age snapshot calculates 0 DAYS")
    void testNewbornPatient_CalculatesZeroDays() {
        authenticateUser(labStaffOrgA);

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Patient newborn = patientRepository.save(Patient.builder()
                .organization(orgA)
                .salutation(Salutation.BABY)
                .name("Baby Doe")
                .patientCode("NB-" + suffix)
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.now())
                .gender(Gender.FEMALE)
                .build());

        ReportResponse report = reportService.createReport(new CreateReportRequest(newborn.getRefId()));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String testRefId = report.tests().get(0).refId();

        UpdateReportParametersRequest updateReq = new UpdateReportParametersRequest(
                null,
                List.of(
                        new TestParameterResultInput(paramHgb.getRefId(), "HGB", "14.0"),
                        new TestParameterResultInput(paramRbc.getRefId(), "RBC", "4.8"),
                        new TestParameterResultInput(paramHct.getRefId(), "HCT", "42.0")
                )
        );
        report = reportService.updateParameterValues(report.refId(), testRefId, updateReq);
        ReportResponse finalized = reportService.finalizeReport(report.refId());

        Report entity = reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull(finalized.refId(), orgA.getId()).orElseThrow();
        assertThat(entity.getPatientAgeAtReportingValue()).isEqualTo(0);
        assertThat(entity.getPatientAgeAtReportingUnit()).isEqualTo("DAYS");

        ReportPdfData pdfData = reportService.getReportPdfData(finalized.refId());
        assertThat(pdfData.patient().ageValue()).isEqualTo(0);
        assertThat(pdfData.patient().ageUnit()).isEqualTo("DAYS");
    }

    @Test
    @DisplayName("17. Patient with unknown DOB snapshot preserves manual ageValue and ageUnit")
    void testUnknownDobPatient_PreservesManualAgeValueAndUnit() {
        authenticateUser(labStaffOrgA);

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Patient patientUnknownDob = patientRepository.save(Patient.builder()
                .organization(orgA)
                .salutation(Salutation.MR)
                .name("John Unknown")
                .patientCode("UK-" + suffix)
                .dateOfBirthKnown(false)
                .ageValue(45)
                .ageUnit(com.swasthai.report_generator.patient.entity.AgeUnit.YEARS)
                .gender(Gender.MALE)
                .build());

        ReportResponse report = reportService.createReport(new CreateReportRequest(patientUnknownDob.getRefId()));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String testRefId = report.tests().get(0).refId();

        UpdateReportParametersRequest updateReq = new UpdateReportParametersRequest(
                null,
                List.of(
                        new TestParameterResultInput(paramHgb.getRefId(), "HGB", "13.5"),
                        new TestParameterResultInput(paramRbc.getRefId(), "RBC", "4.5"),
                        new TestParameterResultInput(paramHct.getRefId(), "HCT", "40.0")
                )
        );
        report = reportService.updateParameterValues(report.refId(), testRefId, updateReq);
        ReportResponse finalized = reportService.finalizeReport(report.refId());

        Report entity = reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull(finalized.refId(), orgA.getId()).orElseThrow();
        assertThat(entity.getPatientAgeAtReportingValue()).isEqualTo(45);
        assertThat(entity.getPatientAgeAtReportingUnit()).isEqualTo("YEARS");
        assertThat(entity.getPatientDateOfBirthKnown()).isFalse();

        ReportPdfData pdfData = reportService.getReportPdfData(finalized.refId());
        assertThat(pdfData.patient().ageValue()).isEqualTo(45);
        assertThat(pdfData.patient().ageUnit()).isEqualTo("YEARS");
    }

    @Test
    @DisplayName("18. PDF generation fails safely when verification URL is missing or blank")
    void testPdfGeneration_FailsSafely_WhenVerificationUrlMissing() {
        authenticateUser(labStaffOrgA);
        ReportResponse finalized = createAndFinalizeReport();
        ReportPdfData validData = reportService.getReportPdfData(finalized.refId());

        ReportPdfData invalidData = ReportPdfData.builder()
                .reportRefId(validData.reportRefId())
                .status(validData.status())
                .reportVersion(validData.reportVersion())
                .createdAt(validData.createdAt())
                .finalizedAt(validData.finalizedAt())
                .organization(validData.organization())
                .patient(validData.patient())
                .createdBy(validData.createdBy())
                .finalizedBy(validData.finalizedBy())
                .tests(validData.tests())
                .verificationUrl("   ")
                .build();

        assertThatThrownBy(() -> openPdfRenderer.render(invalidData))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing verification URL");
    }

    @Test
    @DisplayName("19. QR code image is present in footer on EVERY page of multi-page report")
    void testMultiPageReport_ContainsQrCodeOnEveryPage() {
        authenticateUser(labStaffOrgA);

        // Create a test with many parameters to cause a dynamic page break
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        com.swasthai.report_generator.test.entity.Test largeTest = testRepository.save(
                com.swasthai.report_generator.test.entity.Test.builder()
                        .name("Comprehensive Metabolic Panel " + suffix)
                        .code("CMP-" + suffix)
                        .shortName("CMP")
                        .category(paramHgb.getTest().getCategory())
                        .sampleType(SampleType.SERUM)
                        .specimenContainer("SST Vacutainer (Gold Top)")
                        .reportSection("BIOCHEMISTRY")
                        .status(TestStatus.ACTIVE)
                        .version(1)
                        .build()
        );

        organizationTestRepository.save(
                OrganizationTest.builder()
                        .organization(orgA)
                        .test(largeTest)
                        .status(OrganizationTestStatus.ACTIVE)
                        .build()
        );

        java.util.List<TestParameterResultInput> paramInputs = new java.util.ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            TestParameter p = testParameterRepository.save(TestParameter.builder()
                    .test(largeTest)
                    .code("P_" + i)
                    .name("Metabolic Parameter " + i)
                    .dataType(TestParameterDataType.DECIMAL)
                    .inputType(ParameterInputType.MANUAL)
                    .unit("mg/dL")
                    .referenceMin(new BigDecimal("10.0"))
                    .referenceMax(new BigDecimal("100.0"))
                    .displayOrder(i)
                    .required(true)
                    .status(TestParameterStatus.ACTIVE)
                    .build());
            paramInputs.add(new TestParameterResultInput(p.getRefId(), "P_" + i, "50.0"));
        }

        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(largeTest.getRefId(), null));

        String cbcRefId = report.tests().get(0).refId();
        String cmpRefId = report.tests().get(1).refId();

        reportService.updateParameterValues(report.refId(), cbcRefId, new UpdateReportParametersRequest(
                null,
                List.of(
                        new TestParameterResultInput(paramHgb.getRefId(), "HGB", "14.0"),
                        new TestParameterResultInput(paramRbc.getRefId(), "RBC", "4.8"),
                        new TestParameterResultInput(paramHct.getRefId(), "HCT", "42.0")
                )
        ));

        reportService.updateParameterValues(report.refId(), cmpRefId, new UpdateReportParametersRequest(
                null,
                paramInputs
        ));

        ReportResponse finalized = reportService.finalizeReport(report.refId());
        byte[] pdfBytes = reportService.generateReportPdf(finalized.refId());
        assertThat(pdfBytes).isNotNull().isNotEmpty();

        try (PdfReader reader = new PdfReader(pdfBytes)) {
            int pageCount = reader.getNumberOfPages();
            assertThat(pageCount).as("Must produce a multi-page document").isGreaterThanOrEqualTo(2);

            for (int p = 1; p <= pageCount; p++) {
                PdfDictionary pageDict = reader.getPageN(p);
                PdfDictionary resources = pageDict.getAsDict(PdfName.RESOURCES);
                assertThat(resources).as("Page " + p + " must have resources").isNotNull();
                PdfDictionary xobjects = resources.getAsDict(PdfName.XOBJECT);
                assertThat(xobjects).as("Page " + p + " must have XObjects (QR code)").isNotNull();
                assertThat(xobjects.getKeys()).as("Page " + p + " must contain QR code image").isNotEmpty();

                PdfTextExtractor extractor = new PdfTextExtractor(reader);
                String pageText = extractor.getTextFromPage(p);
                assertThat(pageText).as("Page " + p + " must contain Report ID").contains("Report ID: " + finalized.refId());
                assertThat(pageText).as("Page " + p + " must contain Page number").contains("Page " + p);
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}

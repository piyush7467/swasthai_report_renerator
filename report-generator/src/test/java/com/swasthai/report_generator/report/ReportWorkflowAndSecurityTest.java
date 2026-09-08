package com.swasthai.report_generator.report;

import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.dto.request.AddReportTestRequest;
import com.swasthai.report_generator.report.dto.request.CreateReportRequest;
import com.swasthai.report_generator.report.dto.request.ReorderReportTestsRequest;
import com.swasthai.report_generator.report.dto.request.TestOrderItemInput;
import com.swasthai.report_generator.report.dto.request.UpdateReportParametersRequest;
import com.swasthai.report_generator.report.dto.response.ReportParameterItemResponse;
import com.swasthai.report_generator.report.dto.response.ReportResponse;
import com.swasthai.report_generator.report.dto.response.ReportTestItemResponse;
import com.swasthai.report_generator.report.entity.ReportStatus;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReportWorkflowAndSecurityTest {

    @Autowired
    private ReportService reportService;

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

    private Organization orgA;
    private Organization orgB;
    private User labStaffOrgA;
    private User labStaffOrgB;
    private Patient patientOrgA;
    private Patient patientOrgB;

    private com.swasthai.report_generator.test.entity.Test cbcTest;
    private com.swasthai.report_generator.test.entity.Test lftTest;
    private com.swasthai.report_generator.test.entity.Test kftTest;
    private com.swasthai.report_generator.test.entity.Test unassignedTest;

    private TestParameter paramHgb;
    private TestParameter paramRbc;
    private TestParameter paramHct;
    private TestParameter paramMcv;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Setup Organizations
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

        // Setup Users
        labStaffOrgA = userRepository.save(User.builder()
                .email("staff-a-" + suffix + "@alpha.com")
                .name("Staff Alpha")
                .passwordHash("hashed")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build());

        labStaffOrgB = userRepository.save(User.builder()
                .email("staff-b-" + suffix + "@beta.com")
                .name("Staff Beta")
                .passwordHash("hashed")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgB)
                .build());

        // Setup Patients
        patientOrgA = patientRepository.save(Patient.builder()
                .organization(orgA)
                .name("John Doe")
                .patientCode("PA-" + suffix)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .build());

        patientOrgB = patientRepository.save(Patient.builder()
                .organization(orgB)
                .name("Jane Smith")
                .patientCode("PB-" + suffix)
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .gender(Gender.FEMALE)
                .build());

        // Setup Test Category
        TestCategory category = testCategoryRepository.save(TestCategory.builder()
                .name("Hematology " + suffix)
                .code("HEM-" + suffix)
                .build());

        // Setup Tests
        cbcTest = testRepository.save(com.swasthai.report_generator.test.entity.Test.builder()
                .name("Complete Blood Count " + suffix)
                .code("CBC-" + suffix)
                .category(category)
                .sampleType(SampleType.WHOLE_BLOOD)
                .status(TestStatus.ACTIVE)
                .version(1)
                .build());

        lftTest = testRepository.save(com.swasthai.report_generator.test.entity.Test.builder()
                .name("Liver Function Test " + suffix)
                .code("LFT-" + suffix)
                .category(category)
                .sampleType(SampleType.SERUM)
                .status(TestStatus.ACTIVE)
                .version(1)
                .build());

        kftTest = testRepository.save(com.swasthai.report_generator.test.entity.Test.builder()
                .name("Kidney Function Test " + suffix)
                .code("KFT-" + suffix)
                .category(category)
                .sampleType(SampleType.SERUM)
                .status(TestStatus.ACTIVE)
                .version(1)
                .build());

        unassignedTest = testRepository.save(com.swasthai.report_generator.test.entity.Test.builder()
                .name("Thyroid Profile " + suffix)
                .code("THY-" + suffix)
                .category(category)
                .sampleType(SampleType.SERUM)
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

        // Assign CBC, LFT, KFT to Org A
        organizationTestRepository.save(OrganizationTest.builder()
                .organization(orgA)
                .test(cbcTest)
                .status(OrganizationTestStatus.ACTIVE)
                .build());

        organizationTestRepository.save(OrganizationTest.builder()
                .organization(orgA)
                .test(lftTest)
                .status(OrganizationTestStatus.ACTIVE)
                .build());

        organizationTestRepository.save(OrganizationTest.builder()
                .organization(orgA)
                .test(kftTest)
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Complete Multi-Test Workflow: Draft -> Add CBC -> Add LFT -> Add KFT -> Remove LFT -> Calculate -> Finalize")
    void testCompleteMultiTestReportWorkflow() {
        authenticateUser(labStaffOrgA);

        // 1. Create Report Draft
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        assertThat(report.refId()).isNotNull();
        assertThat(report.status()).isEqualTo(ReportStatus.DRAFT);
        assertThat(report.tests()).isEmpty();

        // 2. Add CBC
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        assertThat(report.tests()).hasSize(1);
        ReportTestItemResponse rtrCbc = report.tests().get(0);
        assertThat(rtrCbc.testCode()).isEqualTo(cbcTest.getCode());
        assertThat(rtrCbc.parameters()).hasSize(4);

        // 3. Add LFT
        report = reportService.addTest(report.refId(), new AddReportTestRequest(lftTest.getRefId(), null));
        assertThat(report.tests()).hasSize(2);
        ReportTestItemResponse rtrLft = report.tests().get(1);
        assertThat(rtrLft.testCode()).isEqualTo(lftTest.getCode());

        // 4. Add KFT
        report = reportService.addTest(report.refId(), new AddReportTestRequest(kftTest.getRefId(), null));
        assertThat(report.tests()).hasSize(3);

        // 5. Remove LFT -> CBC and KFT remain untouched
        report = reportService.removeTest(report.refId(), rtrLft.refId());
        assertThat(report.tests()).hasSize(2);
        assertThat(report.tests().get(0).testCode()).isEqualTo(cbcTest.getCode());
        assertThat(report.tests().get(0).displayOrder()).isEqualTo(1);
        assertThat(report.tests().get(1).testCode()).isEqualTo(kftTest.getCode());
        assertThat(report.tests().get(1).displayOrder()).isEqualTo(2);

        // 6. Enter parameter values for CBC: HGB=14.0, RBC=5.0, HCT=45.0
        // Expected calculated MCV = 45.0 * 10 / 5.0 = 90.0000
        UpdateReportParametersRequest paramRequest = new UpdateReportParametersRequest(
                null,
                List.of(
                        new TestParameterResultInput(paramHgb.getRefId(), "HGB", "14.0"),
                        new TestParameterResultInput(paramRbc.getRefId(), "RBC", "5.0"),
                        new TestParameterResultInput(paramHct.getRefId(), "HCT", "45.0")
                )
        );
        report = reportService.updateParameterValues(report.refId(), rtrCbc.refId(), paramRequest);

        ReportTestItemResponse updatedCbc = report.tests().stream()
                .filter(t -> t.testCode().equals(cbcTest.getCode()))
                .findFirst()
                .orElseThrow();

        ReportParameterItemResponse mcvResult = updatedCbc.parameters().stream()
                .filter(p -> p.parameterCode().equals("MCV"))
                .findFirst()
                .orElseThrow();

        assertThat(mcvResult.value()).isEqualTo("90");
        assertThat(mcvResult.flag()).isEqualTo(ResultFlag.NORMAL);

        // 7. Reorder tests: Move KFT to 1, CBC to 2
        String kftRefId = report.tests().get(1).refId();
        String cbcRtrRefId = updatedCbc.refId();

        report = reportService.reorderTests(report.refId(), new ReorderReportTestsRequest(
                null,
                List.of(
                        new TestOrderItemInput(kftRefId, 1),
                        new TestOrderItemInput(cbcRtrRefId, 2)
                )
        ));

        assertThat(report.tests().get(0).refId()).isEqualTo(kftRefId);
        assertThat(report.tests().get(0).displayOrder()).isEqualTo(1);
        assertThat(report.tests().get(1).refId()).isEqualTo(cbcRtrRefId);
        assertThat(report.tests().get(1).displayOrder()).isEqualTo(2);

        // 8. Refresh/Get report: Exact draft state restored
        ReportResponse reloaded = reportService.getReport(report.refId());
        assertThat(reloaded.refId()).isEqualTo(report.refId());
        assertThat(reloaded.tests()).hasSize(2);

        // 9. Finalize Report
        ReportResponse finalized = reportService.finalizeReport(report.refId());
        assertThat(finalized.status()).isEqualTo(ReportStatus.FINALIZED);
        assertThat(finalized.finalizedByEmail()).isEqualTo(labStaffOrgA.getEmail());
        assertThat(finalized.finalizedAt()).isNotNull();

        // 10. Immutability: Mutating a finalized report must be rejected
        final String finalizedRefId = report.refId();
        assertThatThrownBy(() -> reportService.addTest(finalizedRefId, new AddReportTestRequest(lftTest.getRefId(), null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Finalized report cannot be modified");

        assertThatThrownBy(() -> reportService.removeTest(finalizedRefId, kftRefId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Finalized report cannot be modified");

        assertThatThrownBy(() -> reportService.updateParameterValues(finalizedRefId, cbcRtrRefId, paramRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Finalized report cannot be modified");
    }

    @Test
    @DisplayName("Tenant Isolation: Org B user cannot access or modify Org A report")
    void testTenantIsolation_CrossTenantAccessDenied() {
        authenticateUser(labStaffOrgA);
        ReportResponse reportOrgA = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));

        // Switch to Org B user
        authenticateUser(labStaffOrgB);

        assertThatThrownBy(() -> reportService.getReport(reportOrgA.refId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> reportService.addTest(reportOrgA.refId(), new AddReportTestRequest(cbcTest.getRefId(), null)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> reportService.finalizeReport(reportOrgA.refId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Patient Validation: Cannot create report with patient from another organization")
    void testPatientOwnershipValidation_CrossTenantPatientRejected() {
        authenticateUser(labStaffOrgA);

        assertThatThrownBy(() -> reportService.createReport(new CreateReportRequest(patientOrgB.getRefId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient not found or does not belong to your organization");
    }

    @Test
    @DisplayName("Test Assignment Validation: Cannot add test not assigned to organization")
    void testTestAssignmentValidation_UnassignedTestRejected() {
        authenticateUser(labStaffOrgA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));

        assertThatThrownBy(() -> reportService.addTest(report.refId(), new AddReportTestRequest(unassignedTest.getRefId(), null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Organization does not have active access to test");
    }

    @Test
    @DisplayName("Duplicate Test Prevention: Adding same test twice in same report is rejected")
    void testDuplicateTestRejected() {
        authenticateUser(labStaffOrgA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));

        assertThatThrownBy(() -> reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Test already added to this report");
    }

    @Test
    @DisplayName("Calculated Parameter Protection: Client attempting to manually override MCV is rejected")
    void testCalculatedParameterClientOverrideRejected() {
        authenticateUser(labStaffOrgA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        ReportResponse reportWithCbc = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String rtrCbcRefId = reportWithCbc.tests().get(0).refId();
        final String attackReportRefId = reportWithCbc.refId();

        UpdateReportParametersRequest attackRequest = new UpdateReportParametersRequest(
                null,
                List.of(new TestParameterResultInput(paramMcv.getRefId(), "MCV", "999.0"))
        );

        assertThatThrownBy(() -> reportService.updateParameterValues(attackReportRefId, rtrCbcRefId, attackRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Calculated parameters cannot be manually provided");
    }

    @Test
    @DisplayName("Optimistic Locking: Stale lock version in update request throws conflict exception")
    void testOptimisticLockingStaleVersionRejected() {
        authenticateUser(labStaffOrgA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        ReportResponse reportWithCbc = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String rtrCbcRefId = reportWithCbc.tests().get(0).refId();
        final String staleReportRefId = reportWithCbc.refId();

        // Stale lock version (sending 99 when actual is 1)
        UpdateReportParametersRequest staleRequest = new UpdateReportParametersRequest(
                99L,
                List.of(new TestParameterResultInput(paramHgb.getRefId(), "HGB", "14.5"))
        );

        assertThatThrownBy(() -> reportService.updateParameterValues(staleReportRefId, rtrCbcRefId, staleRequest))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                .hasMessageContaining("Report has been modified by another transaction");
    }
}
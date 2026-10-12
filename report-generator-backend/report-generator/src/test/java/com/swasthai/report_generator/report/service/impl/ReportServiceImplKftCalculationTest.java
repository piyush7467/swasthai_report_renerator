package com.swasthai.report_generator.report.service.impl;

import com.swasthai.report_generator.license.service.LicenseGuard;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationProfileRepository;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.config.ReportRetentionProperties;
import com.swasthai.report_generator.report.dto.request.UpdateReportParametersRequest;
import com.swasthai.report_generator.report.dto.response.ReportResponse;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.entity.ReportTestResult;
import com.swasthai.report_generator.report.pdf.PdfRenderer;
import com.swasthai.report_generator.report.pdf.ReportPdfDataBuilder;
import com.swasthai.report_generator.report.pdf.VerificationQrCodeGenerator;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.repository.ReportShareRepository;
import com.swasthai.report_generator.report.repository.ReportTestResultRepository;
import com.swasthai.report_generator.report.service.ReportDeletionBatchExecutor;
import com.swasthai.report_generator.report.service.ReportPurgeBatchExecutor;
import com.swasthai.report_generator.security.audit.service.AuditLogService;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.CbcDifferentialValidator;
import com.swasthai.report_generator.test.calculation.calculators.*;
import com.swasthai.report_generator.test.calculation.impl.CalculationEngineImpl;
import com.swasthai.report_generator.test.dto.request.TestParameterResultInput;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestParameterResultRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.test.service.OrganizationTestService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplKftCalculationTest {

    @Mock private ReportRepository reportRepository;
    @Mock private ReportTestResultRepository reportTestResultRepository;
    @Mock private TestRepository testRepository;
    @Mock private TestParameterRepository testParameterRepository;
    @Mock private TestParameterResultRepository testParameterResultRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private OrganizationTestService organizationTestService;
    @Mock private ReportRetentionProperties retentionProperties;
    @Mock private ReportPurgeBatchExecutor purgeBatchExecutor;
    @Mock private ReportDeletionBatchExecutor deletionBatchExecutor;
    @Mock private LicenseGuard licenseGuard;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationProfileRepository organizationProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private ReportPdfDataBuilder reportPdfDataBuilder;
    @Mock private PdfRenderer pdfRenderer;
    @Mock private VerificationQrCodeGenerator verificationQrCodeGenerator;
    @Mock private ReportShareRepository reportShareRepository;

    private CalculationEngine calculationEngine;
    private CbcDifferentialValidator cbcDifferentialValidator;
    private ReportServiceImpl reportService;

    private Organization testOrg;
    private User orgAdminUser;
    private Report draftReport;
    private ReportTestResult kftReportTest;

    private TestParameterResult creaResult;
    private TestParameterResult ureaResult;
    private TestParameterResult naResult;
    private TestParameterResult kResult;
    private TestParameterResult clResult;
    private TestParameterResult caResult;
    private TestParameterResult ureaCreatRatioResult;
    private TestParameterResult bunCreatRatioResult;
    private TestParameterResult egfrResult;

    @BeforeEach
    void setUp() {
        calculationEngine = new CalculationEngineImpl(List.of(
                new BUNCreatinineRatioCalculator(),
                new UreaCreatinineRatioCalculator(),
                new EgfrCkdEpi2021Calculator(),
                new AnionGapCalculator(),
                new AnionGapKCalculator()
        ));
        cbcDifferentialValidator = new CbcDifferentialValidator();

        reportService = new ReportServiceImpl(
                reportRepository,
                reportTestResultRepository,
                testRepository,
                testParameterRepository,
                testParameterResultRepository,
                patientRepository,
                organizationTestService,
                calculationEngine,
                cbcDifferentialValidator,
                retentionProperties,
                purgeBatchExecutor,
                deletionBatchExecutor,
                licenseGuard,
                organizationRepository,
                organizationProfileRepository,
                userRepository,
                auditLogService,
                reportPdfDataBuilder,
                pdfRenderer,
                verificationQrCodeGenerator,
                reportShareRepository
        );

        testOrg = Organization.builder()
                .id(UUID.randomUUID())
                .refId("ORG-001")
                .name("Apex Labs")
                .status(OrganizationStatus.ACTIVE)
                .build();

        orgAdminUser = User.builder()
                .id(UUID.randomUUID())
                .refId("USER-001")
                .email("admin@apex.com")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(testOrg)
                .build();

        setAuthenticatedUser(orgAdminUser, "ROLE_ORG_ADMIN");

        com.swasthai.report_generator.test.entity.Test kftEntity =
                com.swasthai.report_generator.test.entity.Test.builder()
                        .id(UUID.randomUUID())
                        .refId("TEST-KFT")
                        .code("KFT")
                        .name("Kidney Function Test")
                        .status(TestStatus.ACTIVE)
                        .build();

        kftReportTest = ReportTestResult.builder()
                .id(UUID.randomUUID())
                .refId("RTR-KFT01")
                .test(kftEntity)
                .testCode("KFT")
                .testName("Kidney Function Test")
                .parameterResults(new ArrayList<>())
                .build();

        // 6 manual parameters
        creaResult = createParamResult(kftReportTest, "CREA", "Serum Creatinine", ParameterInputType.MANUAL, CalculationType.NONE, "mg/dL", 1);
        ureaResult = createParamResult(kftReportTest, "UREA", "Blood Urea", ParameterInputType.MANUAL, CalculationType.NONE, "mg/dL", 2);
        naResult = createParamResult(kftReportTest, "NA", "Sodium", ParameterInputType.MANUAL, CalculationType.NONE, "mmol/L", 3);
        kResult = createParamResult(kftReportTest, "K", "Potassium", ParameterInputType.MANUAL, CalculationType.NONE, "mmol/L", 4);
        clResult = createParamResult(kftReportTest, "CL", "Chloride", ParameterInputType.MANUAL, CalculationType.NONE, "mmol/L", 5);
        caResult = createParamResult(kftReportTest, "CA", "Total Calcium", ParameterInputType.MANUAL, CalculationType.NONE, "mg/dL", 6);

        // optional calculated parameters
        ureaCreatRatioResult = createParamResult(kftReportTest, "UREA_CREATININE_RATIO", "Urea / Creatinine Ratio", ParameterInputType.CALCULATED, CalculationType.UREA_CREATININE_RATIO, "", 7);
        bunCreatRatioResult = createParamResult(kftReportTest, "BUN_CREATININE_RATIO", "BUN / Creatinine Ratio", ParameterInputType.CALCULATED, CalculationType.BUN_CREATININE_RATIO, "", 8);
        egfrResult = createParamResult(kftReportTest, "EGFR", "eGFR (CKD-EPI 2021)", ParameterInputType.CALCULATED, CalculationType.EGFR_CKD_EPI_2021, "mL/min/1.73 m²", 9);

        kftReportTest.getParameterResults().addAll(List.of(
                creaResult, ureaResult, naResult, kResult, clResult, caResult,
                ureaCreatRatioResult, bunCreatRatioResult, egfrResult
        ));

        draftReport = Report.builder()
                .id(UUID.randomUUID())
                .refId("REP-KFT-01")
                .organization(testOrg)
                .status(ReportStatus.DRAFT)
                .reportVersion(1)
                .lockVersion(0L)
                .patientAgeAtReportingValue(40)
                .patientAgeAtReportingUnit("YEARS")
                .patientGender("FEMALE")
                .tests(new ArrayList<>(List.of(kftReportTest)))
                .build();

        kftReportTest.setReport(draftReport);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private TestParameterResult createParamResult(
            ReportTestResult rtr, String code, String name,
            ParameterInputType inputType, CalculationType calcType,
            String unit, int displayOrder
    ) {
        TestParameter param = TestParameter.builder()
                .id(UUID.randomUUID())
                .refId("PARAM-" + code)
                .code(code)
                .name(name)
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(inputType)
                .calculationType(calcType)
                .unit(unit)
                .displayOrder(displayOrder)
                .build();

        return TestParameterResult.builder()
                .id(UUID.randomUUID())
                .refId("TPR-" + code)
                .reportTestResult(rtr)
                .testParameter(param)
                .parameterCode(code)
                .parameterName(name)
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(inputType)
                .calculationType(calcType)
                .unit(unit)
                .displayOrder(displayOrder)
                .build();
    }

    private void setAuthenticatedUser(User user, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                Collections.singleton(new SimpleGrantedAuthority(role))
        );
        auth.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("1. Update manual parameters and execute KFT calculations: Urea/Creatinine ratio, BUN/Creatinine ratio (converted), and eGFR")
    void testKftCalculationsExecution() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-KFT-01", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Submit manual inputs: CREA=0.8, UREA=36.0, NA=140.0, K=4.2, CL=102.0, CA=9.5
        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "CREA", "0.8"),
                        new TestParameterResultInput(null, "UREA", "36.0"),
                        new TestParameterResultInput(null, "NA", "140.0"),
                        new TestParameterResultInput(null, "K", "4.2"),
                        new TestParameterResultInput(null, "CL", "102.0"),
                        new TestParameterResultInput(null, "CA", "9.5")
                )
        );

        ReportResponse response = reportService.updateParameterValues("REP-KFT-01", "RTR-KFT01", request);

        assertThat(response).isNotNull();

        // 1. Verify Urea / Creatinine Ratio: 36.0 / 0.8 = 45.00
        assertThat(ureaCreatRatioResult.getNumericValue()).isEqualByComparingTo("45.00");
        assertThat(ureaCreatRatioResult.getValue()).isEqualTo("45");

        // 2. Verify BUN / Creatinine Ratio with Urea conversion:
        // BUN = 36.0 * 0.467 = 16.8120 => Ratio = 16.8120 / 0.8 = 21.02
        assertThat(bunCreatRatioResult.getNumericValue()).isEqualByComparingTo("21.02");

        // 3. Verify eGFR CKD-EPI 2021: Female, 40yo, Creatinine 0.8 mg/dL => 95.5 mL/min/1.73 m²
        assertThat(egfrResult.getNumericValue()).isEqualByComparingTo("95.5");
        assertThat(egfrResult.getValue()).isEqualTo("95.5");
    }

    @Test
    @DisplayName("2. Client cannot manually override calculated parameters")
    void testCalculatedParameterCannotBeManuallyProvided() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-KFT-01", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));

        // Attempt to pass a manual value for calculated parameter EGFR
        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "CREA", "0.8"),
                        new TestParameterResultInput(null, "EGFR", "100.0")
                )
        );

        assertThatThrownBy(() -> reportService.updateParameterValues("REP-KFT-01", "RTR-KFT01", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Calculated parameters cannot be manually provided: EGFR");
    }

    @Test
    @DisplayName("3. Pediatric age (< 18) for eGFR is handled gracefully without failing report update")
    void testPediatricAgeHandledGracefully() {
        // Set patient age to 14 (pediatric)
        draftReport.setPatientAgeAtReportingValue(14);

        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-KFT-01", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "CREA", "0.8"),
                        new TestParameterResultInput(null, "UREA", "36.0")
                )
        );

        ReportResponse response = reportService.updateParameterValues("REP-KFT-01", "RTR-KFT01", request);

        assertThat(response).isNotNull();
        // eGFR is safely null for pediatric patient
        assertThat(egfrResult.getNumericValue()).isNull();
        assertThat(egfrResult.getValue()).isNull();
        // Other calculations that don't depend on adult age still succeed
        assertThat(ureaCreatRatioResult.getNumericValue()).isEqualByComparingTo("45.00");
    }

    @Test
    @DisplayName("4. Missing age or gender for eGFR is handled gracefully without failing report update")
    void testMissingDemographicsHandledGracefully() {
        // Clear age and gender
        draftReport.setPatientAgeAtReportingValue(null);
        draftReport.setPatientGender(null);

        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-KFT-01", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "CREA", "0.8"),
                        new TestParameterResultInput(null, "UREA", "36.0")
                )
        );

        ReportResponse response = reportService.updateParameterValues("REP-KFT-01", "RTR-KFT01", request);

        assertThat(response).isNotNull();
        // eGFR is safely null due to missing demographics
        assertThat(egfrResult.getNumericValue()).isNull();
        assertThat(egfrResult.getValue()).isNull();
        // Ratio calculation still succeeds
        assertThat(ureaCreatRatioResult.getNumericValue()).isEqualByComparingTo("45.00");
    }
}

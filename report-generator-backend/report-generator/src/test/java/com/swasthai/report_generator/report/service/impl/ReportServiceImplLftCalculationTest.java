package com.swasthai.report_generator.report.service.impl;

import com.swasthai.report_generator.license.service.LicenseGuard;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationProfileRepository;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.config.ReportRetentionProperties;
import com.swasthai.report_generator.report.dto.request.AddReportTestsBulkRequest;
import com.swasthai.report_generator.report.dto.request.UpdateReportParametersRequest;
import com.swasthai.report_generator.report.dto.response.ReportParameterItemResponse;
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
import com.swasthai.report_generator.test.calculation.calculators.AGRatioCalculator;
import com.swasthai.report_generator.test.calculation.calculators.GlobulinCalculator;
import com.swasthai.report_generator.test.calculation.calculators.IndirectBilirubinCalculator;
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
class ReportServiceImplLftCalculationTest {

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
    private ReportTestResult lftReportTest;

    private TestParameterResult tbilResult;
    private TestParameterResult dbilResult;
    private TestParameterResult ibilResult;
    private TestParameterResult astResult;
    private TestParameterResult altResult;
    private TestParameterResult alpResult;
    private TestParameterResult tpResult;
    private TestParameterResult albResult;
    private TestParameterResult globResult;
    private TestParameterResult agRatioResult;

    @BeforeEach
    void setUp() {
        calculationEngine = new CalculationEngineImpl(List.of(
                new IndirectBilirubinCalculator(),
                new GlobulinCalculator(),
                new AGRatioCalculator()
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

        com.swasthai.report_generator.test.entity.Test lftEntity =
                com.swasthai.report_generator.test.entity.Test.builder()
                        .id(UUID.randomUUID())
                        .refId("TEST-LFT")
                        .code("LFT")
                        .name("Liver Function Test")
                        .status(TestStatus.ACTIVE)
                        .build();

        lftReportTest = ReportTestResult.builder()
                .id(UUID.randomUUID())
                .refId("RTR-LFT01")
                .test(lftEntity)
                .testCode("LFT")
                .testName("Liver Function Test")
                .parameterResults(new ArrayList<>())
                .build();

        // Build all 10 draft LFT parameters
        tbilResult = createParamResult(lftReportTest, "TBIL", "Total Bilirubin", ParameterInputType.MANUAL, CalculationType.NONE, "mg/dL", "0.2", "1.2", 1);
        dbilResult = createParamResult(lftReportTest, "DBIL", "Direct Bilirubin", ParameterInputType.MANUAL, CalculationType.NONE, "mg/dL", "0.2", "0.4", 2);
        ibilResult = createParamResult(lftReportTest, "IBIL", "Indirect Bilirubin", ParameterInputType.CALCULATED, CalculationType.INDIRECT_BILIRUBIN, "mg/dL", "0.0", "0.8", 3);
        astResult = createParamResult(lftReportTest, "AST", "SGOT (AST)", ParameterInputType.MANUAL, CalculationType.NONE, "IU/L", "0.0", "40.0", 4);
        altResult = createParamResult(lftReportTest, "ALT", "SGPT (ALT)", ParameterInputType.MANUAL, CalculationType.NONE, "IU/L", "0.0", "40.0", 5);
        alpResult = createParamResult(lftReportTest, "ALP", "Serum Alkaline Phosphatase", ParameterInputType.MANUAL, CalculationType.NONE, "IU/L", "64.0", "306.0", 6);
        tpResult = createParamResult(lftReportTest, "TP", "Total Protein", ParameterInputType.MANUAL, CalculationType.NONE, "g/dL", "6.0", "9.0", 7);
        albResult = createParamResult(lftReportTest, "ALB", "Albumin", ParameterInputType.MANUAL, CalculationType.NONE, "g/dL", "3.7", "5.3", 8);
        globResult = createParamResult(lftReportTest, "GLOB", "Globulin", ParameterInputType.CALCULATED, CalculationType.GLOBULIN, "g/dL", "2.3", "3.6", 9);
        agRatioResult = createParamResult(lftReportTest, "AG_RATIO", "Albumin/Globulin Ratio", ParameterInputType.CALCULATED, CalculationType.AG_RATIO, "", "1.0", "2.3", 10);

        lftReportTest.getParameterResults().addAll(List.of(
                tbilResult, dbilResult, ibilResult,
                astResult, altResult, alpResult,
                tpResult, albResult, globResult, agRatioResult
        ));

        draftReport = Report.builder()
                .id(UUID.randomUUID())
                .refId("REP-LFT-01")
                .organization(testOrg)
                .status(ReportStatus.DRAFT)
                .reportVersion(1)
                .lockVersion(0L)
                .tests(new ArrayList<>(List.of(lftReportTest)))
                .build();

        lftReportTest.setReport(draftReport);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private TestParameterResult createParamResult(
            ReportTestResult rtr, String code, String name,
            ParameterInputType inputType, CalculationType calcType,
            String unit, String refMin, String refMax, int displayOrder
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
                .referenceMin(refMin.isEmpty() ? null : new BigDecimal(refMin))
                .referenceMax(refMax.isEmpty() ? null : new BigDecimal(refMax))
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
                .referenceMin(refMin.isEmpty() ? null : new BigDecimal(refMin))
                .referenceMax(refMax.isEmpty() ? null : new BigDecimal(refMax))
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
    @DisplayName("LFT Report Update: Auto-derives IBIL, GLOB, and AG_RATIO with exact user audit examples")
    void testUpdateReportParameters_Lft_AutoDerivesIbilGlobulinAndAgRatio() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-LFT-01", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Submit manual values: TBIL=1.00, DBIL=0.34, TP=7.03, ALB=4.10, AST=30, ALT=35, ALP=120
        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "TBIL", "1.00"),
                        new TestParameterResultInput(null, "DBIL", "0.34"),
                        new TestParameterResultInput(null, "TP", "7.03"),
                        new TestParameterResultInput(null, "ALB", "4.10"),
                        new TestParameterResultInput(null, "AST", "30"),
                        new TestParameterResultInput(null, "ALT", "35"),
                        new TestParameterResultInput(null, "ALP", "120")
                )
        );

        ReportResponse response = reportService.updateParameterValues("REP-LFT-01", "RTR-LFT01", request);

        assertThat(response).isNotNull();

        // 1. Indirect Bilirubin: 1.00 - 0.34 = 0.66 mg/dL
        assertThat(ibilResult.getValue()).isEqualTo("0.66");
        assertThat(ibilResult.getNumericValue()).isEqualByComparingTo("0.66");
        assertThat(ibilResult.getFlag()).isEqualTo(ResultFlag.NORMAL); // 0.66 in [0.0, 0.8]

        // 2. Globulin: 7.03 - 4.10 = 2.93 g/dL
        assertThat(globResult.getValue()).isEqualTo("2.93");
        assertThat(globResult.getNumericValue()).isEqualByComparingTo("2.93");
        assertThat(globResult.getFlag()).isEqualTo(ResultFlag.NORMAL); // 2.93 in [2.3, 3.6]

        // 3. A/G Ratio: 4.10 / 2.93 = 1.3993... -> 1.40 (half-up rounding; stripTrailingZeros produces "1.4")
        assertThat(agRatioResult.getValue()).isEqualTo("1.4");
        assertThat(agRatioResult.getNumericValue()).isEqualByComparingTo("1.40");
        assertThat(agRatioResult.getFlag()).isEqualTo(ResultFlag.NORMAL); // 1.40 in [1.0, 2.3]

        // Manual parameters remain correctly saved
        assertThat(tbilResult.getValue()).isEqualTo("1.00");
        assertThat(dbilResult.getValue()).isEqualTo("0.34");
        assertThat(tpResult.getValue()).isEqualTo("7.03");
        assertThat(albResult.getValue()).isEqualTo("4.10");
        assertThat(astResult.getValue()).isEqualTo("30");
        assertThat(altResult.getValue()).isEqualTo("35");
        assertThat(alpResult.getValue()).isEqualTo("120");

        // Response DTO exposes CALCULATED for the 3 derived parameters
        ReportParameterItemResponse respIbil = response.tests().get(0).parameters().stream()
                .filter(p -> "IBIL".equals(p.parameterCode())).findFirst().orElseThrow();
        ReportParameterItemResponse respGlob = response.tests().get(0).parameters().stream()
                .filter(p -> "GLOB".equals(p.parameterCode())).findFirst().orElseThrow();
        ReportParameterItemResponse respAg = response.tests().get(0).parameters().stream()
                .filter(p -> "AG_RATIO".equals(p.parameterCode())).findFirst().orElseThrow();

        assertThat(respIbil.inputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(respGlob.inputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(respAg.inputType()).isEqualTo(ParameterInputType.CALCULATED);
    }

    @Test
    @DisplayName("LFT Report Update: Missing inputs leaves calculated fields empty without failing autosave")
    void testUpdateReportParameters_Lft_MissingInputs_LeavesCalculatedFieldsEmpty() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-LFT-01", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Submit TBIL without DBIL, and TP without ALB
        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "TBIL", "1.00"),
                        new TestParameterResultInput(null, "TP", "7.03")
                )
        );

        ReportResponse response = reportService.updateParameterValues("REP-LFT-01", "RTR-LFT01", request);

        assertThat(response).isNotNull();
        assertThat(ibilResult.getValue()).isNull();
        assertThat(ibilResult.getNumericValue()).isNull();
        assertThat(globResult.getValue()).isNull();
        assertThat(globResult.getNumericValue()).isNull();
        assertThat(agRatioResult.getValue()).isNull();
        assertThat(agRatioResult.getNumericValue()).isNull();
    }

    @Test
    @DisplayName("LFT Report Update: Inconsistent inputs (DBIL > TBIL, ALB > TP) clears calculated fields gracefully")
    void testUpdateReportParameters_Lft_InconsistentInputs_ClearsGracefully() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-LFT-01", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // DBIL > TBIL (1.50 > 1.00), and ALB > TP (5.50 > 5.00)
        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "TBIL", "1.00"),
                        new TestParameterResultInput(null, "DBIL", "1.50"),
                        new TestParameterResultInput(null, "TP", "5.00"),
                        new TestParameterResultInput(null, "ALB", "5.50")
                )
        );

        ReportResponse response = reportService.updateParameterValues("REP-LFT-01", "RTR-LFT01", request);

        assertThat(response).isNotNull();
        // IBIL cleared gracefully
        assertThat(ibilResult.getValue()).isNull();
        assertThat(ibilResult.getNumericValue()).isNull();
        // GLOB and AG_RATIO cleared gracefully
        assertThat(globResult.getValue()).isNull();
        assertThat(globResult.getNumericValue()).isNull();
        assertThat(agRatioResult.getValue()).isNull();
        assertThat(agRatioResult.getNumericValue()).isNull();
    }

    @Test
    @DisplayName("LFT Report Update: Zero Globulin denominator protects against division by zero gracefully")
    void testUpdateReportParameters_Lft_ZeroGlobulin_ProtectsAgainstDivisionByZero() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-LFT-01", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // TP = 4.10, ALB = 4.10 -> GLOB = 0.00 -> A/G ratio denominator = 0
        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "TP", "4.10"),
                        new TestParameterResultInput(null, "ALB", "4.10")
                )
        );

        ReportResponse response = reportService.updateParameterValues("REP-LFT-01", "RTR-LFT01", request);

        assertThat(response).isNotNull();
        assertThat(globResult.getValue()).isEqualTo("0");
        assertThat(globResult.getNumericValue()).isEqualByComparingTo("0.00");
        // AG_RATIO cleared gracefully (no division by zero crash)
        assertThat(agRatioResult.getValue()).isNull();
        assertThat(agRatioResult.getNumericValue()).isNull();
    }

    @Test
    @DisplayName("LFT Report Update: Client attempting manual entry for calculated parameter is rejected")
    void testUpdateReportParameters_Lft_ManualValueForCalculatedParam_ThrowsException() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-LFT-01", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));

        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "TBIL", "1.00"),
                        new TestParameterResultInput(null, "IBIL", "0.66") // Client manually providing calculated IBIL!
                )
        );

        assertThatThrownBy(() -> reportService.updateParameterValues("REP-LFT-01", "RTR-LFT01", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Calculated parameters cannot be manually provided: IBIL");
    }

    @Test
    @DisplayName("LFT Bulk Add: Entities strictly satisfy DB check constraint chk_tpr_calculation_consistency")
    void testAddTestsBulk_LftTest_EntitiesSatisfyCheckConstraint() {
        Report emptyReport = Report.builder()
                .id(UUID.randomUUID())
                .refId("REP-NEW-LFT")
                .organization(testOrg)
                .status(ReportStatus.DRAFT)
                .reportVersion(1)
                .lockVersion(0L)
                .tests(new ArrayList<>())
                .build();

        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-NEW-LFT", testOrg.getId()))
                .thenReturn(Optional.of(emptyReport));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        com.swasthai.report_generator.test.entity.Test lftEntity =
                com.swasthai.report_generator.test.entity.Test.builder()
                        .id(UUID.randomUUID())
                        .refId("TEST-LFT-REF")
                        .code("LFT")
                        .name("Liver Function Test")
                        .status(TestStatus.ACTIVE)
                        .build();

        when(organizationTestService.hasTestAccess("TEST-LFT-REF")).thenReturn(true);
        when(testRepository.findByRefId("TEST-LFT-REF")).thenReturn(Optional.of(lftEntity));

        List<TestParameter> lftParams = List.of(
                TestParameter.builder().code("TBIL").name("Total Bilirubin").inputType(ParameterInputType.MANUAL).calculationType(CalculationType.NONE).displayOrder(1).build(),
                TestParameter.builder().code("DBIL").name("Direct Bilirubin").inputType(ParameterInputType.MANUAL).calculationType(CalculationType.NONE).displayOrder(2).build(),
                TestParameter.builder().code("IBIL").name("Indirect Bilirubin").inputType(ParameterInputType.CALCULATED).calculationType(CalculationType.INDIRECT_BILIRUBIN).displayOrder(3).build(),
                TestParameter.builder().code("AST").name("SGOT (AST)").inputType(ParameterInputType.MANUAL).calculationType(CalculationType.NONE).displayOrder(4).build(),
                TestParameter.builder().code("ALT").name("SGPT (ALT)").inputType(ParameterInputType.MANUAL).calculationType(CalculationType.NONE).displayOrder(5).build(),
                TestParameter.builder().code("ALP").name("Serum Alkaline Phosphatase").inputType(ParameterInputType.MANUAL).calculationType(CalculationType.NONE).displayOrder(6).build(),
                TestParameter.builder().code("TP").name("Total Protein").inputType(ParameterInputType.MANUAL).calculationType(CalculationType.NONE).displayOrder(7).build(),
                TestParameter.builder().code("ALB").name("Albumin").inputType(ParameterInputType.MANUAL).calculationType(CalculationType.NONE).displayOrder(8).build(),
                TestParameter.builder().code("GLOB").name("Globulin").inputType(ParameterInputType.CALCULATED).calculationType(CalculationType.GLOBULIN).displayOrder(9).build(),
                TestParameter.builder().code("AG_RATIO").name("Albumin/Globulin Ratio").inputType(ParameterInputType.CALCULATED).calculationType(CalculationType.AG_RATIO).displayOrder(10).build()
        );

        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(lftEntity.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(lftParams);

        AddReportTestsBulkRequest request = new AddReportTestsBulkRequest(List.of("TEST-LFT-REF"), 0L);
        ReportResponse response = reportService.addTestsBulk("REP-NEW-LFT", request);

        assertThat(response).isNotNull();
        assertThat(emptyReport.getTests()).hasSize(1);
        ReportTestResult reportTest = emptyReport.getTests().get(0);

        Set<CalculationType> allowedCalculatedTypes = Set.of(
                CalculationType.MCV, CalculationType.MCH, CalculationType.MCHC,
                CalculationType.VLDL, CalculationType.LDL_FRIEDEWALD, CalculationType.NON_HDL_CHOLESTEROL,
                CalculationType.CHOL_HDL_RATIO, CalculationType.LDL_HDL_RATIO, CalculationType.INDIRECT_BILIRUBIN,
                CalculationType.GLOBULIN, CalculationType.AG_RATIO, CalculationType.BUN_CREATININE_RATIO,
                CalculationType.UREA_CREATININE_RATIO, CalculationType.EGFR_CKD_EPI_2021,
                CalculationType.ANION_GAP, CalculationType.ANION_GAP_K
        );

        // Verify that EVERY persisted TestParameterResult strictly satisfies chk_tpr_calculation_consistency
        for (TestParameterResult result : reportTest.getParameterResults()) {
            boolean isManualConsistency = result.getInputType() == ParameterInputType.MANUAL
                    && result.getCalculationType() == CalculationType.NONE;

            boolean isCalculatedConsistency = result.getInputType() == ParameterInputType.CALCULATED
                    && allowedCalculatedTypes.contains(result.getCalculationType());

            assertThat(isManualConsistency || isCalculatedConsistency)
                    .withFailMessage("Parameter %s violates chk_tpr_calculation_consistency with inputType=%s and calculationType=%s",
                            result.getParameterCode(), result.getInputType(), result.getCalculationType())
                    .isTrue();
        }

        // Verify that IBIL, GLOB, and AG_RATIO entities are CALCULATED with correct CalculationType
        TestParameterResult ibilEntity = reportTest.getParameterResults().stream()
                .filter(p -> "IBIL".equals(p.getParameterCode())).findFirst().orElseThrow();
        TestParameterResult globEntity = reportTest.getParameterResults().stream()
                .filter(p -> "GLOB".equals(p.getParameterCode())).findFirst().orElseThrow();
        TestParameterResult agEntity = reportTest.getParameterResults().stream()
                .filter(p -> "AG_RATIO".equals(p.getParameterCode())).findFirst().orElseThrow();

        assertThat(ibilEntity.getInputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(ibilEntity.getCalculationType()).isEqualTo(CalculationType.INDIRECT_BILIRUBIN);

        assertThat(globEntity.getInputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(globEntity.getCalculationType()).isEqualTo(CalculationType.GLOBULIN);

        assertThat(agEntity.getInputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(agEntity.getCalculationType()).isEqualTo(CalculationType.AG_RATIO);
    }
}

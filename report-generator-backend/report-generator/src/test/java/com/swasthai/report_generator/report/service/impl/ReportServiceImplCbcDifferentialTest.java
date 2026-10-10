package com.swasthai.report_generator.report.service.impl;

import com.swasthai.report_generator.license.service.LicenseGuard;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationProfile;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationProfileRepository;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
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
import com.swasthai.report_generator.test.calculation.CbcDifferentialValidationException;
import com.swasthai.report_generator.test.calculation.CbcDifferentialValidator;
import com.swasthai.report_generator.test.calculation.calculators.MCHCalculator;
import com.swasthai.report_generator.test.calculation.calculators.MCHCCalculator;
import com.swasthai.report_generator.test.calculation.calculators.MCVCalculator;
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
class ReportServiceImplCbcDifferentialTest {

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
    private ReportTestResult cbcReportTest;
    private ReportTestResult lftReportTest;

    private TestParameterResult hgbResult;
    private TestParameterResult rbcResult;
    private TestParameterResult hctResult;
    private TestParameterResult mcvResult;
    private TestParameterResult neutResult;
    private TestParameterResult lymphResult;
    private TestParameterResult monoResult;
    private TestParameterResult eosResult;
    private TestParameterResult basoResult;

    @BeforeEach
    void setUp() {
        calculationEngine = new CalculationEngineImpl(List.of(
                new MCVCalculator(),
                new MCHCalculator(),
                new MCHCCalculator()
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

        com.swasthai.report_generator.test.entity.Test cbcEntity =
                com.swasthai.report_generator.test.entity.Test.builder()
                        .id(UUID.randomUUID())
                        .refId("TEST-CBC")
                        .code("CBC")
                        .name("Complete Blood Count")
                        .build();

        cbcReportTest = ReportTestResult.builder()
                .id(UUID.randomUUID())
                .refId("RTR-CBC01")
                .test(cbcEntity)
                .testCode("CBC")
                .testName("Complete Blood Count")
                .parameterResults(new ArrayList<>())
                .build();

        hgbResult = createParamResult(cbcReportTest, "HGB", "Hemoglobin", ParameterInputType.MANUAL, CalculationType.NONE, "12.0", "16.0", 1);
        rbcResult = createParamResult(cbcReportTest, "RBC", "Red Blood Cells", ParameterInputType.MANUAL, CalculationType.NONE, "4.0", "5.5", 2);
        hctResult = createParamResult(cbcReportTest, "HCT", "Hematocrit", ParameterInputType.MANUAL, CalculationType.NONE, "36.0", "48.0", 3);
        mcvResult = createParamResult(cbcReportTest, "MCV", "Mean Corpuscular Volume", ParameterInputType.CALCULATED, CalculationType.MCV, "80.0", "100.0", 4);
        neutResult = createParamResult(cbcReportTest, "NEUT", "Neutrophils", ParameterInputType.MANUAL, CalculationType.NONE, "40.0", "75.0", 9);
        lymphResult = createParamResult(cbcReportTest, "LYMPH", "Lymphocytes", ParameterInputType.MANUAL, CalculationType.NONE, "20.0", "45.0", 10);
        monoResult = createParamResult(cbcReportTest, "MONO", "Monocytes", ParameterInputType.MANUAL, CalculationType.NONE, "2.0", "10.0", 11);
        eosResult = createParamResult(cbcReportTest, "EOS", "Eosinophils", ParameterInputType.MANUAL, CalculationType.NONE, "1.0", "6.0", 12);
        basoResult = createParamResult(cbcReportTest, "BASO", "Basophils", ParameterInputType.MANUAL, CalculationType.NONE, "0.0", "2.0", 13);

        cbcReportTest.getParameterResults().addAll(List.of(
                hgbResult, rbcResult, hctResult, mcvResult,
                neutResult, lymphResult, monoResult, eosResult, basoResult
        ));

        com.swasthai.report_generator.test.entity.Test lftEntity =
                com.swasthai.report_generator.test.entity.Test.builder()
                        .id(UUID.randomUUID())
                        .refId("TEST-LFT")
                        .code("LFT")
                        .name("Liver Function Test")
                        .build();

        lftReportTest = ReportTestResult.builder()
                .id(UUID.randomUUID())
                .refId("RTR-LFT01")
                .test(lftEntity)
                .testCode("LFT")
                .testName("Liver Function Test")
                .parameterResults(new ArrayList<>())
                .build();

        draftReport = Report.builder()
                .id(UUID.randomUUID())
                .refId("REP-001")
                .organization(testOrg)
                .status(ReportStatus.DRAFT)
                .reportVersion(1)
                .lockVersion(0L)
                .tests(new ArrayList<>(List.of(cbcReportTest, lftReportTest)))
                .build();

        cbcReportTest.setReport(draftReport);
        lftReportTest.setReport(draftReport);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private TestParameterResult createParamResult(
            ReportTestResult rtr, String code, String name,
            ParameterInputType inputType, CalculationType calcType,
            String refMin, String refMax, int displayOrder
    ) {
        TestParameter param = TestParameter.builder()
                .id(UUID.randomUUID())
                .refId("PARAM-" + code)
                .code(code)
                .name(name)
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(inputType)
                .calculationType(calcType)
                .unit("%")
                .referenceMin(new BigDecimal(refMin))
                .referenceMax(new BigDecimal(refMax))
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
                .unit("%")
                .referenceMin(new BigDecimal(refMin))
                .referenceMax(new BigDecimal(refMax))
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
    @DisplayName("Report Update: Automatically derives MONO and sets BASO to 0 when NEUT, LYMPH, EOS are updated")
    void testUpdateReportParameters_CbcDifferential_AutoDerivesMonoAndBaso() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-001", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Submit HGB=15, RBC=5, HCT=45, and differential: NEUT=68, LYMPH=12, EOS=8
        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "HGB", "15"),
                        new TestParameterResultInput(null, "RBC", "5"),
                        new TestParameterResultInput(null, "HCT", "45"),
                        new TestParameterResultInput(null, "NEUT", "68"),
                        new TestParameterResultInput(null, "LYMPH", "12"),
                        new TestParameterResultInput(null, "EOS", "8")
                )
        );

        ReportResponse response = reportService.updateParameterValues("REP-001", "RTR-CBC01", request);

        assertThat(response).isNotNull();

        // Check that MONO was calculated: 100 - 68 - 12 - 8 = 12
        assertThat(monoResult.getValue()).isEqualTo("12");
        assertThat(monoResult.getNumericValue()).isEqualByComparingTo("12");
        assertThat(monoResult.getFlag()).isEqualTo(ResultFlag.HIGH); // 12 > refMax 10.0

        // Check that BASO was set to 0
        assertThat(basoResult.getValue()).isEqualTo("0");
        assertThat(basoResult.getNumericValue()).isEqualByComparingTo("0");
        assertThat(basoResult.getFlag()).isEqualTo(ResultFlag.NORMAL); // 0 in [0.0, 2.0]

        // Check that MCV was calculated: 45 * 10 / 5 = 90
        assertThat(mcvResult.getValue()).isEqualTo("90");
        assertThat(mcvResult.getNumericValue()).isEqualByComparingTo("90");
    }

    @Test
    @DisplayName("Report Update: Conflicting client-supplied MONO throws CbcDifferentialValidationException")
    void testUpdateReportParameters_ConflictingMono_ThrowsException() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-001", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));

        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "NEUT", "68"),
                        new TestParameterResultInput(null, "LYMPH", "12"),
                        new TestParameterResultInput(null, "EOS", "8"),
                        new TestParameterResultInput(null, "MONO", "15") // Conflicting!
                )
        );

        assertThatThrownBy(() -> reportService.updateParameterValues("REP-001", "RTR-CBC01", request))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Conflicting value supplied for calculated parameter MONO");
    }

    @Test
    @DisplayName("Report Update: Conflicting client-supplied BASO throws CbcDifferentialValidationException")
    void testUpdateReportParameters_ConflictingBaso_ThrowsException() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-001", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));

        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "NEUT", "68"),
                        new TestParameterResultInput(null, "LYMPH", "12"),
                        new TestParameterResultInput(null, "EOS", "8"),
                        new TestParameterResultInput(null, "BASO", "2") // Conflicting!
                )
        );

        assertThatThrownBy(() -> reportService.updateParameterValues("REP-001", "RTR-CBC01", request))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Conflicting value supplied for calculated parameter BASO");
    }

    @Test
    @DisplayName("Report Update: Incomplete differential (missing EOS) throws CbcDifferentialValidationException")
    void testUpdateReportParameters_IncompleteDifferential_ThrowsException() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-001", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));

        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "NEUT", "68"),
                        new TestParameterResultInput(null, "LYMPH", "12")
                )
        );

        assertThatThrownBy(() -> reportService.updateParameterValues("REP-001", "RTR-CBC01", request))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Differential leukocyte count requires manual entry")
                .hasMessageContaining("EOS");
    }

    @Test
    @DisplayName("Report Update: Sum of manual percentages exceeding 100% throws CbcDifferentialValidationException")
    void testUpdateReportParameters_SumExceedingHundred_ThrowsException() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-001", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));

        // NEUT: 70, LYMPH: 25, EOS: 10 -> Sum = 105 -> MONO = -5
        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "NEUT", "70"),
                        new TestParameterResultInput(null, "LYMPH", "25"),
                        new TestParameterResultInput(null, "EOS", "10")
                )
        );

        assertThatThrownBy(() -> reportService.updateParameterValues("REP-001", "RTR-CBC01", request))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Calculated Monocytes percentage is negative (-5%)");
    }

    @Test
    @DisplayName("Report Update: Non-CBC tests (e.g. LFT) are completely unaffected by differential rules")
    void testUpdateReportParameters_NonCbcTest_Unaffected() {
        when(reportRepository.findByRefIdAndOrganization_IdAndDeletedAtIsNull("REP-001", testOrg.getId()))
                .thenReturn(Optional.of(draftReport));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestParameterResult altParam = createParamResult(lftReportTest, "ALT", "Alanine Aminotransferase", ParameterInputType.MANUAL, CalculationType.NONE, "7.0", "56.0", 1);
        lftReportTest.getParameterResults().add(altParam);

        UpdateReportParametersRequest request = new UpdateReportParametersRequest(
                0L,
                List.of(
                        new TestParameterResultInput(null, "ALT", "25")
                )
        );

        ReportResponse response = reportService.updateParameterValues("REP-001", "RTR-LFT01", request);

        assertThat(response).isNotNull();
        assertThat(altParam.getValue()).isEqualTo("25");
        assertThat(altParam.getNumericValue()).isEqualByComparingTo("25");
    }
}

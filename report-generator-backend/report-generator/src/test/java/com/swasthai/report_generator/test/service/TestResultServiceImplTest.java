package com.swasthai.report_generator.test.service;

import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.CbcDifferentialValidationException;
import com.swasthai.report_generator.test.calculation.CbcDifferentialValidator;
import com.swasthai.report_generator.test.calculation.calculators.MCHCalculator;
import com.swasthai.report_generator.test.calculation.calculators.MCHCCalculator;
import com.swasthai.report_generator.test.calculation.calculators.MCVCalculator;
import com.swasthai.report_generator.test.calculation.impl.CalculationEngineImpl;
import com.swasthai.report_generator.test.dto.request.CreateTestResultRequest;
import com.swasthai.report_generator.test.dto.request.TestParameterResultInput;
import com.swasthai.report_generator.test.dto.response.TestResultResponse;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.repository.PatientTestResultRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestParameterResultRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.test.service.impl.TestResultServiceImpl;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestResultServiceImplTest {

    @Mock
    private PatientTestResultRepository patientTestResultRepository;

    @Mock
    private TestParameterResultRepository testParameterResultRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestParameterRepository testParameterRepository;

    @Mock
    private OrganizationTestService organizationTestService;

    private CbcDifferentialValidator cbcDifferentialValidator;
    private CalculationEngine calculationEngine;
    private TestResultServiceImpl testResultService;

    private Organization testOrg;
    private User orgAdminUser;
    private com.swasthai.report_generator.test.entity.Test cbcTest;

    private TestParameter hgbParam;
    private TestParameter rbcParam;
    private TestParameter hctParam;
    private TestParameter wbcParam;
    private TestParameter pltParam;
    private TestParameter mcvParam;
    private TestParameter mchParam;
    private TestParameter mchcParam;
    private TestParameter neutParam;
    private TestParameter lymphParam;
    private TestParameter monoParam;
    private TestParameter eosParam;
    private TestParameter basoParam;

    private List<TestParameter> allCbcParams;

    @BeforeEach
    void setUp() {
        cbcDifferentialValidator = new CbcDifferentialValidator();
        calculationEngine = new CalculationEngineImpl(List.of(
                new MCVCalculator(),
                new MCHCalculator(),
                new MCHCCalculator()
        ));

        testResultService = new TestResultServiceImpl(
                patientTestResultRepository,
                testParameterResultRepository,
                testRepository,
                testParameterRepository,
                organizationTestService,
                calculationEngine,
                cbcDifferentialValidator
        );

        testOrg = Organization.builder()
                .id(UUID.randomUUID())
                .refId("ORG-TEST001")
                .name("Apex Diagnostics")
                .status(OrganizationStatus.ACTIVE)
                .build();

        orgAdminUser = User.builder()
                .id(UUID.randomUUID())
                .refId("USER-ORG001")
                .email("orgadmin@apex.com")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(testOrg)
                .build();

        setAuthenticatedUser(orgAdminUser, "ROLE_ORG_ADMIN");

        cbcTest = com.swasthai.report_generator.test.entity.Test.builder()
                .id(UUID.randomUUID())
                .refId("TEST-CBC123")
                .code("CBC")
                .name("Complete Blood Count")
                .status(TestStatus.ACTIVE)
                .build();

        hgbParam = createParam("PARAM-HGB", "HGB", ParameterInputType.MANUAL, CalculationType.NONE, true, 1, "12.0", "16.0");
        rbcParam = createParam("PARAM-RBC", "RBC", ParameterInputType.MANUAL, CalculationType.NONE, true, 2, "4.0", "5.5");
        hctParam = createParam("PARAM-HCT", "HCT", ParameterInputType.MANUAL, CalculationType.NONE, true, 3, "36.0", "48.0");
        mcvParam = createParam("PARAM-MCV", "MCV", ParameterInputType.CALCULATED, CalculationType.MCV, false, 4, "80.0", "100.0");
        mchParam = createParam("PARAM-MCH", "MCH", ParameterInputType.CALCULATED, CalculationType.MCH, false, 5, "27.0", "33.0");
        mchcParam = createParam("PARAM-MCHC", "MCHC", ParameterInputType.CALCULATED, CalculationType.MCHC, false, 6, "32.0", "36.0");
        wbcParam = createParam("PARAM-WBC", "WBC", ParameterInputType.MANUAL, CalculationType.NONE, true, 8, "4.0", "11.0");
        neutParam = createParam("PARAM-NEUT", "NEUT", ParameterInputType.MANUAL, CalculationType.NONE, false, 9, "40.0", "75.0");
        lymphParam = createParam("PARAM-LYMPH", "LYMPH", ParameterInputType.MANUAL, CalculationType.NONE, false, 10, "20.0", "45.0");
        monoParam = createParam("PARAM-MONO", "MONO", ParameterInputType.MANUAL, CalculationType.NONE, false, 11, "2.0", "10.0");
        eosParam = createParam("PARAM-EOS", "EOS", ParameterInputType.MANUAL, CalculationType.NONE, false, 12, "1.0", "6.0");
        basoParam = createParam("PARAM-BASO", "BASO", ParameterInputType.MANUAL, CalculationType.NONE, false, 13, "0.0", "2.0");
        pltParam = createParam("PARAM-PLT", "PLT", ParameterInputType.MANUAL, CalculationType.NONE, true, 14, "150.0", "450.0");

        allCbcParams = List.of(
                hgbParam, rbcParam, hctParam, mcvParam, mchParam, mchcParam,
                wbcParam, neutParam, lymphParam, monoParam, eosParam, basoParam, pltParam
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private TestParameter createParam(
            String refId, String code, ParameterInputType inputType,
            CalculationType calcType, boolean required, int displayOrder,
            String refMin, String refMax
    ) {
        return TestParameter.builder()
                .id(UUID.randomUUID())
                .refId(refId)
                .test(cbcTest)
                .code(code)
                .name(code)
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(inputType)
                .calculationType(calcType)
                .unit("%")
                .required(required)
                .referenceMin(refMin != null ? new BigDecimal(refMin) : null)
                .referenceMax(refMax != null ? new BigDecimal(refMax) : null)
                .status(TestParameterStatus.ACTIVE)
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

    private CreateTestResultRequest createRequest(List<TestParameterResultInput> params) {
        return CreateTestResultRequest.builder()
                .testRefId("TEST-CBC123")
                .patientRefId("PAT-001")
                .parameters(params)
                .build();
    }

    @Test
    @DisplayName("CBC Result Entry: Automatically derives MONO and sets BASO to 0, saving them as TestParameterResult")
    void testCreateResult_CbcDifferential_AutoCalculatesAndPersistsMonoAndBaso() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(allCbcParams);

        when(patientTestResultRepository.save(any(PatientTestResult.class)))
                .thenAnswer(inv -> {
                    PatientTestResult ptr = inv.getArgument(0);
                    ptr.setId(UUID.randomUUID());
                    ptr.setRefId("PTR-TEST001");
                    return ptr;
                });

        // User submits: HGB, RBC, HCT, WBC, PLT, and differential: NEUT=68, LYMPH=12, EOS=8
        // User DOES NOT submit MONO or BASO
        CreateTestResultRequest request = createRequest(
                List.of(
                        new TestParameterResultInput(hgbParam.getRefId(), "HGB", "15"),
                        new TestParameterResultInput(rbcParam.getRefId(), "RBC", "5"),
                        new TestParameterResultInput(hctParam.getRefId(), "HCT", "45"),
                        new TestParameterResultInput(wbcParam.getRefId(), "WBC", "7.5"),
                        new TestParameterResultInput(pltParam.getRefId(), "PLT", "250"),
                        new TestParameterResultInput(neutParam.getRefId(), "NEUT", "68"),
                        new TestParameterResultInput(lymphParam.getRefId(), "LYMPH", "12"),
                        new TestParameterResultInput(eosParam.getRefId(), "EOS", "8")
                )
        );

        TestResultResponse response = testResultService.createResult(request);

        assertThat(response).isNotNull();

        ArgumentCaptor<PatientTestResult> captor = ArgumentCaptor.forClass(PatientTestResult.class);
        verify(patientTestResultRepository).save(captor.capture());
        PatientTestResult saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(PatientTestResultStatus.CALCULATED);

        // Verify MONO parameter result was persisted
        TestParameterResult savedMono = saved.getParameterResults().stream()
                .filter(pr -> "MONO".equals(pr.getParameterCode()))
                .findFirst()
                .orElseThrow();
        assertThat(savedMono.getNumericValue()).isEqualByComparingTo("12");
        assertThat(savedMono.getValue()).isEqualTo("12");
        assertThat(savedMono.getFlag()).isEqualTo(ResultFlag.HIGH); // 12 > refMax 10.0

        // Verify BASO parameter result was persisted
        TestParameterResult savedBaso = saved.getParameterResults().stream()
                .filter(pr -> "BASO".equals(pr.getParameterCode()))
                .findFirst()
                .orElseThrow();
        assertThat(savedBaso.getNumericValue()).isEqualByComparingTo("0");
        assertThat(savedBaso.getValue()).isEqualTo("0");
        assertThat(savedBaso.getFlag()).isEqualTo(ResultFlag.NORMAL); // 0 in [0.0, 2.0]

        // Verify MCV calculated index was also computed
        TestParameterResult savedMcv = saved.getParameterResults().stream()
                .filter(pr -> "MCV".equals(pr.getParameterCode()))
                .findFirst()
                .orElseThrow();
        assertThat(savedMcv.getNumericValue()).isEqualByComparingTo("90.0");
    }

    @Test
    @DisplayName("CBC Result Entry: Incomplete differential (missing EOS) is rejected")
    void testCreateResult_CbcDifferential_Incomplete_ThrowsException() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(allCbcParams);

        CreateTestResultRequest request = createRequest(
                List.of(
                        new TestParameterResultInput(hgbParam.getRefId(), "HGB", "15"),
                        new TestParameterResultInput(rbcParam.getRefId(), "RBC", "5"),
                        new TestParameterResultInput(hctParam.getRefId(), "HCT", "45"),
                        new TestParameterResultInput(wbcParam.getRefId(), "WBC", "7.5"),
                        new TestParameterResultInput(pltParam.getRefId(), "PLT", "250"),
                        new TestParameterResultInput(neutParam.getRefId(), "NEUT", "68"),
                        new TestParameterResultInput(lymphParam.getRefId(), "LYMPH", "12")
                )
        );

        assertThatThrownBy(() -> testResultService.createResult(request))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Differential leukocyte count requires manual entry")
                .hasMessageContaining("EOS");
    }

    @Test
    @DisplayName("CBC Result Entry: Conflicting MONO supplied by client is rejected")
    void testCreateResult_CbcDifferential_ConflictingMono_ThrowsException() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(allCbcParams);

        CreateTestResultRequest request = createRequest(
                List.of(
                        new TestParameterResultInput(hgbParam.getRefId(), "HGB", "15"),
                        new TestParameterResultInput(rbcParam.getRefId(), "RBC", "5"),
                        new TestParameterResultInput(hctParam.getRefId(), "HCT", "45"),
                        new TestParameterResultInput(wbcParam.getRefId(), "WBC", "7.5"),
                        new TestParameterResultInput(pltParam.getRefId(), "PLT", "250"),
                        new TestParameterResultInput(neutParam.getRefId(), "NEUT", "68"),
                        new TestParameterResultInput(lymphParam.getRefId(), "LYMPH", "12"),
                        new TestParameterResultInput(eosParam.getRefId(), "EOS", "8"),
                        new TestParameterResultInput(monoParam.getRefId(), "MONO", "15") // Conflicting!
                )
        );

        assertThatThrownBy(() -> testResultService.createResult(request))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Conflicting value supplied for calculated parameter MONO");
    }

    @Test
    @DisplayName("CBC Result Entry: Manual percentages exceeding 100% (negative MONO) is rejected")
    void testCreateResult_CbcDifferential_NegativeMono_ThrowsException() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(allCbcParams);

        CreateTestResultRequest request = createRequest(
                List.of(
                        new TestParameterResultInput(hgbParam.getRefId(), "HGB", "15"),
                        new TestParameterResultInput(rbcParam.getRefId(), "RBC", "5"),
                        new TestParameterResultInput(hctParam.getRefId(), "HCT", "45"),
                        new TestParameterResultInput(wbcParam.getRefId(), "WBC", "7.5"),
                        new TestParameterResultInput(pltParam.getRefId(), "PLT", "250"),
                        new TestParameterResultInput(neutParam.getRefId(), "NEUT", "70"),
                        new TestParameterResultInput(lymphParam.getRefId(), "LYMPH", "25"),
                        new TestParameterResultInput(eosParam.getRefId(), "EOS", "10") // Sum = 105 -> MONO = -5
                )
        );

        assertThatThrownBy(() -> testResultService.createResult(request))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Calculated Monocytes percentage is negative (-5%)");
    }

    @Test
    @DisplayName("CBC Result Entry: Basic CBC without differential succeeds")
    void testCreateResult_BasicCbcWithoutDifferential_Success() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(allCbcParams);

        when(patientTestResultRepository.save(any(PatientTestResult.class)))
                .thenAnswer(inv -> {
                    PatientTestResult ptr = inv.getArgument(0);
                    ptr.setId(UUID.randomUUID());
                    ptr.setRefId("PTR-TEST002");
                    return ptr;
                });

        CreateTestResultRequest request = createRequest(
                List.of(
                        new TestParameterResultInput(hgbParam.getRefId(), "HGB", "15"),
                        new TestParameterResultInput(rbcParam.getRefId(), "RBC", "5"),
                        new TestParameterResultInput(hctParam.getRefId(), "HCT", "45"),
                        new TestParameterResultInput(wbcParam.getRefId(), "WBC", "7.5"),
                        new TestParameterResultInput(pltParam.getRefId(), "PLT", "250")
                )
        );

        TestResultResponse response = testResultService.createResult(request);

        assertThat(response).isNotNull();
    }
}

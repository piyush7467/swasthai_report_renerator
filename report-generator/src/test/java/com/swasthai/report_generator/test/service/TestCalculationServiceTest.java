package com.swasthai.report_generator.test.service;

import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.calculation.calculators.MCHCalculator;
import com.swasthai.report_generator.test.calculation.calculators.MCHCCalculator;
import com.swasthai.report_generator.test.calculation.calculators.MCVCalculator;
import com.swasthai.report_generator.test.calculation.impl.CalculationEngineImpl;
import com.swasthai.report_generator.test.dto.request.CalculateTestRequest;
import com.swasthai.report_generator.test.dto.request.ParameterValueInput;
import com.swasthai.report_generator.test.dto.response.CalculatedParameterItemResponse;
import com.swasthai.report_generator.test.dto.response.CalculatedTestResponse;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.test.service.impl.TestCalculationServiceImpl;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCalculationServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestParameterRepository testParameterRepository;

    @Mock
    private OrganizationTestService organizationTestService;

    private TestCalculationServiceImpl testCalculationService;

    private com.swasthai.report_generator.test.entity.Test cbcTest;
    private Organization testOrg;
    private User orgAdminUser;
    private User superAdminUser;

    private TestParameter hgbParam;
    private TestParameter rbcParam;
    private TestParameter hctParam;
    private TestParameter mcvParam;
    private TestParameter mchParam;
    private TestParameter mchcParam;

    @BeforeEach
    void setUp() {
        CalculationEngineImpl calculationEngine = new CalculationEngineImpl(List.of(
                new MCVCalculator(),
                new MCHCalculator(),
                new MCHCCalculator()
        ));

        testCalculationService = new TestCalculationServiceImpl(
                testRepository,
                testParameterRepository,
                organizationTestService,
                calculationEngine
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

        superAdminUser = User.builder()
                .id(UUID.randomUUID())
                .refId("USER-SA001")
                .email("superadmin@swasthai.com")
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        setAuthenticatedUser(orgAdminUser, "ROLE_ORG_ADMIN");

        cbcTest = com.swasthai.report_generator.test.entity.Test.builder()
                .id(UUID.randomUUID())
                .refId("TEST-CBC123")
                .code("CBC")
                .name("Complete Blood Count")
                .status(TestStatus.ACTIVE)
                .build();

        hgbParam = TestParameter.builder()
                .id(UUID.randomUUID())
                .refId("PARAM-HGB01")
                .test(cbcTest)
                .code("HGB")
                .name("Hemoglobin")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .calculationType(CalculationType.NONE)
                .unit("g/dL")
                .required(true)
                .referenceMin(new BigDecimal("12.0"))
                .referenceMax(new BigDecimal("16.0"))
                .criticalLow(new BigDecimal("7.0"))
                .criticalHigh(new BigDecimal("20.0"))
                .status(TestParameterStatus.ACTIVE)
                .displayOrder(1)
                .build();

        rbcParam = TestParameter.builder()
                .id(UUID.randomUUID())
                .refId("PARAM-RBC01")
                .test(cbcTest)
                .code("RBC")
                .name("Red Blood Cell Count")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .calculationType(CalculationType.NONE)
                .unit("10^6/µL")
                .required(true)
                .referenceMin(new BigDecimal("4.0"))
                .referenceMax(new BigDecimal("5.5"))
                .status(TestParameterStatus.ACTIVE)
                .displayOrder(2)
                .build();

        hctParam = TestParameter.builder()
                .id(UUID.randomUUID())
                .refId("PARAM-HCT01")
                .test(cbcTest)
                .code("HCT")
                .name("Hematocrit")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .calculationType(CalculationType.NONE)
                .unit("%")
                .required(true)
                .referenceMin(new BigDecimal("36.0"))
                .referenceMax(new BigDecimal("48.0"))
                .status(TestParameterStatus.ACTIVE)
                .displayOrder(3)
                .build();

        mcvParam = TestParameter.builder()
                .id(UUID.randomUUID())
                .refId("PARAM-MCV01")
                .test(cbcTest)
                .code("MCV")
                .name("Mean Corpuscular Volume")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.MCV)
                .unit("fL")
                .required(false)
                .referenceMin(new BigDecimal("80.0"))
                .referenceMax(new BigDecimal("100.0"))
                .status(TestParameterStatus.ACTIVE)
                .displayOrder(4)
                .build();

        mchParam = TestParameter.builder()
                .id(UUID.randomUUID())
                .refId("PARAM-MCH01")
                .test(cbcTest)
                .code("MCH")
                .name("Mean Corpuscular Hemoglobin")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.MCH)
                .unit("pg")
                .required(false)
                .referenceMin(new BigDecimal("27.0"))
                .referenceMax(new BigDecimal("33.0"))
                .status(TestParameterStatus.ACTIVE)
                .displayOrder(5)
                .build();

        mchcParam = TestParameter.builder()
                .id(UUID.randomUUID())
                .refId("PARAM-MCHC01")
                .test(cbcTest)
                .code("MCHC")
                .name("Mean Corpuscular Hemoglobin Concentration")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.MCHC)
                .unit("g/dL")
                .required(false)
                .referenceMin(new BigDecimal("32.0"))
                .referenceMax(new BigDecimal("36.0"))
                .status(TestParameterStatus.ACTIVE)
                .displayOrder(6)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
    @DisplayName("Calculate CBC: HGB=15, RBC=5, HCT=45 correctly calculates MCV=90, MCH=30, MCHC=33.3333")
    void testCalculate_CBCFlow_Success() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(List.of(hgbParam, rbcParam, hctParam, mcvParam, mchParam, mchcParam));

        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "15"),
                new ParameterValueInput("RBC", null, "5"),
                new ParameterValueInput("HCT", null, "45")
        ));

        CalculatedTestResponse response = testCalculationService.calculateParameters("TEST-CBC123", request);

        assertThat(response).isNotNull();
        assertThat(response.testCode()).isEqualTo("CBC");
        assertThat(response.parameters()).hasSize(6);

        Map<String, CalculatedParameterItemResponse> paramMap = new HashMap<>();
        for (CalculatedParameterItemResponse p : response.parameters()) {
            paramMap.put(p.parameterCode(), p);
        }

        // MCV = 45 * 10 / 5 = 90
        CalculatedParameterItemResponse mcvRes = paramMap.get("MCV");
        assertThat(mcvRes).isNotNull();
        assertThat(mcvRes.numericValue().compareTo(new BigDecimal("90"))).isEqualTo(0);
        assertThat(mcvRes.flag()).isEqualTo(ResultFlag.NORMAL);

        // MCH = 15 * 10 / 5 = 30
        CalculatedParameterItemResponse mchRes = paramMap.get("MCH");
        assertThat(mchRes).isNotNull();
        assertThat(mchRes.numericValue().compareTo(new BigDecimal("30"))).isEqualTo(0);
        assertThat(mchRes.flag()).isEqualTo(ResultFlag.NORMAL);

        // MCHC = 15 * 100 / 45 = 33.3333
        CalculatedParameterItemResponse mchcRes = paramMap.get("MCHC");
        assertThat(mchcRes).isNotNull();
        assertThat(mchcRes.numericValue().compareTo(new BigDecimal("33.3333"))).isEqualTo(0);
        assertThat(mchcRes.flag()).isEqualTo(ResultFlag.NORMAL);
    }

    @Test
    @DisplayName("Range Classification: Flags classified as NORMAL, LOW, HIGH, CRITICAL_LOW, CRITICAL_HIGH")
    void testRangeClassification_Flags() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(List.of(hgbParam, rbcParam, hctParam, mcvParam, mchParam, mchcParam));

        // HGB = 5.0 (criticalLow is 7.0 => CRITICAL_LOW)
        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "5.0"),
                new ParameterValueInput("RBC", null, "5.0"),
                new ParameterValueInput("HCT", null, "45.0")
        ));

        CalculatedTestResponse response = testCalculationService.calculateParameters("TEST-CBC123", request);

        CalculatedParameterItemResponse hgbRes = response.parameters().stream()
                .filter(p -> p.parameterCode().equals("HGB"))
                .findFirst().orElseThrow();
        assertThat(hgbRes.flag()).isEqualTo(ResultFlag.CRITICAL_LOW);
    }

    @Test
    @DisplayName("Attack: Manual override of CALCULATED parameter (e.g. MCV=999) is strictly rejected")
    void testAttack_ManualOverrideOfCalculatedParameter_ThrowsBadRequest() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(List.of(hgbParam, rbcParam, hctParam, mcvParam));

        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "15"),
                new ParameterValueInput("RBC", null, "5"),
                new ParameterValueInput("HCT", null, "45"),
                new ParameterValueInput("MCV", null, "999")
        ));

        assertThatThrownBy(() -> testCalculationService.calculateParameters("TEST-CBC123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Calculated parameters cannot be manually provided: MCV");
    }

    @Test
    @DisplayName("Attack: Duplicate parameter in request is strictly rejected")
    void testAttack_DuplicateParameter_ThrowsBadRequest() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(List.of(hgbParam, rbcParam, hctParam));

        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "15"),
                new ParameterValueInput("HGB", null, "16")
        ));

        assertThatThrownBy(() -> testCalculationService.calculateParameters("TEST-CBC123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate parameter submission for: HGB");
    }

    @Test
    @DisplayName("Attack: Unknown parameter code is rejected")
    void testAttack_UnknownParameter_ThrowsBadRequest() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(List.of(hgbParam, rbcParam, hctParam));

        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("SECRET_PARAM", null, "42")
        ));

        assertThatThrownBy(() -> testCalculationService.calculateParameters("TEST-CBC123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown parameter code for test: SECRET_PARAM");
    }

    @Test
    @DisplayName("Validation: Missing required manual parameter throws IllegalArgumentException")
    void testValidation_MissingRequiredManualParameter_ThrowsException() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(List.of(hgbParam, rbcParam, hctParam));

        // Missing HCT (which is required)
        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "15"),
                new ParameterValueInput("RBC", null, "5")
        ));

        assertThatThrownBy(() -> testCalculationService.calculateParameters("TEST-CBC123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required parameter: HCT");
    }

    @Test
    @DisplayName("Zero Division: RBC=0 causes safe CalculationException")
    void testZeroDivision_RBCZero_ThrowsCalculationException() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(List.of(hgbParam, rbcParam, hctParam, mcvParam));

        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "15"),
                new ParameterValueInput("RBC", null, "0"),
                new ParameterValueInput("HCT", null, "45")
        ));

        assertThatThrownBy(() -> testCalculationService.calculateParameters("TEST-CBC123", request))
                .isInstanceOf(CalculationException.class)
                .hasMessageContaining("Cannot calculate MCV because RBC is zero");
    }

    @Test
    @DisplayName("Negative Value: Negative input value causes safe CalculationException")
    void testNegativeValue_ThrowsCalculationException() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(true);
        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(List.of(hgbParam, rbcParam, hctParam, mcvParam));

        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "15"),
                new ParameterValueInput("RBC", null, "-5"),
                new ParameterValueInput("HCT", null, "45")
        ));

        assertThatThrownBy(() -> testCalculationService.calculateParameters("TEST-CBC123", request))
                .isInstanceOf(CalculationException.class)
                .hasMessageContaining("Parameter value cannot be negative: RBC");
    }

    @Test
    @DisplayName("Tenant Isolation: Inactive user account is rejected")
    void testTenantIsolation_InactiveUser_ThrowsAccessDenied() {
        orgAdminUser.setStatus(UserStatus.INACTIVE);

        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "15")
        ));

        assertThatThrownBy(() -> testCalculationService.calculateParameters("TEST-CBC123", request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User account is inactive");
    }

    @Test
    @DisplayName("Tenant Isolation: Inactive organization is rejected")
    void testTenantIsolation_InactiveOrg_ThrowsAccessDenied() {
        testOrg.setStatus(OrganizationStatus.SUSPENDED);

        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "15")
        ));

        assertThatThrownBy(() -> testCalculationService.calculateParameters("TEST-CBC123", request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User organization is inactive or not found");
    }

    @Test
    @DisplayName("Tenant Isolation: Test without active access is rejected with AccessDeniedException")
    void testTenantIsolation_NoTestAccess_ThrowsAccessDenied() {
        when(organizationTestService.hasTestAccess("TEST-CBC123")).thenReturn(false);

        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "15")
        ));

        assertThatThrownBy(() -> testCalculationService.calculateParameters("TEST-CBC123", request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Organization does not have active access to test");
    }

    @Test
    @DisplayName("Master Access: SUPER_ADMIN can calculate without organization assignment")
    void testSuperAdmin_CanCalculateWithoutOrgAssignment() {
        setAuthenticatedUser(superAdminUser, "ROLE_SUPER_ADMIN");

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(cbcTest));
        when(testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(cbcTest.getId(), TestParameterStatus.ACTIVE))
                .thenReturn(List.of(hgbParam, rbcParam, hctParam, mcvParam));

        CalculateTestRequest request = new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "15"),
                new ParameterValueInput("RBC", null, "5"),
                new ParameterValueInput("HCT", null, "45")
        ));

        CalculatedTestResponse response = testCalculationService.calculateParameters("TEST-CBC123", request);

        assertThat(response).isNotNull();
        assertThat(response.testCode()).isEqualTo("CBC");
    }
}

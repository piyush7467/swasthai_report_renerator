package com.swasthai.report_generator.test.service;

import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.calculators.MCHCalculator;
import com.swasthai.report_generator.test.calculation.calculators.MCHCCalculator;
import com.swasthai.report_generator.test.calculation.calculators.MCVCalculator;
import com.swasthai.report_generator.test.calculation.impl.CalculationEngineImpl;
import com.swasthai.report_generator.test.service.OrganizationTestService;
import com.swasthai.report_generator.test.dto.request.CreateTestParameterRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestParameterRequest;
import com.swasthai.report_generator.test.dto.response.TestParameterResponse;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.test.service.impl.TestParameterServiceImpl;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestParameterServiceTest {

    @Mock
    private TestParameterRepository testParameterRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private OrganizationTestService organizationTestService;

    private CalculationEngine calculationEngine;
    private TestParameterServiceImpl testParameterService;

    private com.swasthai.report_generator.test.entity.Test parentTest;

    @BeforeEach
    void setUp() {
        calculationEngine = new CalculationEngineImpl(List.of(
                new MCVCalculator(),
                new MCHCalculator(),
                new MCHCCalculator()
        ));

        testParameterService = new TestParameterServiceImpl(
                testParameterRepository,
                testRepository,
                organizationTestService,
                calculationEngine
        );

        User superAdmin = User.builder()
                .id(UUID.randomUUID())
                .email("superadmin@swasthai.com")
                .role(Role.SUPER_ADMIN)
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                superAdmin.getEmail(),
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );
        auth.setDetails(superAdmin);
        SecurityContextHolder.getContext().setAuthentication(auth);

        parentTest = com.swasthai.report_generator.test.entity.Test.builder()
                .id(UUID.randomUUID())
                .refId("TEST-CBC123")
                .code("CBC")
                .name("Complete Blood Count")
                .status(TestStatus.ACTIVE)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CreateTestParameterRequest createRequest(
            String code,
            ParameterInputType inputType,
            CalculationType calculationType
    ) {
        return new CreateTestParameterRequest(
                code,
                "Parameter " + code,
                "Description for " + code,
                TestParameterDataType.DECIMAL,
                inputType,
                calculationType,
                "g/dL",
                true,
                1,
                new BigDecimal("10.0"),
                new BigDecimal("20.0"),
                new BigDecimal("5.0"),
                new BigDecimal("25.0"),
                "Report description",
                "Guidance"
        );
    }

    @Test
    @DisplayName("Create Parameter: MANUAL inputType with NONE calculationType succeeds")
    void testCreateParameter_ManualWithNone_Success() {
        CreateTestParameterRequest request = createRequest("HB", ParameterInputType.MANUAL, CalculationType.NONE);

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));
        when(testParameterRepository.existsByTest_IdAndCode(any(), eq("HB"))).thenReturn(false);
        when(testParameterRepository.save(any(TestParameter.class))).thenAnswer(invocation -> {
            TestParameter param = invocation.getArgument(0);
            param.setId(UUID.randomUUID());
            param.setCreatedAt(Instant.now());
            param.setUpdatedAt(Instant.now());
            return param;
        });

        TestParameterResponse response = testParameterService.createParameter("TEST-CBC123", request);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("HB");
        assertThat(response.getInputType()).isEqualTo(ParameterInputType.MANUAL);
        assertThat(response.getCalculationType()).isEqualTo(CalculationType.NONE);

        ArgumentCaptor<TestParameter> captor = ArgumentCaptor.forClass(TestParameter.class);
        verify(testParameterRepository).save(captor.capture());
        assertThat(captor.getValue().getInputType()).isEqualTo(ParameterInputType.MANUAL);
        assertThat(captor.getValue().getCalculationType()).isEqualTo(CalculationType.NONE);
    }

    @Test
    @DisplayName("Create Parameter: CALCULATED inputType with MCV calculationType succeeds")
    void testCreateParameter_CalculatedWithMcv_Success() {
        CreateTestParameterRequest request = createRequest("MCV", ParameterInputType.CALCULATED, CalculationType.MCV);

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));
        when(testParameterRepository.existsByTest_IdAndCode(any(), eq("MCV"))).thenReturn(false);
        when(testParameterRepository.save(any(TestParameter.class))).thenAnswer(invocation -> {
            TestParameter param = invocation.getArgument(0);
            param.setId(UUID.randomUUID());
            param.setCreatedAt(Instant.now());
            param.setUpdatedAt(Instant.now());
            return param;
        });

        TestParameterResponse response = testParameterService.createParameter("TEST-CBC123", request);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("MCV");
        assertThat(response.getInputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(response.getCalculationType()).isEqualTo(CalculationType.MCV);
    }

    @Test
    @DisplayName("Create Parameter: CALCULATED inputType with MCH calculationType succeeds")
    void testCreateParameter_CalculatedWithMch_Success() {
        CreateTestParameterRequest request = createRequest("MCH", ParameterInputType.CALCULATED, CalculationType.MCH);

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));
        when(testParameterRepository.existsByTest_IdAndCode(any(), eq("MCH"))).thenReturn(false);
        when(testParameterRepository.save(any(TestParameter.class))).thenAnswer(invocation -> {
            TestParameter param = invocation.getArgument(0);
            param.setId(UUID.randomUUID());
            param.setCreatedAt(Instant.now());
            param.setUpdatedAt(Instant.now());
            return param;
        });

        TestParameterResponse response = testParameterService.createParameter("TEST-CBC123", request);

        assertThat(response).isNotNull();
        assertThat(response.getInputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(response.getCalculationType()).isEqualTo(CalculationType.MCH);
    }

    @Test
    @DisplayName("Create Parameter: CALCULATED inputType with MCHC calculationType succeeds")
    void testCreateParameter_CalculatedWithMchc_Success() {
        CreateTestParameterRequest request = createRequest("MCHC", ParameterInputType.CALCULATED, CalculationType.MCHC);

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));
        when(testParameterRepository.existsByTest_IdAndCode(any(), eq("MCHC"))).thenReturn(false);
        when(testParameterRepository.save(any(TestParameter.class))).thenAnswer(invocation -> {
            TestParameter param = invocation.getArgument(0);
            param.setId(UUID.randomUUID());
            param.setCreatedAt(Instant.now());
            param.setUpdatedAt(Instant.now());
            return param;
        });

        TestParameterResponse response = testParameterService.createParameter("TEST-CBC123", request);

        assertThat(response).isNotNull();
        assertThat(response.getInputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(response.getCalculationType()).isEqualTo(CalculationType.MCHC);
    }

    @Test
    @DisplayName("Create Parameter: MANUAL inputType with MCV calculationType throws IllegalArgumentException")
    void testCreateParameter_ManualWithMcv_ThrowsException() {
        CreateTestParameterRequest request = createRequest("MCV", ParameterInputType.MANUAL, CalculationType.MCV);

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));

        assertThatThrownBy(() -> testParameterService.createParameter("TEST-CBC123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Manual parameter must have calculation type NONE");

        verify(testParameterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Parameter: MANUAL inputType with MCH calculationType throws IllegalArgumentException")
    void testCreateParameter_ManualWithMch_ThrowsException() {
        CreateTestParameterRequest request = createRequest("MCH", ParameterInputType.MANUAL, CalculationType.MCH);

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));

        assertThatThrownBy(() -> testParameterService.createParameter("TEST-CBC123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Manual parameter must have calculation type NONE");

        verify(testParameterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Parameter: CALCULATED inputType with NONE calculationType throws IllegalArgumentException")
    void testCreateParameter_CalculatedWithNone_ThrowsException() {
        CreateTestParameterRequest request = createRequest("MCV", ParameterInputType.CALCULATED, CalculationType.NONE);

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));

        assertThatThrownBy(() -> testParameterService.createParameter("TEST-CBC123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Calculated parameter must have a valid calculation type");

        verify(testParameterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Parameter: CALCULATED inputType with null calculationType throws IllegalArgumentException")
    void testCreateParameter_CalculatedWithNull_ThrowsException() {
        CreateTestParameterRequest request = createRequest("MCV", ParameterInputType.CALCULATED, null);

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));

        assertThatThrownBy(() -> testParameterService.createParameter("TEST-CBC123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Calculated parameter must have a valid calculation type");

        verify(testParameterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Parameter: Partial update preserves existing inputType and calculationType")
    void testUpdateParameter_PatchPreservesExistingCalculationConfig_Success() {
        TestParameter existing = TestParameter.builder()
                .id(UUID.randomUUID())
                .refId("PARAM-MCV001")
                .test(parentTest)
                .code("MCV")
                .name("Mean Corpuscular Volume")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.MCV)
                .unit("fL")
                .required(true)
                .displayOrder(5)
                .status(TestParameterStatus.ACTIVE)
                .version(1)
                .build();

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));
        when(testParameterRepository.findByRefId("PARAM-MCV001")).thenReturn(Optional.of(existing));
        when(testParameterRepository.save(any(TestParameter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Update name only, leave inputType and calculationType null
        UpdateTestParameterRequest updateRequest = new UpdateTestParameterRequest(
                null, "Updated MCV Name", null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        TestParameterResponse response = testParameterService.updateParameter("PARAM-MCV001", updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated MCV Name");
        assertThat(response.getInputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(response.getCalculationType()).isEqualTo(CalculationType.MCV);
    }

    @Test
    @DisplayName("Update Parameter: Update to CALCULATED + MCV succeeds")
    void testUpdateParameter_PatchChangeToCalculated_Success() {
        TestParameter existing = TestParameter.builder()
                .id(UUID.randomUUID())
                .refId("PARAM-MCV001")
                .test(parentTest)
                .code("MCV")
                .name("Mean Corpuscular Volume")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .calculationType(CalculationType.NONE)
                .unit("fL")
                .required(true)
                .displayOrder(5)
                .status(TestParameterStatus.ACTIVE)
                .version(1)
                .build();

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));
        when(testParameterRepository.findByRefId("PARAM-MCV001")).thenReturn(Optional.of(existing));
        when(testParameterRepository.save(any(TestParameter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTestParameterRequest updateRequest = new UpdateTestParameterRequest(
                null, null, null, null, ParameterInputType.CALCULATED, CalculationType.MCV, null, null, null, null, null, null, null, null, null, null
        );

        TestParameterResponse response = testParameterService.updateParameter("PARAM-MCV001", updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getInputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(response.getCalculationType()).isEqualTo(CalculationType.MCV);
    }

    @Test
    @DisplayName("Update Parameter: Invalid combination in update throws IllegalArgumentException")
    void testUpdateParameter_PatchInvalidCombination_ThrowsException() {
        TestParameter existing = TestParameter.builder()
                .id(UUID.randomUUID())
                .refId("PARAM-HB001")
                .test(parentTest)
                .code("HB")
                .name("Hemoglobin")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .calculationType(CalculationType.NONE)
                .unit("g/dL")
                .required(true)
                .displayOrder(1)
                .status(TestParameterStatus.ACTIVE)
                .version(1)
                .build();

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));
        when(testParameterRepository.findByRefId("PARAM-HB001")).thenReturn(Optional.of(existing));

        // Existing is MANUAL, updating calculationType to MCV without changing inputType is invalid
        UpdateTestParameterRequest updateRequest = new UpdateTestParameterRequest(
                null, null, null, null, null, CalculationType.MCV, null, null, null, null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> testParameterService.updateParameter("PARAM-HB001", updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Manual parameter must have calculation type NONE");

        verify(testParameterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Parameter: CALCULATED inputType with TEXT data type throws IllegalArgumentException")
    void testCreateParameter_CalculatedWithTextDataType_ThrowsException() {
        CreateTestParameterRequest request = new CreateTestParameterRequest(
                "MCV", "Mean Corpuscular Volume", "Desc",
                TestParameterDataType.TEXT, ParameterInputType.CALCULATED, CalculationType.MCV,
                "fL", true, 5, null, null, null, null, null, null
        );

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));

        assertThatThrownBy(() -> testParameterService.createParameter("TEST-CBC123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data type TEXT is not supported for calculation type: MCV");

        verify(testParameterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Parameter: CALCULATED inputType with BOOLEAN data type throws IllegalArgumentException")
    void testCreateParameter_CalculatedWithBooleanDataType_ThrowsException() {
        CreateTestParameterRequest request = new CreateTestParameterRequest(
                "MCH", "Mean Corpuscular Hemoglobin", "Desc",
                TestParameterDataType.BOOLEAN, ParameterInputType.CALCULATED, CalculationType.MCH,
                "pg", true, 6, null, null, null, null, null, null
        );

        when(testRepository.findByRefId("TEST-CBC123")).thenReturn(Optional.of(parentTest));

        assertThatThrownBy(() -> testParameterService.createParameter("TEST-CBC123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data type BOOLEAN is not supported for calculation type: MCH");

        verify(testParameterRepository, never()).save(any());
    }
}

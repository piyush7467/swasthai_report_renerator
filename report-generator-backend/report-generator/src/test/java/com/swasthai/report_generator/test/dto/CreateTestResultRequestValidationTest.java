package com.swasthai.report_generator.test.dto;

import com.swasthai.report_generator.test.dto.request.CreateTestResultRequest;
import com.swasthai.report_generator.test.dto.request.TestParameterResultInput;
import com.swasthai.report_generator.test.dto.response.TestParameterResultResponse;
import com.swasthai.report_generator.test.dto.response.TestResultResponse;
import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.PatientTestResultStatus;
import com.swasthai.report_generator.test.entity.ResultFlag;
import com.swasthai.report_generator.test.entity.TestParameterDataType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTestResultRequestValidationTest {

    private static Validator validator;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
    }

    private CreateTestResultRequest buildValidRequest() {
        return CreateTestResultRequest.builder()
                .testRefId("TEST-CBC123")
                .patientRefId("PAT-987654")
                .performedAt(Instant.now())
                .parameters(List.of(
                        new TestParameterResultInput("PARAM-HGB", "HGB", "14.5"),
                        new TestParameterResultInput("PARAM-RBC", "RBC", "4.8")
                ))
                .build();
    }

    @Nested
    @DisplayName("CreateTestResultRequest Validation Tests")
    class RequestValidation {

        @Test
        @DisplayName("Valid request passes validation with zero violations")
        void testValidRequest_Passes() {
            CreateTestResultRequest request = buildValidRequest();
            Set<ConstraintViolation<CreateTestResultRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Blank or null testRefId fails validation")
        void testTestRefId_BlankOrNull_Fails() {
            CreateTestResultRequest req1 = buildValidRequest();
            req1.setTestRefId(null);
            assertThat(validator.validate(req1))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("testRefId"));

            CreateTestResultRequest req2 = buildValidRequest();
            req2.setTestRefId("   ");
            assertThat(validator.validate(req2))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("testRefId"));
        }

        @Test
        @DisplayName("Oversized testRefId (> 30 chars) fails validation")
        void testTestRefId_Oversized_Fails() {
            CreateTestResultRequest req = buildValidRequest();
            req.setTestRefId("A".repeat(31));
            assertThat(validator.validate(req))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("testRefId")
                            && v.getMessage().contains("must not exceed 30 characters"));
        }

        @Test
        @DisplayName("Blank or null patientRefId fails validation")
        void testPatientRefId_BlankOrNull_Fails() {
            CreateTestResultRequest req1 = buildValidRequest();
            req1.setPatientRefId(null);
            assertThat(validator.validate(req1))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("patientRefId"));

            CreateTestResultRequest req2 = buildValidRequest();
            req2.setPatientRefId("");
            assertThat(validator.validate(req2))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("patientRefId"));
        }

        @Test
        @DisplayName("Oversized patientRefId (> 50 chars) fails validation")
        void testPatientRefId_Oversized_Fails() {
            CreateTestResultRequest req = buildValidRequest();
            req.setPatientRefId("P".repeat(51));
            assertThat(validator.validate(req))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("patientRefId")
                            && v.getMessage().contains("must not exceed 50 characters"));
        }

        @Test
        @DisplayName("Null or empty parameters list fails validation")
        void testParameters_NullOrEmpty_Fails() {
            CreateTestResultRequest req1 = buildValidRequest();
            req1.setParameters(null);
            assertThat(validator.validate(req1))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("parameters"));

            CreateTestResultRequest req2 = buildValidRequest();
            req2.setParameters(Collections.emptyList());
            assertThat(validator.validate(req2))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("parameters"));
        }

        @Test
        @DisplayName("Parameters list exceeding 100 items fails validation")
        void testParameters_OversizedList_Fails() {
            List<TestParameterResultInput> list = new ArrayList<>();
            for (int i = 0; i < 101; i++) {
                list.add(new TestParameterResultInput("PARAM-" + i, "P" + i, "10"));
            }
            CreateTestResultRequest req = buildValidRequest();
            req.setParameters(list);
            assertThat(validator.validate(req))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("parameters")
                            && v.getMessage().contains("Cannot submit more than 100 parameters"));
        }

        @Test
        @DisplayName("Nested input validation: invalid item in parameters triggers violation")
        void testNestedParameterValidation_Fails() {
            CreateTestResultRequest req = buildValidRequest();
            req.setParameters(List.of(
                    new TestParameterResultInput("X".repeat(35), "HGB", "12.0")
            ));
            Set<ConstraintViolation<CreateTestResultRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("parameters[0].parameterRefId"));
        }
    }

    @Nested
    @DisplayName("TestParameterResultInput Field Constraints")
    class ParameterInputValidation {

        @Test
        @DisplayName("Oversized parameterRefId (> 30) fails validation")
        void testParameterRefId_Oversized_Fails() {
            TestParameterResultInput input = new TestParameterResultInput("P".repeat(31), "HGB", "10");
            Set<ConstraintViolation<TestParameterResultInput>> violations = validator.validate(input);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("parameterRefId"));
        }

        @Test
        @DisplayName("Oversized parameterCode (> 50) fails validation")
        void testParameterCode_Oversized_Fails() {
            TestParameterResultInput input = new TestParameterResultInput("PARAM-1", "C".repeat(51), "10");
            Set<ConstraintViolation<TestParameterResultInput>> violations = validator.validate(input);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("parameterCode"));
        }

        @Test
        @DisplayName("Oversized value (> 500) fails validation")
        void testValue_Oversized_Fails() {
            TestParameterResultInput input = new TestParameterResultInput("PARAM-1", "HGB", "V".repeat(501));
            Set<ConstraintViolation<TestParameterResultInput>> violations = validator.validate(input);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("value"));
        }

        @Test
        @DisplayName("Valid parameter input passes with zero violations")
        void testValidParameterInput_Passes() {
            TestParameterResultInput input = new TestParameterResultInput("PARAM-12345", "HGB", "14.5");
            Set<ConstraintViolation<TestParameterResultInput>> violations = validator.validate(input);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Response DTO Security and Serialization Tests")
    class ResponseDtoSecurityAndSerialization {

        @Test
        @DisplayName("TestResultResponse and TestParameterResultResponse serialize and deserialize cleanly")
        void testResponseSerialization() throws Exception {
            Instant now = Instant.now();
            TestParameterResultResponse paramResponse = TestParameterResultResponse.builder()
                    .refId("TPR-abc12345")
                    .parameterRefId("PARAM-xyz890")
                    .parameterCode("MCV")
                    .parameterName("Mean Corpuscular Volume")
                    .dataType(TestParameterDataType.DECIMAL)
                    .inputType(ParameterInputType.CALCULATED)
                    .calculationType(CalculationType.MCV)
                    .calculationVersion("1.0.0")
                    .unit("fL")
                    .value("90.0")
                    .numericValue(new BigDecimal("90.0000"))
                    .flag(ResultFlag.NORMAL)
                    .referenceMin(new BigDecimal("80.0"))
                    .referenceMax(new BigDecimal("100.0"))
                    .criticalLow(new BigDecimal("60.0"))
                    .criticalHigh(new BigDecimal("120.0"))
                    .displayOrder(4)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            TestResultResponse resultResponse = TestResultResponse.builder()
                    .refId("PTR-main999")
                    .organizationRefId("ORG-citylab")
                    .organizationName("City Diagnostic Centre")
                    .testRefId("TEST-cbc")
                    .testCode("CBC")
                    .testName("Complete Blood Count")
                    .patientRefId("PAT-1001")
                    .status(PatientTestResultStatus.CALCULATED)
                    .resultVersion(1)
                    .performedAt(now)
                    .finalizedAt(null)
                    .createdAt(now)
                    .updatedAt(now)
                    .parameters(List.of(paramResponse))
                    .build();

            String json = objectMapper.writeValueAsString(resultResponse);
            assertThat(json).contains("PTR-main999");
            assertThat(json).contains("ORG-citylab");
            assertThat(json).contains("TEST-cbc");
            assertThat(json).contains("TPR-abc12345");
            assertThat(json).contains("CALCULATED");
            assertThat(json).doesNotContain("id\":"); // Ensures no internal UUID leakage

            TestResultResponse deserialized = objectMapper.readValue(json, TestResultResponse.class);
            assertThat(deserialized.getRefId()).isEqualTo("PTR-main999");
            assertThat(deserialized.getParameters()).hasSize(1);
            assertThat(deserialized.getParameters().get(0).getCalculationType()).isEqualTo(CalculationType.MCV);
            assertThat(deserialized.getParameters().get(0).getFlag()).isEqualTo(ResultFlag.NORMAL);
        }

        @Test
        @DisplayName("Ensure request DTO does not allow client injection of server-controlled fields")
        void testRequestDto_CannotAcceptServerControlledFields() {
            List<String> declaredFieldNames = List.of(CreateTestResultRequest.class.getDeclaredFields())
                    .stream()
                    .map(java.lang.reflect.Field::getName)
                    .toList();

            assertThat(declaredFieldNames)
                    .doesNotContain("organizationId", "organizationRefId", "id", "refId", "status",
                            "resultVersion", "finalizedAt", "createdAt", "updatedAt");

            List<String> inputDeclaredFieldNames = List.of(TestParameterResultInput.class.getDeclaredFields())
                    .stream()
                    .map(java.lang.reflect.Field::getName)
                    .toList();

            assertThat(inputDeclaredFieldNames)
                    .doesNotContain("numericValue", "flag", "calculationType", "calculationVersion",
                            "inputType", "dataType", "referenceMin", "referenceMax",
                            "criticalLow", "criticalHigh", "displayOrder", "id", "refId");
        }
    }
}
package com.swasthai.report_generator.test.service.impl;

import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.dto.request.CreateTestResultRequest;
import com.swasthai.report_generator.test.dto.request.TestParameterResultInput;
import com.swasthai.report_generator.test.dto.response.TestParameterResultResponse;
import com.swasthai.report_generator.test.dto.response.TestResultResponse;



import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.repository.PatientTestResultRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestParameterResultRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.test.service.OrganizationTestService;
import com.swasthai.report_generator.test.service.TestResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TestResultServiceImpl implements TestResultService {

    private final PatientTestResultRepository patientTestResultRepository;
    private final TestParameterResultRepository testParameterResultRepository;
    private final TestRepository testRepository;
    private final TestParameterRepository testParameterRepository;
    private final OrganizationTestService organizationTestService;
    private final CalculationEngine calculationEngine;

    @Override
    public TestResultResponse createResult(
            CreateTestResultRequest request
    ) {
        User currentUser = getCurrentUser();

        validateRequest(request);

        Test test = testRepository.findByRefId(request.getTestRefId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Test not found")
                );

        validateTestAccess(currentUser, test);

        Organization organization = getRequiredOrganization(currentUser);

        List<TestParameter> parameters =
                testParameterRepository
                        .findAllByTest_IdAndStatusOrderByDisplayOrderAsc(
                                test.getId(),
                                TestParameterStatus.ACTIVE
                        );

        if (parameters.isEmpty()) {
            throw new IllegalArgumentException(
                    "Test has no active parameters"
            );
        }

        Map<String, TestParameterResultInput> inputs =
                validateAndIndexInputs(request, parameters);

        validateRequiredParameters(parameters, inputs);

        Map<String, BigDecimal> numericValues =
                extractManualNumericValues(parameters, inputs);

        PatientTestResult result = PatientTestResult.builder()
                .organization(organization)
                .test(test)
                .patientRefId(normalize(request.getPatientRefId()))
                .status(PatientTestResultStatus.DRAFT)
                .resultVersion(1)
                .build();

        for (TestParameter parameter : parameters) {

            TestParameterResult parameterResult =
                    buildParameterResult(
                            parameter,
                            inputs,
                            numericValues
                    );

            result.addParameterResult(parameterResult);
        }

        result.setStatus(PatientTestResultStatus.CALCULATED);

        PatientTestResult saved =
                patientTestResultRepository.save(result);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResultResponse getResult(String resultRefId) {

        User currentUser = getCurrentUser();

        if (resultRefId == null || resultRefId.isBlank()) {
            throw new IllegalArgumentException(
                    "Result reference ID is required"
            );
        }

        PatientTestResult result;

        if (isSuperAdmin(currentUser)) {

            result = patientTestResultRepository
                    .findByRefId(resultRefId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Test result not found"
                            )
                    );

        } else {

            Organization organization =
                    getRequiredOrganization(currentUser);

            result = patientTestResultRepository
                    .findByRefIdAndOrganization_Id(
                            resultRefId,
                            organization.getId()
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Test result not found"
                            )
                    );
        }

        return mapToResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestResultResponse> getMyOrganizationResults(
            PatientTestResultStatus status,
            Pageable pageable
    ) {
        User currentUser = getCurrentUser();

        Organization organization =
                getRequiredOrganization(currentUser);

        Page<PatientTestResult> page;

        if (status == null) {

            page = patientTestResultRepository
                    .findAllByOrganization_Id(
                            organization.getId(),
                            pageable
                    );

        } else {

            page = patientTestResultRepository
                    .findAllByOrganization_IdAndStatus(
                            organization.getId(),
                            status,
                            pageable
                    );
        }

        return page.map(this::mapToResponse);
    }

    @Override
    public TestResultResponse finalizeResult(
            String resultRefId
    ) {
        User currentUser = getCurrentUser();

        PatientTestResult result =
                findResultForCurrentUser(
                        resultRefId,
                        currentUser
                );

        if (result.getStatus() == PatientTestResultStatus.FINALIZED) {
            throw new IllegalStateException(
                    "Test result is already finalized"
            );
        }

        if (result.getStatus() != PatientTestResultStatus.CALCULATED) {
            throw new IllegalStateException(
                    "Only calculated results can be finalized"
            );
        }

        Instant now = Instant.now();

        result.setStatus(PatientTestResultStatus.FINALIZED);
        result.setFinalizedAt(now);

        PatientTestResult saved =
                patientTestResultRepository.save(result);

        return mapToResponse(saved);
    }

    private TestParameterResult buildParameterResult(
            TestParameter parameter,
            Map<String, TestParameterResultInput> inputs,
            Map<String, BigDecimal> numericValues
    ) {

        if (parameter.getInputType() == ParameterInputType.CALCULATED) {

            CalculationType calculationType =
                    parameter.getCalculationType();

            if (calculationType == null ||
                    calculationType == CalculationType.NONE) {

                throw new CalculationException(
                        "Invalid calculation configuration for parameter: "
                                + parameter.getCode()
                );
            }

            if (!calculationEngine.isSupported(calculationType)) {
                throw new CalculationException(
                        "Unsupported calculation type: "
                                + calculationType
                );
            }

            if (!calculationEngine.isResultDataTypeSupported(
                    calculationType,
                    parameter.getDataType()
            )) {
                throw new CalculationException(
                        "Calculation result data type is not supported"
                );
            }

            Set<String> required =
                    calculationEngine.getRequiredParameters(
                            calculationType
                    );

            Map<String, BigDecimal> calculationValues =
                    new HashMap<>();

            for (String requiredParameter : required) {

                BigDecimal value =
                        numericValues.get(requiredParameter);

                if (value == null) {
                    throw new CalculationException(
                            "Required parameter value is missing: "
                                    + requiredParameter
                    );
                }

                calculationValues.put(
                        requiredParameter,
                        value
                );
            }

            BigDecimal calculatedValue =
                    calculationEngine.calculate(
                            calculationType,
                            calculationValues
                    );

            return createParameterResult(
                    parameter,
                    calculatedValue.toPlainString(),
                    calculatedValue,
                    calculationType.name() + ":v1"
            );
        }

        TestParameterResultInput input =
                inputs.get(parameter.getRefId());

        if (input == null) {
            throw new IllegalArgumentException(
                    "Required parameter value is missing: "
                            + parameter.getCode()
            );
        }

        String value = normalize(input.value());

        BigDecimal numericValue =
                parseNumericValueIfRequired(
                        parameter,
                        value
                );

        ResultFlag flag =
                calculateFlag(
                        parameter,
                        numericValue
                );

        return createParameterResult(
                parameter,
                value,
                numericValue,
                null,
                flag
        );
    }

    private TestParameterResult createParameterResult(
            TestParameter parameter,
            String value,
            BigDecimal numericValue,
            String calculationVersion
    ) {
        return createParameterResult(
                parameter,
                value,
                numericValue,
                calculationVersion,
                calculateFlag(parameter, numericValue)
        );
    }

    private TestParameterResult createParameterResult(
            TestParameter parameter,
            String value,
            BigDecimal numericValue,
            String calculationVersion,
            ResultFlag flag
    ) {
        return TestParameterResult.builder()
                .testParameter(parameter)
                .value(value)
                .numericValue(numericValue)
                .flag(flag)
                .parameterCode(parameter.getCode())
                .parameterName(parameter.getName())
                .dataType(parameter.getDataType())
                .inputType(parameter.getInputType())
                .calculationType(
                        parameter.getCalculationType() == null
                                ? CalculationType.NONE
                                : parameter.getCalculationType()
                )
                .calculationVersion(calculationVersion)
                .unit(parameter.getUnit())
                .referenceMin(parameter.getReferenceMin())
                .referenceMax(parameter.getReferenceMax())
                .criticalLow(parameter.getCriticalLow())
                .criticalHigh(parameter.getCriticalHigh())
                .displayOrder(parameter.getDisplayOrder())
                .build();
    }

    private Map<String, TestParameterResultInput> validateAndIndexInputs(
            CreateTestResultRequest request,
            List<TestParameter> parameters
    ) {

        Map<String, TestParameter> parametersByRefId =
                parameters.stream()
                        .collect(Collectors.toMap(
                                TestParameter::getRefId,
                                Function.identity()
                        ));

        Map<String, TestParameterResultInput> inputs =
                new HashMap<>();

        for (TestParameterResultInput input :
                request.getParameters()) {

            if (input == null) {
                throw new IllegalArgumentException(
                        "Parameter input cannot be null"
                );
            }

            String parameterRefId =
                    normalize(input.parameterRefId());

            if (parameterRefId == null) {
                throw new IllegalArgumentException(
                        "Parameter reference ID is required"
                );
            }

            if (inputs.put(
                    parameterRefId,
                    input
            ) != null) {
                throw new IllegalArgumentException(
                        "Duplicate parameter submitted: "
                                + parameterRefId
                );
            }

            TestParameter parameter =
                    parametersByRefId.get(parameterRefId);

            if (parameter == null) {
                throw new IllegalArgumentException(
                        "Parameter does not belong to this test: "
                                + parameterRefId
                );
            }

            if (parameter.getInputType() ==
                    ParameterInputType.CALCULATED) {

                throw new IllegalArgumentException(
                        "Calculated parameter cannot be submitted: "
                                + parameter.getCode()
                );
            }
        }

        return inputs;
    }

    private void validateRequiredParameters(
            List<TestParameter> parameters,
            Map<String, TestParameterResultInput> inputs
    ) {
        for (TestParameter parameter : parameters) {

            if (!parameter.isRequired()) {
                continue;
            }

            if (parameter.getInputType() ==
                    ParameterInputType.CALCULATED) {
                continue;
            }

            if (!inputs.containsKey(parameter.getRefId())) {
                throw new IllegalArgumentException(
                        "Required parameter is missing: "
                                + parameter.getCode()
                );
            }
        }
    }

    private Map<String, BigDecimal> extractManualNumericValues(
            List<TestParameter> parameters,
            Map<String, TestParameterResultInput> inputs
    ) {

        Map<String, BigDecimal> values = new HashMap<>();

        for (TestParameter parameter : parameters) {

            if (parameter.getInputType() !=
                    ParameterInputType.MANUAL) {
                continue;
            }

            TestParameterResultInput input =
                    inputs.get(parameter.getRefId());

            if (input == null) {
                continue;
            }

            if (!isNumericDataType(parameter.getDataType())) {
                continue;
            }

            BigDecimal numeric =
                    parseNumericValueIfRequired(
                            parameter,
                            normalize(input.value())
                    );

            values.put(parameter.getCode(), numeric);
        }

        return values;
    }

    private BigDecimal parseNumericValueIfRequired(
            TestParameter parameter,
            String value
    ) {

        if (!isNumericDataType(parameter.getDataType())) {
            return null;
        }

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Numeric value is required for parameter: "
                            + parameter.getCode()
            );
        }

        if (value.contains("e") ||
                value.contains("E")) {
            throw new IllegalArgumentException(
                    "Scientific notation is not allowed for parameter: "
                            + parameter.getCode()
            );
        }

        final BigDecimal numeric;

        try {
            numeric = new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Invalid numeric value for parameter: "
                            + parameter.getCode()
            );
        }

        if (numeric.scale() > 6) {
            throw new IllegalArgumentException(
                    "Numeric value has too many decimal places for parameter: "
                            + parameter.getCode()
            );
        }

        return numeric;
    }

    private ResultFlag calculateFlag(
            TestParameter parameter,
            BigDecimal value
    ) {

        if (value == null) {
            return null;
        }

        BigDecimal criticalLow =
                parameter.getCriticalLow();

        BigDecimal referenceMin =
                parameter.getReferenceMin();

        BigDecimal referenceMax =
                parameter.getReferenceMax();

        BigDecimal criticalHigh =
                parameter.getCriticalHigh();

        if (criticalLow != null &&
                value.compareTo(criticalLow) < 0) {
            return ResultFlag.CRITICAL_LOW;
        }

        if (criticalHigh != null &&
                value.compareTo(criticalHigh) > 0) {
            return ResultFlag.CRITICAL_HIGH;
        }

        if (referenceMin != null &&
                value.compareTo(referenceMin) < 0) {
            return ResultFlag.LOW;
        }

        if (referenceMax != null &&
                value.compareTo(referenceMax) > 0) {
            return ResultFlag.HIGH;
        }

        return ResultFlag.NORMAL;
    }

    private void validateTestAccess(
            User user,
            Test test
    ) {

        if (test.getStatus() != TestStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Test is not active"
            );
        }

        if (isSuperAdmin(user)) {
            return;
        }

        if (user.getOrganization() == null) {
            throw new IllegalStateException(
                    "User organization is required"
            );
        }

        if (!organizationTestService.hasTestAccess(
                test.getRefId()
        )) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this test"
            );
        }
    }

    private PatientTestResult findResultForCurrentUser(
            String resultRefId,
            User user
    ) {

        if (resultRefId == null ||
                resultRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Result reference ID is required"
            );
        }

        if (isSuperAdmin(user)) {

            return patientTestResultRepository
                    .findByRefId(resultRefId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Test result not found"
                            )
                    );
        }

        Organization organization =
                getRequiredOrganization(user);

        return patientTestResultRepository
                .findByRefIdAndOrganization_Id(
                        resultRefId,
                        organization.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Test result not found"
                        )
                );
    }

    private Organization getRequiredOrganization(
            User user
    ) {

        if (user == null ||
                user.getOrganization() == null) {

            throw new IllegalStateException(
                    "User organization is required"
            );
        }

        return user.getOrganization();
    }

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new org.springframework.security.access.AccessDeniedException(
                    "Authentication is required"
            );
        }

        Object details =
                authentication.getDetails();

        if (!(details instanceof User user)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Authenticated user context is invalid"
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User is inactive"
            );
        }

        return user;
    }

    private boolean isSuperAdmin(User user) {
        return user.getRole() == Role.SUPER_ADMIN;
    }

    private void validateRequest(
            CreateTestResultRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }

        if (request.getTestRefId() == null ||
                request.getTestRefId().isBlank()) {

            throw new IllegalArgumentException(
                    "Test reference ID is required"
            );
        }

        if (request.getPatientRefId() == null ||
                request.getPatientRefId().isBlank()) {

            throw new IllegalArgumentException(
                    "Patient reference ID is required"
            );
        }

        if (request.getPatientRefId().length() > 50) {
            throw new IllegalArgumentException(
                    "Patient reference ID is too long"
            );
        }

        if (request.getParameters() == null ||
                request.getParameters().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one parameter value is required"
            );
        }

        if (request.getParameters().size() > 500) {
            throw new IllegalArgumentException(
                    "Too many parameters submitted"
            );
        }
    }

    private boolean isNumericDataType(
            TestParameterDataType dataType
    ) {
        return dataType == TestParameterDataType.INTEGER ||
                dataType == TestParameterDataType.DECIMAL;
    }

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private TestResultResponse mapToResponse(
            PatientTestResult result
    ) {

        List<TestParameterResultResponse> parameterResponses =
                result.getParameterResults()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        TestParameterResult::getDisplayOrder
                                )
                        )
                        .map(this::mapParameterResponse)
                        .toList();

        return TestResultResponse.builder()
                .refId(result.getRefId())
                .organizationRefId(
                        result.getOrganization().getRefId()
                )
                .organizationName(
                        result.getOrganization().getName()
                )
                .testRefId(
                        result.getTest().getRefId()
                )
                .testCode(
                        result.getTest().getCode()
                )
                .testName(
                        result.getTest().getName()
                )
                .patientRefId(result.getPatientRefId())
                .status(result.getStatus())
                .resultVersion(result.getResultVersion())
                .performedAt(result.getPerformedAt())
                .finalizedAt(result.getFinalizedAt())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .parameters(parameterResponses)
                .build();
    }

    private TestParameterResultResponse mapParameterResponse(
            TestParameterResult result
    ) {

        return TestParameterResultResponse.builder()
                .refId(result.getRefId())
                .parameterRefId(
                        result.getTestParameter().getRefId()
                )
                .parameterCode(result.getParameterCode())
                .parameterName(result.getParameterName())
                .dataType(result.getDataType())
                .inputType(result.getInputType())
                .calculationType(result.getCalculationType())
                .calculationVersion(
                        result.getCalculationVersion()
                )
                .value(result.getValue())
                .numericValue(result.getNumericValue())
                .flag(result.getFlag())
                .unit(result.getUnit())
                .referenceMin(result.getReferenceMin())
                .referenceMax(result.getReferenceMax())
                .criticalLow(result.getCriticalLow())
                .criticalHigh(result.getCriticalHigh())
                .displayOrder(result.getDisplayOrder())
                .build();
    }
}
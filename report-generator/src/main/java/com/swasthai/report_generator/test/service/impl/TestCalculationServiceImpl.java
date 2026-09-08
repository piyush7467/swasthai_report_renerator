package com.swasthai.report_generator.test.service.impl;

import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.dto.request.CalculateTestRequest;
import com.swasthai.report_generator.test.dto.request.ParameterValueInput;
import com.swasthai.report_generator.test.dto.response.CalculatedParameterItemResponse;
import com.swasthai.report_generator.test.dto.response.CalculatedTestResponse;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.test.service.OrganizationTestService;
import com.swasthai.report_generator.test.service.TestCalculationService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TestCalculationServiceImpl implements TestCalculationService {

    private final TestRepository testRepository;
    private final TestParameterRepository testParameterRepository;
    private final OrganizationTestService organizationTestService;
    private final CalculationEngine calculationEngine;

    @Override
    @Transactional(readOnly = true)
    public CalculatedTestResponse calculateParameters(
            String testRefId,
            CalculateTestRequest request
    ) {
        if (testRefId == null || testRefId.isBlank()) {
            throw new IllegalArgumentException("Test reference ID is required");
        }

        if (request == null || request.parameters() == null || request.parameters().isEmpty()) {
            throw new IllegalArgumentException("Parameter values list cannot be empty");
        }

        // 1. Authenticate & Resolve Current User
        User currentUser = getCurrentUser();
        validateUserAndTenantAccess(currentUser, testRefId);

        // 2. Load and validate Test
        Test test = testRepository.findByRefId(testRefId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with refId: " + testRefId));

        if (test.getStatus() != TestStatus.ACTIVE) {
            throw new IllegalArgumentException("Test is not active: " + testRefId);
        }

        // 3. Load all active parameters for this test
        List<TestParameter> allActiveParameters =
                testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(
                        test.getId(),
                        TestParameterStatus.ACTIVE
                );

        Map<String, TestParameter> paramByCode = new HashMap<>();
        Map<String, TestParameter> paramByRefId = new HashMap<>();
        for (TestParameter p : allActiveParameters) {
            paramByCode.put(p.getCode(), p);
            paramByRefId.put(p.getRefId(), p);
        }

        // 4. Validate request inputs and check for attacks
        Set<String> seenCodes = new HashSet<>();
        Map<String, String> rawInputByCode = new HashMap<>();

        for (ParameterValueInput input : request.parameters()) {
            if (input == null) {
                continue;
            }

            TestParameter matchedParam = resolveParameter(input, paramByCode, paramByRefId);

            String code = matchedParam.getCode();
            if (seenCodes.contains(code)) {
                throw new IllegalArgumentException("Duplicate parameter submission for: " + code);
            }
            seenCodes.add(code);

            // Reject any attempt to manually submit a value for a CALCULATED parameter
            if (matchedParam.getInputType() == ParameterInputType.CALCULATED) {
                if (input.value() != null && !input.value().isBlank()) {
                    throw new IllegalArgumentException(
                            "Calculated parameters cannot be manually provided: " + code
                    );
                }
            } else {
                rawInputByCode.put(code, input.value());
            }
        }

        // 5. Parse manual parameters and enforce requirements
        Map<String, BigDecimal> numericValues = new HashMap<>();
        Map<String, String> stringValues = new HashMap<>();

        for (TestParameter param : allActiveParameters) {
            if (param.getInputType() == ParameterInputType.MANUAL) {
                String rawValue = rawInputByCode.get(param.getCode());

                if (rawValue == null || rawValue.isBlank()) {
                    if (param.isRequired()) {
                        throw new IllegalArgumentException(
                                "Missing required parameter: " + param.getCode()
                        );
                    }
                } else {
                    String trimmed = rawValue.trim();
                    stringValues.put(param.getCode(), trimmed);

                    if (param.getDataType() == TestParameterDataType.DECIMAL
                            || param.getDataType() == TestParameterDataType.INTEGER) {
                        BigDecimal parsedNumber = parseStrictNumeric(param.getCode(), trimmed);
                        numericValues.put(param.getCode(), parsedNumber);
                    }
                }
            }
        }

        // 6. Perform backend calculations for all CALCULATED parameters
        for (TestParameter param : allActiveParameters) {
            if (param.getInputType() == ParameterInputType.CALCULATED) {
                CalculationType calcType = param.getCalculationType();

                if (calcType == null || calcType == CalculationType.NONE) {
                    throw new CalculationException(
                            "Calculated parameter " + param.getCode() + " has invalid calculation type: " + calcType
                    );
                }

                if (!calculationEngine.isSupported(calcType)) {
                    throw new CalculationException(
                            "No calculator registered for: " + calcType
                    );
                }

                // Verify dependencies belong to active test parameters and have values
                Set<String> requiredDeps = calculationEngine.getRequiredParameters(calcType);
                for (String depCode : requiredDeps) {
                    TestParameter depParam = paramByCode.get(depCode);
                    if (depParam == null) {
                        throw new CalculationException(
                                "Calculation dependency " + depCode + " is not an active parameter of this test"
                        );
                    }
                    if (!numericValues.containsKey(depCode) || numericValues.get(depCode) == null) {
                        throw new CalculationException(
                                "Required parameter is missing: " + depCode
                        );
                    }
                }

                // Calculate deterministically via CalculationEngine
                BigDecimal result = calculationEngine.calculate(calcType, numericValues);
                numericValues.put(param.getCode(), result);
                stringValues.put(param.getCode(), result.stripTrailingZeros().toPlainString());
            }
        }

        // 7. Classify ranges and build response items
        List<CalculatedParameterItemResponse> itemResponses = new ArrayList<>();
        for (TestParameter param : allActiveParameters) {
            BigDecimal numVal = numericValues.get(param.getCode());
            String strVal = stringValues.get(param.getCode());
            ResultFlag flag = classifyFlag(param, numVal);

            itemResponses.add(new CalculatedParameterItemResponse(
                    param.getRefId(),
                    param.getCode(),
                    param.getName(),
                    param.getInputType(),
                    param.getCalculationType(),
                    param.getDataType(),
                    param.getUnit(),
                    strVal,
                    numVal,
                    flag,
                    param.getReferenceMin(),
                    param.getReferenceMax(),
                    param.getCriticalLow(),
                    param.getCriticalHigh(),
                    param.getDisplayOrder()
            ));
        }

        return new CalculatedTestResponse(
                test.getRefId(),
                test.getCode(),
                test.getName(),
                itemResponses
        );
    }

    private TestParameter resolveParameter(
            ParameterValueInput input,
            Map<String, TestParameter> paramByCode,
            Map<String, TestParameter> paramByRefId
    ) {
        String code = input.parameterCode();
        String refId = input.parameterRefId();

        if ((code == null || code.isBlank()) && (refId == null || refId.isBlank())) {
            throw new IllegalArgumentException("Parameter identifier (code or refId) is required");
        }

        if (code != null && !code.isBlank()) {
            String normCode = code.trim().toUpperCase();
            TestParameter param = paramByCode.get(normCode);
            if (param == null) {
                // Check if parameter exists globally in repository to detect cross-test attack
                boolean existsByCode = testParameterRepository.findAll().stream()
                        .anyMatch(p -> normCode.equalsIgnoreCase(p.getCode()));
                if (existsByCode) {
                    throw new IllegalArgumentException("Parameter does not belong to this test: " + code);
                }
                throw new IllegalArgumentException("Unknown parameter code for test: " + code);
            }
            if (refId != null && !refId.isBlank() && !param.getRefId().equalsIgnoreCase(refId.trim())) {
                throw new IllegalArgumentException("Parameter refId does not match code: " + refId);
            }
            return param;
        } else {
            String normRefId = refId.trim();
            TestParameter param = paramByRefId.get(normRefId);
            if (param == null) {
                if (testParameterRepository.existsByRefId(normRefId)) {
                    throw new IllegalArgumentException("Parameter does not belong to this test: " + refId);
                }
                throw new IllegalArgumentException("Unknown parameter refId for test: " + refId);
            }
            return param;
        }
    }

    private BigDecimal parseStrictNumeric(String paramCode, String value) {
        if (value.toLowerCase().contains("e")
                || value.equalsIgnoreCase("nan")
                || value.toLowerCase().contains("infinity")) {
            throw new IllegalArgumentException("Invalid numeric format for parameter: " + paramCode);
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric value for parameter " + paramCode + ": " + value);
        }
    }

    private ResultFlag classifyFlag(TestParameter param, BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (param.getCriticalLow() != null && value.compareTo(param.getCriticalLow()) < 0) {
            return ResultFlag.CRITICAL_LOW;
        }
        if (param.getCriticalHigh() != null && value.compareTo(param.getCriticalHigh()) > 0) {
            return ResultFlag.CRITICAL_HIGH;
        }
        if (param.getReferenceMin() != null && value.compareTo(param.getReferenceMin()) < 0) {
            return ResultFlag.LOW;
        }
        if (param.getReferenceMax() != null && value.compareTo(param.getReferenceMax()) > 0) {
            return ResultFlag.HIGH;
        }
        return ResultFlag.NORMAL;
    }

    private void validateUserAndTenantAccess(User currentUser, String testRefId) {
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User account is inactive");
        }

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        Organization organization = currentUser.getOrganization();
        if (organization == null || organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new AccessDeniedException("User organization is inactive or not found");
        }

        if (!organizationTestService.hasTestAccess(testRefId)) {
            throw new AccessDeniedException("Organization does not have active access to test: " + testRefId);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Object details = authentication.getDetails();
        if (!(details instanceof User user)) {
            throw new AccessDeniedException("Authenticated user information is unavailable");
        }

        return user;
    }
}

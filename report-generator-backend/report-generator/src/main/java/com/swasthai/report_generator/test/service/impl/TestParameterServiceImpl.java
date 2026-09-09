package com.swasthai.report_generator.test.service.impl;

import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.dto.request.CreateTestParameterRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestParameterRequest;
import com.swasthai.report_generator.test.dto.response.TestParameterResponse;
import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.Test;
import com.swasthai.report_generator.test.entity.TestParameter;
import com.swasthai.report_generator.test.entity.TestParameterDataType;
import com.swasthai.report_generator.test.entity.TestParameterStatus;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.test.service.OrganizationTestService;
import com.swasthai.report_generator.test.service.TestParameterService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TestParameterServiceImpl implements TestParameterService {

    private static final int MAX_BULK_SIZE = 100;

    private final TestParameterRepository testParameterRepository;
    private final TestRepository testRepository;
    private final OrganizationTestService organizationTestService;
    private final CalculationEngine calculationEngine;

    // ============================================================
    // CREATE SINGLE PARAMETER
    // ============================================================

    @Override
    public TestParameterResponse createParameter(
            String testRefId,
            CreateTestParameterRequest request
    ) {

        requireSuperAdmin();

        Test test = getActiveTest(testRefId);

        validateCreateRequest(request);

        String code = normalizeCode(request.code());

        if (testParameterRepository.existsByTest_IdAndCode(
                test.getId(),
                code
        )) {
            throw new IllegalArgumentException(
                    "Parameter code already exists for this test"
            );
        }

        TestParameter parameter = TestParameter.builder()
                .test(test)
                .code(code)
                .name(normalizeRequired(
                        request.name(),
                        "Parameter name"
                ))
                .description(
                        normalize(request.description())
                )
                .dataType(request.dataType())
                .inputType(request.inputType())
                .calculationType(
                        request.calculationType() != null
                                ? request.calculationType()
                                : CalculationType.NONE
                )
                .unit(normalize(request.unit()))
                .required(
                        request.required() == null
                                || request.required()
                )
                .displayOrder(request.displayOrder())
                .referenceMin(request.referenceMin())
                .referenceMax(request.referenceMax())
                .criticalLow(request.criticalLow())
                .criticalHigh(request.criticalHigh())
                .reportDescription(
                        normalize(request.reportDescription())
                )
                .interpretationGuidance(
                        normalize(request.interpretationGuidance())
                )
                .status(TestParameterStatus.ACTIVE)
                .version(1)
                .build();

        try {

            parameter =
                    testParameterRepository.save(parameter);

        } catch (DataIntegrityViolationException ex) {

            throw new IllegalArgumentException(
                    "Parameter code already exists for this test"
            );
        }

        return mapToResponse(parameter);
    }

    // ============================================================
    // BULK CREATE
    // ============================================================

    @Override
    public List<TestParameterResponse> createParametersBulk(
            String testRefId,
            List<CreateTestParameterRequest> requests
    ) {

        requireSuperAdmin();

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one parameter is required"
            );
        }

        if (requests.size() > MAX_BULK_SIZE) {
            throw new IllegalArgumentException(
                    "Bulk parameter creation cannot exceed "
                            + MAX_BULK_SIZE
                            + " parameters"
            );
        }

        Test test = getActiveTest(testRefId);

        /*
         * Validate the complete request before saving anything.
         */
        Set<String> batchCodes = new HashSet<>();

        for (CreateTestParameterRequest request : requests) {

            if (request == null) {
                throw new IllegalArgumentException(
                        "Parameter request cannot be null"
                );
            }

            validateCreateRequest(request);

            String code =
                    normalizeCode(request.code());

            if (!batchCodes.add(code)) {
                throw new IllegalArgumentException(
                        "Duplicate parameter code in request: "
                                + code
                );
            }

            if (testParameterRepository.existsByTest_IdAndCode(
                    test.getId(),
                    code
            )) {
                throw new IllegalArgumentException(
                        "Parameter code already exists for this test: "
                                + code
                );
            }
        }

        List<TestParameter> parameters =
                requests.stream()
                        .map(request ->
                                TestParameter.builder()
                                        .test(test)
                                        .code(
                                                normalizeCode(
                                                        request.code()
                                                )
                                        )
                                        .name(
                                                normalizeRequired(
                                                        request.name(),
                                                        "Parameter name"
                                                )
                                        )
                                        .description(
                                                normalize(
                                                        request.description()
                                                )
                                        )
                                        .dataType(
                                                request.dataType()
                                        )
                                        .inputType(
                                                request.inputType()
                                        )
                                        .calculationType(
                                                request.calculationType() != null
                                                        ? request.calculationType()
                                                        : CalculationType.NONE
                                        )
                                        .unit(
                                                normalize(
                                                        request.unit()
                                                )
                                        )
                                        .required(
                                                request.required() == null
                                                        || request.required()
                                        )
                                        .displayOrder(
                                                request.displayOrder()
                                        )
                                        .referenceMin(
                                                request.referenceMin()
                                        )
                                        .referenceMax(
                                                request.referenceMax()
                                        )
                                        .criticalLow(
                                                request.criticalLow()
                                        )
                                        .criticalHigh(
                                                request.criticalHigh()
                                        )
                                        .reportDescription(
                                                normalize(
                                                        request.reportDescription()
                                                )
                                        )
                                        .interpretationGuidance(
                                                normalize(
                                                        request.interpretationGuidance()
                                                )
                                        )
                                        .status(
                                                TestParameterStatus.ACTIVE
                                        )
                                        .version(1)
                                        .build()
                        )
                        .toList();

        try {

            List<TestParameter> saved =
                    testParameterRepository.saveAll(parameters);

            return saved.stream()
                    .map(this::mapToResponse)
                    .toList();

        } catch (DataIntegrityViolationException ex) {

            throw new IllegalArgumentException(
                    "One or more parameter codes already exist "
                            + "for this test"
            );
        }
    }

    // ============================================================
    // GET SINGLE PARAMETER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public TestParameterResponse getParameter(
            String parameterRefId
    ) {

        TestParameter parameter =
                findParameter(parameterRefId);

        /*
         * SUPER_ADMIN can access all master parameters.
         *
         * ORG_ADMIN/LAB_STAFF can access only parameters
         * belonging to tests assigned to their organization.
         */
        ensureParameterReadAccess(
                parameter.getTest()
        );

        return mapToResponse(parameter);
    }

    // ============================================================
    // GET PARAMETERS OF TEST
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<TestParameterResponse> getTestParameters(
            String testRefId,
            String status,
            Pageable pageable
    ) {

        Test test = getTest(testRefId);

        /*
         * Important tenant boundary.
         *
         * We do NOT trust testRefId alone.
         */
        ensureParameterReadAccess(test);

        if (status == null || status.isBlank()) {

            return testParameterRepository
                    .findAllByTest_Id(
                            test.getId(),
                            pageable
                    )
                    .map(this::mapToResponse);
        }

        TestParameterStatus parameterStatus =
                parseStatus(status);

        return testParameterRepository
                .findAllByTest_IdAndStatus(
                        test.getId(),
                        parameterStatus,
                        pageable
                )
                .map(this::mapToResponse);
    }

    // ============================================================
    // UPDATE PARAMETER
    // ============================================================

    @Override
    public TestParameterResponse updateParameter(
            String parameterRefId,
            UpdateTestParameterRequest request
    ) {

        requireSuperAdmin();

        if (request == null) {
            throw new IllegalArgumentException(
                    "Update request is required"
            );
        }

        TestParameter parameter =
                findParameter(parameterRefId);

        /*
         * Master Test itself must still be active when
         * modifying its parameter configuration.
         */
        getActiveTest(
                parameter.getTest().getRefId()
        );

        // --------------------------------------------------------
        // CODE
        // --------------------------------------------------------

        if (request.code() != null) {

            String newCode =
                    normalizeCode(request.code());

            if (newCode.isBlank()) {
                throw new IllegalArgumentException(
                        "Parameter code cannot be blank"
                );
            }

            if (!newCode.equals(parameter.getCode())
                    && testParameterRepository
                    .existsByTest_IdAndCodeAndIdNot(
                            parameter.getTest().getId(),
                            newCode,
                            parameter.getId()
                    )) {

                throw new IllegalArgumentException(
                        "Parameter code already exists for this test"
                );
            }

            parameter.setCode(newCode);
        }

        // --------------------------------------------------------
        // NAME
        // --------------------------------------------------------

        if (request.name() != null) {

            parameter.setName(
                    normalizeRequired(
                            request.name(),
                            "Parameter name"
                    )
            );
        }

        // --------------------------------------------------------
        // DESCRIPTION
        // --------------------------------------------------------

        if (request.description() != null) {

            parameter.setDescription(
                    normalize(request.description())
            );
        }

        // --------------------------------------------------------
        // DATA TYPE
        // --------------------------------------------------------

        if (request.dataType() != null) {

            parameter.setDataType(
                    request.dataType()
            );
        }

        // --------------------------------------------------------
        // INPUT TYPE
        // --------------------------------------------------------

        if (request.inputType() != null) {

            parameter.setInputType(
                    request.inputType()
            );
        }

        // --------------------------------------------------------
        // CALCULATION TYPE
        // --------------------------------------------------------

        if (request.calculationType() != null) {

            parameter.setCalculationType(
                    request.calculationType()
            );
        }

        // --------------------------------------------------------
        // UNIT
        // --------------------------------------------------------

        if (request.unit() != null) {

            parameter.setUnit(
                    normalize(request.unit())
            );
        }

        // --------------------------------------------------------
        // REQUIRED
        // --------------------------------------------------------

        if (request.required() != null) {

            parameter.setRequired(
                    request.required()
            );
        }

        // --------------------------------------------------------
        // DISPLAY ORDER
        // --------------------------------------------------------

        if (request.displayOrder() != null) {

            parameter.setDisplayOrder(
                    request.displayOrder()
            );
        }

        // --------------------------------------------------------
        // REFERENCE RANGE
        // --------------------------------------------------------

        if (request.referenceMin() != null) {

            parameter.setReferenceMin(
                    request.referenceMin()
            );
        }

        if (request.referenceMax() != null) {

            parameter.setReferenceMax(
                    request.referenceMax()
            );
        }

        // --------------------------------------------------------
        // CRITICAL RANGE
        // --------------------------------------------------------

        if (request.criticalLow() != null) {

            parameter.setCriticalLow(
                    request.criticalLow()
            );
        }

        if (request.criticalHigh() != null) {

            parameter.setCriticalHigh(
                    request.criticalHigh()
            );
        }

        // --------------------------------------------------------
        // REPORT DESCRIPTION
        // --------------------------------------------------------

        if (request.reportDescription() != null) {

            parameter.setReportDescription(
                    normalize(
                            request.reportDescription()
                    )
            );
        }

        // --------------------------------------------------------
        // INTERPRETATION GUIDANCE
        // --------------------------------------------------------

        if (request.interpretationGuidance() != null) {

            parameter.setInterpretationGuidance(
                    normalize(
                            request.interpretationGuidance()
                    )
            );
        }

        // --------------------------------------------------------
        // STATUS
        // --------------------------------------------------------

        if (request.status() != null) {

            parameter.setStatus(
                    request.status()
            );
        }

        /*
         * Validate the FINAL state after applying the PATCH.
         */
        validateFinalParameter(parameter);

        parameter.setVersion(
                parameter.getVersion() + 1
        );

        try {

            parameter =
                    testParameterRepository.save(parameter);

        } catch (DataIntegrityViolationException ex) {

            throw new IllegalArgumentException(
                    "Parameter code already exists for this test"
            );
        }

        return mapToResponse(parameter);
    }

    // ============================================================
    // SOFT DELETE
    // ============================================================

    @Override
    public void deactivateParameter(
            String parameterRefId
    ) {

        requireSuperAdmin();

        TestParameter parameter =
                findParameter(parameterRefId);

        if (parameter.getStatus()
                == TestParameterStatus.INACTIVE) {

            throw new IllegalStateException(
                    "Parameter is already inactive"
            );
        }

        parameter.setStatus(
                TestParameterStatus.INACTIVE
        );

        parameter.setVersion(
                parameter.getVersion() + 1
        );

        testParameterRepository.save(parameter);
    }

    // ============================================================
    // CREATE REQUEST VALIDATION
    // ============================================================

    private void validateCreateRequest(
            CreateTestParameterRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Parameter request is required"
            );
        }

        String code =
                normalizeCode(request.code());

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Parameter code cannot be blank"
            );
        }

        String name =
                normalize(request.name());

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Parameter name cannot be blank"
            );
        }

        if (request.displayOrder() == null
                || request.displayOrder() < 1) {

            throw new IllegalArgumentException(
                    "Display order must be at least 1"
            );
        }

        validateRanges(
                request.dataType(),
                request.referenceMin(),
                request.referenceMax(),
                request.criticalLow(),
                request.criticalHigh()
        );

        validateCalculationConfiguration(
                request.inputType(),
                request.calculationType(),
                request.dataType()
        );
    }

    // ============================================================
    // FINAL ENTITY VALIDATION
    // ============================================================

    private void validateFinalParameter(
            TestParameter parameter
    ) {

        validateRanges(
                parameter.getDataType(),
                parameter.getReferenceMin(),
                parameter.getReferenceMax(),
                parameter.getCriticalLow(),
                parameter.getCriticalHigh()
        );

        validateCalculationConfiguration(
                parameter.getInputType(),
                parameter.getCalculationType(),
                parameter.getDataType()
        );

        if (parameter.getCode() == null
                || parameter.getCode().isBlank()) {

            throw new IllegalArgumentException(
                    "Parameter code cannot be blank"
            );
        }

        if (parameter.getName() == null
                || parameter.getName().isBlank()) {

            throw new IllegalArgumentException(
                    "Parameter name cannot be blank"
            );
        }

        if (parameter.getDisplayOrder() == null
                || parameter.getDisplayOrder() < 1) {

            throw new IllegalArgumentException(
                    "Display order must be at least 1"
            );
        }
    }

    // ============================================================
    // CALCULATION CONFIGURATION VALIDATION
    // ============================================================

    private void validateCalculationConfiguration(
            ParameterInputType inputType,
            CalculationType calculationType,
            TestParameterDataType dataType
    ) {

        if (inputType == null) {
            throw new IllegalArgumentException(
                    "Input type is required"
            );
        }

        CalculationType effectiveCalcType =
                calculationType != null ? calculationType : CalculationType.NONE;

        if (inputType == ParameterInputType.MANUAL) {

            if (effectiveCalcType != CalculationType.NONE) {

                throw new IllegalArgumentException(
                        "Manual parameter must have calculation type NONE"
                );
            }

        } else if (inputType == ParameterInputType.CALCULATED) {

            if (effectiveCalcType == CalculationType.NONE) {

                throw new IllegalArgumentException(
                        "Calculated parameter must have a valid calculation type"
                );
            }

            if (calculationEngine != null && !calculationEngine.isSupported(effectiveCalcType)) {
                throw new IllegalArgumentException(
                        "Unsupported calculation type: " + effectiveCalcType
                );
            }

            if (dataType != null) {
                if (calculationEngine != null && !calculationEngine.isResultDataTypeSupported(effectiveCalcType, dataType)) {
                    throw new IllegalArgumentException(
                            "Data type " + dataType + " is not supported for calculation type: " + effectiveCalcType
                    );
                } else if (dataType != TestParameterDataType.DECIMAL && dataType != TestParameterDataType.INTEGER) {
                    throw new IllegalArgumentException(
                            "Calculated parameter must have numeric data type"
                    );
                }
            }
        }
    }

    // ============================================================
    // RANGE VALIDATION
    // ============================================================

    private void validateRanges(
            TestParameterDataType dataType,
            BigDecimal referenceMin,
            BigDecimal referenceMax,
            BigDecimal criticalLow,
            BigDecimal criticalHigh
    ) {

        if (dataType == null) {
            throw new IllegalArgumentException(
                    "Data type is required"
            );
        }

        boolean numeric =
                dataType == TestParameterDataType.INTEGER
                        || dataType == TestParameterDataType.DECIMAL;

        /*
         * Numeric ranges make sense only for numeric parameters.
         */
        if (!numeric) {

            if (referenceMin != null
                    || referenceMax != null
                    || criticalLow != null
                    || criticalHigh != null) {

                throw new IllegalArgumentException(
                        "Numeric ranges are allowed only for "
                                + "INTEGER or DECIMAL parameters"
                );
            }

            return;
        }

        // --------------------------------------------------------
        // REFERENCE RANGE
        // --------------------------------------------------------

        if (referenceMin != null
                && referenceMax != null
                && referenceMin.compareTo(referenceMax) > 0) {

            throw new IllegalArgumentException(
                    "Reference minimum cannot be greater than "
                            + "reference maximum"
            );
        }

        // --------------------------------------------------------
        // CRITICAL RANGE
        // --------------------------------------------------------

        if (criticalLow != null
                && criticalHigh != null
                && criticalLow.compareTo(criticalHigh) > 0) {

            throw new IllegalArgumentException(
                    "Critical low cannot be greater than "
                            + "critical high"
            );
        }

        // --------------------------------------------------------
        // CRITICAL LOW VS REFERENCE
        // --------------------------------------------------------

        if (criticalLow != null
                && referenceMin != null
                && criticalLow.compareTo(referenceMin) > 0) {

            throw new IllegalArgumentException(
                    "Critical low should not be greater than "
                            + "reference minimum"
            );
        }

        // --------------------------------------------------------
        // CRITICAL HIGH VS REFERENCE
        // --------------------------------------------------------

        if (criticalHigh != null
                && referenceMax != null
                && criticalHigh.compareTo(referenceMax) < 0) {

            throw new IllegalArgumentException(
                    "Critical high should not be less than "
                            + "reference maximum"
            );
        }
    }

    // ============================================================
    // TEST LOOKUP
    // ============================================================

    private Test getActiveTest(
            String testRefId
    ) {

        Test test = getTest(testRefId);

        if (test.getStatus() == null
                || !"ACTIVE".equals(
                        test.getStatus().name()
                )) {

            throw new IllegalStateException(
                    "Test is inactive"
            );
        }

        return test;
    }

    private Test getTest(
            String testRefId
    ) {

        if (testRefId == null
                || testRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Test refId is required"
            );
        }

        return testRepository
                .findByRefId(testRefId.trim())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Test not found"
                        )
                );
    }

    // ============================================================
    // PARAMETER LOOKUP
    // ============================================================

    private TestParameter findParameter(
            String parameterRefId
    ) {

        if (parameterRefId == null
                || parameterRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Parameter refId is required"
            );
        }

        return testParameterRepository
                .findByRefId(parameterRefId.trim())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Test parameter not found"
                        )
                );
    }

    // ============================================================
    // PARAMETER READ AUTHORIZATION
    // ============================================================

    private void ensureParameterReadAccess(
            Test test
    ) {

        User user = getCurrentUser();

        /*
         * SUPER_ADMIN manages the complete master catalog.
         */
        if (user.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        /*
         * Only organization users can access assigned tests.
         */
        if (user.getRole() != Role.ORG_ADMIN
                && user.getRole() != Role.LAB_STAFF) {

            throw new AccessDeniedException(
                    "You do not have permission to access "
                            + "test parameters"
            );
        }

        /*
         * OrganizationTestService owns the business rule for:
         *
         * current organization
         *       +
         * active organization
         *       +
         * active test
         *       +
         * active assignment
         *       +
         * effective dates
         */
        boolean hasAccess =
                organizationTestService.hasTestAccess(
                        test.getRefId()
                );

        if (!hasAccess) {

            throw new AccessDeniedException(
                    "This test is not available to your organization"
            );
        }
    }

    // ============================================================
    // STATUS
    // ============================================================

    private TestParameterStatus parseStatus(
            String status
    ) {

        try {

            return TestParameterStatus.valueOf(
                    status.trim().toUpperCase()
            );

        } catch (IllegalArgumentException ex) {

            throw new IllegalArgumentException(
                    "Invalid parameter status: " + status
            );
        }
    }

    // ============================================================
    // SECURITY
    // ============================================================

    private void requireSuperAdmin() {

        User user = getCurrentUser();

        if (user.getRole() != Role.SUPER_ADMIN) {

            throw new AccessDeniedException(
                    "Only SUPER_ADMIN can manage test parameters"
            );
        }
    }

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Authentication required"
            );
        }

        Object details =
                authentication.getDetails();

        if (!(details instanceof User user)) {

            throw new AccessDeniedException(
                    "Authenticated user context is unavailable"
            );
        }

        return user;
    }

    // ============================================================
    // RESPONSE MAPPING
    // ============================================================

    private TestParameterResponse mapToResponse(
            TestParameter parameter
    ) {

        Test test = parameter.getTest();

        return TestParameterResponse.builder()
                .refId(parameter.getRefId())

                .testRefId(test.getRefId())
                .testCode(test.getCode())
                .testName(test.getName())

                .code(parameter.getCode())
                .name(parameter.getName())
                .description(parameter.getDescription())

                .dataType(parameter.getDataType())
                .inputType(parameter.getInputType())
                .calculationType(parameter.getCalculationType())
                .unit(parameter.getUnit())
                .required(parameter.isRequired())
                .displayOrder(parameter.getDisplayOrder())

                .referenceMin(
                        parameter.getReferenceMin()
                )
                .referenceMax(
                        parameter.getReferenceMax()
                )

                .criticalLow(
                        parameter.getCriticalLow()
                )
                .criticalHigh(
                        parameter.getCriticalHigh()
                )

                .reportDescription(
                        parameter.getReportDescription()
                )
                .interpretationGuidance(
                        parameter.getInterpretationGuidance()
                )

                .status(parameter.getStatus())
                .version(parameter.getVersion())

                .createdAt(parameter.getCreatedAt())
                .updatedAt(parameter.getUpdatedAt())

                .build();
    }

    // ============================================================
    // STRING NORMALIZATION
    // ============================================================

    private String normalize(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isBlank()
                ? null
                : trimmed;
    }

    private String normalizeRequired(
            String value,
            String fieldName
    ) {

        String normalized =
                normalize(value);

        if (normalized == null) {

            throw new IllegalArgumentException(
                    fieldName + " cannot be blank"
            );
        }

        return normalized;
    }

    private String normalizeCode(
            String code
    ) {

        if (code == null) {
            return null;
        }

        return code
                .trim()
                .toUpperCase();
    }
}
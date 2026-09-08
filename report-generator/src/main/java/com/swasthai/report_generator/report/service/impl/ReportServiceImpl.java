package com.swasthai.report_generator.report.service.impl;

import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.dto.request.AddReportTestRequest;
import com.swasthai.report_generator.report.dto.request.CreateReportRequest;
import com.swasthai.report_generator.report.dto.request.ReorderReportTestsRequest;
import com.swasthai.report_generator.report.dto.request.TestOrderItemInput;
import com.swasthai.report_generator.report.dto.request.UpdateReportParametersRequest;
import com.swasthai.report_generator.report.dto.response.ReportParameterItemResponse;
import com.swasthai.report_generator.report.dto.response.ReportResponse;
import com.swasthai.report_generator.report.dto.response.ReportTestItemResponse;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.entity.ReportTestResult;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.repository.ReportTestResultRepository;
import com.swasthai.report_generator.report.service.ReportService;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.dto.request.TestParameterResultInput;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestParameterResultRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.test.service.OrganizationTestService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReportTestResultRepository reportTestResultRepository;
    private final TestRepository testRepository;
    private final TestParameterRepository testParameterRepository;
    private final TestParameterResultRepository testParameterResultRepository;
    private final PatientRepository patientRepository;
    private final OrganizationTestService organizationTestService;
    private final CalculationEngine calculationEngine;

    // ============================================================
    // 1. CREATE REPORT (DRAFT)
    // ============================================================

    @Override
    public ReportResponse createReport(CreateReportRequest request) {
        if (request == null || request.patientRefId() == null || request.patientRefId().isBlank()) {
            throw new IllegalArgumentException("Patient reference ID is required");
        }

        User currentUser = getCurrentUser();
        Organization organization = getRequiredOrganization(currentUser);

        // Verify patient belongs to user's active organization
        String patientRefId = request.patientRefId().trim();
        validatePatientOwnership(patientRefId, organization.getId());

        Report report = Report.builder()
                .organization(organization)
                .patientRefId(patientRefId)
                .status(ReportStatus.DRAFT)
                .reportVersion(1)
                .lockVersion(0L)
                .createdBy(currentUser)
                .build();

        Report saved = reportRepository.save(report);
        return mapToResponse(saved);
    }

    // ============================================================
    // 2. GET REPORT
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReport(String reportRefId) {
        if (reportRefId == null || reportRefId.isBlank()) {
            throw new IllegalArgumentException("Report reference ID is required");
        }

        User currentUser = getCurrentUser();
        Report report = findAuthorizedReport(reportRefId, currentUser);
        return mapToResponse(report);
    }

    // ============================================================
    // 3. GET MY REPORTS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getMyReports(ReportStatus status, Pageable pageable) {
        User currentUser = getCurrentUser();
        Organization organization = getRequiredOrganization(currentUser);

        Page<Report> page;
        if (status != null) {
            page = reportRepository.findAllByOrganization_IdAndStatus(organization.getId(), status, pageable);
        } else {
            page = reportRepository.findAllByOrganization_Id(organization.getId(), pageable);
        }

        return page.map(this::mapToResponse);
    }

    // ============================================================
    // 4. ADD TEST TO REPORT DRAFT
    // ============================================================

    @Override
    public ReportResponse addTest(String reportRefId, AddReportTestRequest request) {
        if (request == null || request.testRefId() == null || request.testRefId().isBlank()) {
            throw new IllegalArgumentException("Test reference ID is required");
        }

        User currentUser = getCurrentUser();
        Report report = findAuthorizedReport(reportRefId, currentUser);

        validateDraftStatus(report);
        verifyLockVersion(report, request.lockVersion());

        String testRefId = request.testRefId().trim();

        // Check active test assignment
        validateTestAssignment(testRefId, currentUser);

        Test test = testRepository.findByRefId(testRefId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testRefId));

        if (test.getStatus() != TestStatus.ACTIVE) {
            throw new IllegalArgumentException("Test is not active: " + testRefId);
        }

        // Prevent duplicate test in same report (idempotency / invariant)
        if (reportTestResultRepository.existsByReport_IdAndTest_Id(report.getId(), test.getId())) {
            throw new IllegalArgumentException("Test already added to this report: " + testRefId);
        }

        // Calculate next display order
        int nextOrder = report.getTests().stream()
                .mapToInt(ReportTestResult::getDisplayOrder)
                .max()
                .orElse(0) + 1;

        ReportTestResult reportTest = ReportTestResult.builder()
                .report(report)
                .test(test)
                .displayOrder(nextOrder)
                .testVersion(test.getVersion())
                .build();

        // Snapshot all active TestParameters of this test
        List<TestParameter> activeParams =
                testParameterRepository.findAllByTest_IdAndStatusOrderByDisplayOrderAsc(
                        test.getId(),
                        TestParameterStatus.ACTIVE
                );

        for (TestParameter param : activeParams) {
            TestParameterResult paramResult = TestParameterResult.builder()
                    .testParameter(param)
                    .reportTestResult(reportTest)
                    .parameterCode(param.getCode())
                    .parameterName(param.getName())
                    .dataType(param.getDataType())
                    .inputType(param.getInputType())
                    .calculationType(param.getCalculationType() == null ? CalculationType.NONE : param.getCalculationType())
                    .calculationVersion(param.getInputType() == ParameterInputType.CALCULATED ? param.getCalculationType().name() + ":v1" : null)
                    .unit(param.getUnit())
                    .referenceMin(param.getReferenceMin())
                    .referenceMax(param.getReferenceMax())
                    .criticalLow(param.getCriticalLow())
                    .criticalHigh(param.getCriticalHigh())
                    .displayOrder(param.getDisplayOrder())
                    .build();

            reportTest.addParameterResult(paramResult);
        }

        report.addTest(reportTest);
        Report saved = reportRepository.save(report);
        return mapToResponse(saved);
    }

    // ============================================================
    // 5. REMOVE TEST FROM REPORT DRAFT
    // ============================================================

    @Override
    public ReportResponse removeTest(String reportRefId, String reportTestRefId) {
        if (reportTestRefId == null || reportTestRefId.isBlank()) {
            throw new IllegalArgumentException("Report test reference ID is required");
        }

        User currentUser = getCurrentUser();
        Report report = findAuthorizedReport(reportRefId, currentUser);

        validateDraftStatus(report);

        ReportTestResult targetTest = report.getTests().stream()
                .filter(t -> t.getRefId().equals(reportTestRefId.trim()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Test not found in report: " + reportTestRefId));

        report.removeTest(targetTest);

        // Normalize displayOrder of remaining tests
        int order = 1;
        for (ReportTestResult remaining : report.getTests()) {
            remaining.setDisplayOrder(order++);
        }

        Report saved = reportRepository.save(report);
        return mapToResponse(saved);
    }

    // ============================================================
    // 6. UPDATE PARAMETER VALUES & RUN CALCULATION ENGINE
    // ============================================================

    @Override
    public ReportResponse updateParameterValues(
            String reportRefId,
            String reportTestRefId,
            UpdateReportParametersRequest request
    ) {
        if (request == null || request.parameters() == null || request.parameters().isEmpty()) {
            throw new IllegalArgumentException("Parameter values list cannot be empty");
        }

        User currentUser = getCurrentUser();
        Report report = findAuthorizedReport(reportRefId, currentUser);

        validateDraftStatus(report);
        verifyLockVersion(report, request.lockVersion());

        ReportTestResult reportTest = report.getTests().stream()
                .filter(t -> t.getRefId().equals(reportTestRefId.trim()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Test not found in report: " + reportTestRefId));

        Map<String, TestParameterResult> resultsByRefId = new HashMap<>();
        Map<String, TestParameterResult> resultsByCode = new HashMap<>();

        for (TestParameterResult result : reportTest.getParameterResults()) {
            if (result.getTestParameter() != null) {
                resultsByRefId.put(result.getTestParameter().getRefId(), result);
            }
            resultsByCode.put(result.getParameterCode().toUpperCase(), result);
        }

        // Apply manual parameter inputs
        Set<String> seenCodes = new HashSet<>();
        for (TestParameterResultInput input : request.parameters()) {
            if (input == null) {
                continue;
            }

            TestParameterResult matched = resolveParameterResult(input, resultsByRefId, resultsByCode);

            String code = matched.getParameterCode().toUpperCase();
            if (seenCodes.contains(code)) {
                throw new IllegalArgumentException("Duplicate parameter submission: " + code);
            }
            seenCodes.add(code);

            // Rejection of calculated parameters
            if (matched.getInputType() == ParameterInputType.CALCULATED) {
                if (input.value() != null && !input.value().isBlank()) {
                    throw new IllegalArgumentException("Calculated parameters cannot be manually provided: " + code);
                }
                continue;
            }

            // Manual parameter: update value and numericValue
            String rawVal = input.value() != null ? input.value().trim() : null;
            if (rawVal != null && rawVal.isEmpty()) {
                rawVal = null;
            }

            matched.setValue(rawVal);
            if (rawVal != null && isNumericType(matched.getDataType())) {
                BigDecimal parsedNumber = parseStrictNumeric(code, rawVal);
                matched.setNumericValue(parsedNumber);
                matched.setFlag(classifyFlag(matched, parsedNumber));
            } else {
                matched.setNumericValue(null);
                matched.setFlag(null);
            }
        }

        // Run calculation engine for CALCULATED parameters
        executeCalculationsForTest(reportTest);

        Report saved = reportRepository.save(report);
        return mapToResponse(saved);
    }

    // ============================================================
    // 7. REORDER TESTS IN REPORT
    // ============================================================

    @Override
    public ReportResponse reorderTests(String reportRefId, ReorderReportTestsRequest request) {
        if (request == null || request.testOrders() == null || request.testOrders().isEmpty()) {
            throw new IllegalArgumentException("Test order list cannot be empty");
        }

        User currentUser = getCurrentUser();
        Report report = findAuthorizedReport(reportRefId, currentUser);

        validateDraftStatus(report);
        verifyLockVersion(report, request.lockVersion());

        Map<String, ReportTestResult> testsByRefId = report.getTests().stream()
                .collect(Collectors.toMap(ReportTestResult::getRefId, t -> t));

        if (request.testOrders().size() != report.getTests().size()) {
            throw new IllegalArgumentException("Reorder request must include all tests of the report");
        }

        Set<Integer> assignedOrders = new HashSet<>();
        for (TestOrderItemInput orderInput : request.testOrders()) {
            ReportTestResult test = testsByRefId.get(orderInput.reportTestRefId());
            if (test == null) {
                throw new IllegalArgumentException("Test does not belong to this report: " + orderInput.reportTestRefId());
            }

            int order = orderInput.displayOrder();
            if (order < 1 || order > report.getTests().size() || !assignedOrders.add(order)) {
                throw new IllegalArgumentException("Invalid or duplicate display order: " + order);
            }

            test.setDisplayOrder(order);
        }

        report.getTests().sort(Comparator.comparingInt(ReportTestResult::getDisplayOrder));
        Report saved = reportRepository.save(report);
        return mapToResponse(saved);
    }

    // ============================================================
    // 8. FINALIZE REPORT (IMMUTABILITY)
    // ============================================================

    @Override
    public ReportResponse finalizeReport(String reportRefId) {
        User currentUser = getCurrentUser();
        Report report = findAuthorizedReport(reportRefId, currentUser);

        if (report.getStatus() == ReportStatus.FINALIZED) {
            throw new IllegalStateException("Report is already finalized");
        }

        if (report.getTests().isEmpty()) {
            throw new IllegalArgumentException("Cannot finalize a report with no tests");
        }

        // Validate all tests: required parameters must have values and calculations succeed
        for (ReportTestResult reportTest : report.getTests()) {
            for (TestParameterResult paramResult : reportTest.getParameterResults()) {
                if (paramResult.getInputType() == ParameterInputType.MANUAL) {
                    TestParameter originalParam = paramResult.getTestParameter();
                    boolean isRequired = originalParam == null || originalParam.isRequired();
                    if (isRequired && (paramResult.getValue() == null || paramResult.getValue().isBlank())) {
                        throw new IllegalArgumentException(
                                "Required parameter is missing in test " + reportTest.getTest().getCode() + ": " + paramResult.getParameterCode()
                        );
                    }
                }
            }

            // Recalculate and re-flag to ensure authoritative final state
            executeCalculationsForTest(reportTest);
        }

        report.setStatus(ReportStatus.FINALIZED);
        report.setFinalizedBy(currentUser);
        report.setFinalizedAt(Instant.now());

        Report saved = reportRepository.save(report);
        return mapToResponse(saved);
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private void executeCalculationsForTest(ReportTestResult reportTest) {
        Map<String, BigDecimal> numericValues = new HashMap<>();
        for (TestParameterResult result : reportTest.getParameterResults()) {
            if (result.getInputType() == ParameterInputType.MANUAL && result.getNumericValue() != null) {
                numericValues.put(result.getParameterCode().toUpperCase(), result.getNumericValue());
            }
        }

        for (TestParameterResult result : reportTest.getParameterResults()) {
            if (result.getInputType() == ParameterInputType.CALCULATED) {
                CalculationType calcType = result.getCalculationType();
                if (calcType == null || calcType == CalculationType.NONE) {
                    continue;
                }

                if (!calculationEngine.isSupported(calcType)) {
                    throw new CalculationException("Unsupported calculation type: " + calcType);
                }

                Set<String> requiredDeps = calculationEngine.getRequiredParameters(calcType);
                boolean allDepsPresent = true;
                for (String dep : requiredDeps) {
                    if (!numericValues.containsKey(dep) || numericValues.get(dep) == null) {
                        allDepsPresent = false;
                        break;
                    }
                }

                if (allDepsPresent) {
                    BigDecimal calculated = calculationEngine.calculate(calcType, numericValues);
                    numericValues.put(result.getParameterCode().toUpperCase(), calculated);
                    result.setNumericValue(calculated);
                    result.setValue(calculated.stripTrailingZeros().toPlainString());
                    result.setFlag(classifyFlag(result, calculated));
                } else {
                    result.setNumericValue(null);
                    result.setValue(null);
                    result.setFlag(null);
                }
            }
        }
    }

    private TestParameterResult resolveParameterResult(
            TestParameterResultInput input,
            Map<String, TestParameterResult> byRefId,
            Map<String, TestParameterResult> byCode
    ) {
        if (input.parameterRefId() != null && !input.parameterRefId().isBlank()) {
            TestParameterResult res = byRefId.get(input.parameterRefId().trim());
            if (res == null) {
                throw new IllegalArgumentException("Parameter refId does not belong to this test: " + input.parameterRefId());
            }
            return res;
        }

        if (input.parameterCode() != null && !input.parameterCode().isBlank()) {
            TestParameterResult res = byCode.get(input.parameterCode().trim().toUpperCase());
            if (res == null) {
                throw new IllegalArgumentException("Parameter code does not belong to this test: " + input.parameterCode());
            }
            return res;
        }

        throw new IllegalArgumentException("Parameter refId or code is required");
    }

    private void validateDraftStatus(Report report) {
        if (report.getStatus() == ReportStatus.FINALIZED) {
            throw new IllegalStateException("Finalized report cannot be modified");
        }
    }

    private void verifyLockVersion(Report report, Long clientLockVersion) {
        if (clientLockVersion != null && !Objects.equals(report.getLockVersion(), clientLockVersion)) {
            throw new ObjectOptimisticLockingFailureException(
                    Report.class,
                    "Report has been modified by another transaction. Expected lock version: "
                            + report.getLockVersion() + " but received: " + clientLockVersion
            );
        }
    }

    private Report findAuthorizedReport(String reportRefId, User currentUser) {
        Report report;
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            report = reportRepository.findByRefId(reportRefId.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportRefId));
        } else {
            Organization organization = getRequiredOrganization(currentUser);
            report = reportRepository.findByRefIdAndOrganization_Id(reportRefId.trim(), organization.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportRefId));
        }
        return report;
    }

    private void validatePatientOwnership(String patientRefId, UUID organizationId) {
        boolean patientExists = patientRepository.findByRefIdAndOrganization_Id(patientRefId, organizationId).isPresent();
        if (!patientExists) {
            throw new IllegalArgumentException("Patient not found or does not belong to your organization: " + patientRefId);
        }
    }

    private void validateTestAssignment(String testRefId, User currentUser) {
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        if (!organizationTestService.hasTestAccess(testRefId)) {
            throw new AccessDeniedException("Organization does not have active access to test: " + testRefId);
        }
    }

    private boolean isNumericType(TestParameterDataType dataType) {
        return dataType == TestParameterDataType.DECIMAL || dataType == TestParameterDataType.INTEGER;
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

    private ResultFlag classifyFlag(TestParameterResult param, BigDecimal value) {
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

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Object details = authentication.getDetails();
        if (!(details instanceof User user)) {
            throw new AccessDeniedException("Authenticated user context is invalid");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User account is inactive");
        }

        return user;
    }

    private Organization getRequiredOrganization(User user) {
        Organization org = user.getOrganization();
        if (org == null || org.getStatus() != OrganizationStatus.ACTIVE) {
            throw new AccessDeniedException("Organization is inactive or not found");
        }
        return org;
    }

    private ReportResponse mapToResponse(Report report) {
        List<ReportTestItemResponse> testItems = report.getTests().stream()
                .sorted(Comparator.comparingInt(ReportTestResult::getDisplayOrder))
                .map(this::mapTestItem)
                .toList();

        return ReportResponse.builder()
                .refId(report.getRefId())
                .organizationRefId(report.getOrganization() != null ? report.getOrganization().getRefId() : null)
                .organizationName(report.getOrganization() != null ? report.getOrganization().getName() : null)
                .patientRefId(report.getPatientRefId())
                .status(report.getStatus())
                .reportVersion(report.getReportVersion())
                .lockVersion(report.getLockVersion())
                .createdByEmail(report.getCreatedBy() != null ? report.getCreatedBy().getEmail() : null)
                .finalizedByEmail(report.getFinalizedBy() != null ? report.getFinalizedBy().getEmail() : null)
                .finalizedAt(report.getFinalizedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .tests(testItems)
                .build();
    }

    private ReportTestItemResponse mapTestItem(ReportTestResult reportTest) {
        List<ReportParameterItemResponse> params = reportTest.getParameterResults().stream()
                .sorted(Comparator.comparingInt(TestParameterResult::getDisplayOrder))
                .map(this::mapParameterItem)
                .toList();

        return ReportTestItemResponse.builder()
                .refId(reportTest.getRefId())
                .testRefId(reportTest.getTest() != null ? reportTest.getTest().getRefId() : null)
                .testCode(reportTest.getTest() != null ? reportTest.getTest().getCode() : null)
                .testName(reportTest.getTest() != null ? reportTest.getTest().getName() : null)
                .displayOrder(reportTest.getDisplayOrder())
                .testVersion(reportTest.getTestVersion())
                .parameters(params)
                .build();
    }

    private ReportParameterItemResponse mapParameterItem(TestParameterResult result) {
        return ReportParameterItemResponse.builder()
                .refId(result.getRefId())
                .parameterRefId(result.getTestParameter() != null ? result.getTestParameter().getRefId() : null)
                .parameterCode(result.getParameterCode())
                .parameterName(result.getParameterName())
                .dataType(result.getDataType())
                .inputType(result.getInputType())
                .calculationType(result.getCalculationType())
                .calculationVersion(result.getCalculationVersion())
                .unit(result.getUnit())
                .value(result.getValue())
                .numericValue(result.getNumericValue())
                .flag(result.getFlag())
                .referenceMin(result.getReferenceMin())
                .referenceMax(result.getReferenceMax())
                .criticalLow(result.getCriticalLow())
                .criticalHigh(result.getCriticalHigh())
                .displayOrder(result.getDisplayOrder())
                .build();
    }
}
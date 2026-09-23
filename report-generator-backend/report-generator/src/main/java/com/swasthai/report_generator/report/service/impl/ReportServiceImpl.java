package com.swasthai.report_generator.report.service.impl;

import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.license.service.LicenseGuard;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationProfile;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationProfileRepository;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.patient.entity.AgeUnit;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.config.ReportRetentionProperties;
import com.swasthai.report_generator.report.dto.request.AddReportTestRequest;
import com.swasthai.report_generator.report.dto.request.BreakGlassAccessRequest;
import com.swasthai.report_generator.report.dto.request.BulkDeleteReportsRequest;
import com.swasthai.report_generator.report.dto.request.CreateReportRequest;
import com.swasthai.report_generator.report.dto.request.DeleteReportsByDateRangeRequest;
import com.swasthai.report_generator.report.dto.request.ReorderReportTestsRequest;
import com.swasthai.report_generator.report.dto.request.TestOrderItemInput;
import com.swasthai.report_generator.report.dto.request.UpdateReportParametersRequest;
import com.swasthai.report_generator.report.dto.response.BulkDeleteReportsResponse;
import com.swasthai.report_generator.report.dto.response.DeleteReportResponse;
import com.swasthai.report_generator.report.dto.response.ReportParameterItemResponse;
import com.swasthai.report_generator.report.dto.response.ReportResponse;
import com.swasthai.report_generator.report.dto.response.ReportTestItemResponse;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.entity.ReportTestResult;
import com.swasthai.report_generator.report.pdf.PdfRenderer;
import com.swasthai.report_generator.report.pdf.ReportPdfData;
import com.swasthai.report_generator.report.pdf.ReportPdfDataBuilder;
import com.swasthai.report_generator.report.pdf.VerificationQrCodeGenerator;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.repository.ReportTestResultRepository;
import com.swasthai.report_generator.report.service.ReportDeletionBatchExecutor;
import com.swasthai.report_generator.report.service.ReportPurgeBatchExecutor;
import com.swasthai.report_generator.report.service.ReportService;
import com.swasthai.report_generator.security.audit.service.AuditLogService;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.dto.request.TestParameterResultInput;
import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.ResultFlag;
import com.swasthai.report_generator.test.entity.Test;
import com.swasthai.report_generator.test.entity.TestParameter;
import com.swasthai.report_generator.test.entity.TestParameterDataType;
import com.swasthai.report_generator.test.entity.TestParameterResult;
import com.swasthai.report_generator.test.entity.TestParameterStatus;
import com.swasthai.report_generator.test.entity.TestStatus;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestParameterResultRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.test.service.OrganizationTestService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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

    private final ReportRetentionProperties retentionProperties;

    private final ReportPurgeBatchExecutor purgeBatchExecutor;

    private final ReportDeletionBatchExecutor deletionBatchExecutor;

    private final LicenseGuard licenseGuard;

    private final OrganizationRepository organizationRepository;

    private final OrganizationProfileRepository organizationProfileRepository;

    private final UserRepository userRepository;

    private final AuditLogService auditLogService;

    private final ReportPdfDataBuilder reportPdfDataBuilder;

    private final PdfRenderer pdfRenderer;

    private final VerificationQrCodeGenerator verificationQrCodeGenerator;


    // ============================================================
    // 1. CREATE REPORT
    // ============================================================

    @Override
    public ReportResponse createReport(
            CreateReportRequest request) {

        if (request == null
                || request.patientRefId() == null
                || request.patientRefId().isBlank()) {

            throw new IllegalArgumentException(
                    "Patient reference ID is required");
        }

        User currentUser = getCurrentUser();

        Organization organization =
                getRequiredOrganization(currentUser);

        /*
         * License is checked using the authenticated user's
         * server-side organization.
         *
         * Organization identity never comes from the request.
         */
        licenseGuard.requireReportCreationAllowed(
                organization.getId());

        String patientRefId =
                request.patientRefId().trim();

        validatePatientOwnership(
                patientRefId,
                organization.getId());

        Report report = Report.builder()
                .organization(organization)
                .patientRefId(patientRefId)
                .status(ReportStatus.DRAFT)
                .reportVersion(1)
                .lockVersion(0L)
                .createdBy(currentUser)
                .createdByName(currentUser.getName())
                .createdByEmail(currentUser.getEmail())
                .includeOrganizationHeader(Boolean.TRUE.equals(request.includeOrganizationHeader()))
                .build();

        Report saved =
                reportRepository.save(report);

        return mapToResponse(saved);
    }


    // ============================================================
    // 2. GET REPORT
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReport(
            String reportRefId) {

        String normalizedRefId =
                normalizeReportRefId(reportRefId);

        User currentUser =
                getCurrentUser();

        Report report =
                findAuthorizedReport(
                        normalizedRefId,
                        currentUser);

        return mapToResponse(report);
    }


    // ============================================================
    // 3. GET MY REPORTS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getMyReports(
            ReportStatus status,
            Pageable pageable) {

        User currentUser =
                getCurrentUser();

        Organization organization =
                getRequiredOrganization(currentUser);

        Page<Report> page;

        if (status != null) {

            page =
                    reportRepository
                            .findAllByOrganization_IdAndStatusAndDeletedAtIsNull(
                                    organization.getId(),
                                    status,
                                    pageable);

        } else {

            page =
                    reportRepository
                            .findAllByOrganization_IdAndDeletedAtIsNull(
                                    organization.getId(),
                                    pageable);
        }

        return page.map(this::mapToResponse);
    }


    // ============================================================
    // 4. ADD TEST TO REPORT
    // ============================================================

    @Override
    public ReportResponse addTest(
            String reportRefId,
            AddReportTestRequest request) {

        if (request == null
                || request.testRefId() == null
                || request.testRefId().isBlank()) {

            throw new IllegalArgumentException(
                    "Test reference ID is required");
        }

        User currentUser =
                getCurrentUser();

        Report report =
                findAuthorizedReport(
                        reportRefId,
                        currentUser);

        licenseGuard.requireActiveLicenseForOrganization(
                report.getOrganization().getId());

        validateDraftStatus(report);

        verifyLockVersion(
                report,
                request.lockVersion());

        String testRefId =
                request.testRefId().trim();

        validateTestAssignment(
                testRefId,
                currentUser);

        Test test =
                testRepository
                        .findByRefId(testRefId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Test not found: "
                                                + testRefId));

        if (test.getStatus() != TestStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "Test is not active: "
                            + testRefId);
        }

        boolean alreadyExists =
                reportTestResultRepository
                        .existsByReport_IdAndTest_Id(
                                report.getId(),
                                test.getId());

        if (alreadyExists) {

            throw new IllegalArgumentException(
                    "Test already added to this report: "
                            + testRefId);
        }

        int nextOrder =
                report.getTests()
                        .stream()
                        .mapToInt(
                                testResult ->
                                        testResult.getDisplayOrder() == null
                                                ? 0
                                                : testResult.getDisplayOrder())
                        .max()
                        .orElse(0)
                        + 1;

        ReportTestResult reportTest =
                ReportTestResult.builder()
                        .report(report)
                        .test(test)
                        .displayOrder(nextOrder)
                        .testVersion(test.getVersion())
                        .testCode(test.getCode())
                        .testName(test.getName())
                        .testShortName(test.getShortName())
                        .sampleType(
                                test.getSampleType() != null
                                        ? test.getSampleType().name()
                                        : null)
                        .customSampleType(
                                test.getCustomSampleType())
                        .specimenContainer(
                                test.getSpecimenContainer())
                        .reportSection(
                                test.getReportSection())
                        .build();

        List<TestParameter> activeParams =
                testParameterRepository
                        .findAllByTest_IdAndStatusOrderByDisplayOrderAsc(
                                test.getId(),
                                TestParameterStatus.ACTIVE);

        for (TestParameter param : activeParams) {

            CalculationType calculationType =
                    param.getCalculationType() == null
                            ? CalculationType.NONE
                            : param.getCalculationType();

            String calculationVersion = null;

            if (param.getInputType()
                    == ParameterInputType.CALCULATED) {

                calculationVersion =
                        calculationType.name() + ":v1";
            }

            TestParameterResult paramResult =
                    TestParameterResult.builder()
                            .testParameter(param)
                            .reportTestResult(reportTest)
                            .parameterCode(param.getCode())
                            .parameterName(param.getName())
                            .dataType(param.getDataType())
                            .inputType(param.getInputType())
                            .calculationType(calculationType)
                            .calculationVersion(calculationVersion)
                            .unit(param.getUnit())
                            .referenceMin(param.getReferenceMin())
                            .referenceMax(param.getReferenceMax())
                            .criticalLow(param.getCriticalLow())
                            .criticalHigh(param.getCriticalHigh())
                            .displayOrder(param.getDisplayOrder())
                            .build();

            reportTest.addParameterResult(
                    paramResult);
        }

        report.addTest(reportTest);

        Report saved =
                reportRepository.save(report);

        return mapToResponse(saved);
    }

    // ============================================================
    // 4b. ADD TESTS BULK
    // ============================================================

    @Override
    @Transactional
    public ReportResponse addTestsBulk(
            String reportRefId,
            com.swasthai.report_generator.report.dto.request.AddReportTestsBulkRequest request) {

        if (request == null || request.testRefIds() == null || request.testRefIds().isEmpty()) {
            throw new IllegalArgumentException("At least one test reference ID is required");
        }

        User currentUser = getCurrentUser();
        Report report = findAuthorizedReport(reportRefId, currentUser);

        licenseGuard.requireActiveLicenseForOrganization(report.getOrganization().getId());
        validateDraftStatus(report);
        verifyLockVersion(report, request.lockVersion());

        int currentOrder = report.getTests()
                .stream()
                .mapToInt(t -> t.getDisplayOrder() == null ? 0 : t.getDisplayOrder())
                .max()
                .orElse(0);

        for (String rawTestRefId : request.testRefIds()) {
            if (rawTestRefId == null || rawTestRefId.isBlank()) {
                continue;
            }
            String testRefId = rawTestRefId.trim();

            validateTestAssignment(testRefId, currentUser);

            Test test = testRepository.findByRefId(testRefId)
                    .orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testRefId));

            if (test.getStatus() != TestStatus.ACTIVE) {
                throw new IllegalArgumentException("Test is not active: " + testRefId);
            }

            boolean alreadyExists = report.getTests().stream()
                    .anyMatch(rt -> rt.getTest() != null && rt.getTest().getId().equals(test.getId()));

            if (alreadyExists) {
                continue;
            }

            currentOrder++;
            ReportTestResult reportTest = ReportTestResult.builder()
                    .report(report)
                    .test(test)
                    .displayOrder(currentOrder)
                    .testVersion(test.getVersion())
                    .testCode(test.getCode())
                    .testName(test.getName())
                    .testShortName(test.getShortName())
                    .sampleType(test.getSampleType() != null ? test.getSampleType().name() : null)
                    .customSampleType(test.getCustomSampleType())
                    .specimenContainer(test.getSpecimenContainer())
                    .reportSection(test.getReportSection())
                    .build();

            List<TestParameter> activeParams = testParameterRepository
                    .findAllByTest_IdAndStatusOrderByDisplayOrderAsc(test.getId(), TestParameterStatus.ACTIVE);

            for (TestParameter param : activeParams) {
                CalculationType calculationType = param.getCalculationType() == null ? CalculationType.NONE : param.getCalculationType();
                String calculationVersion = param.getInputType() == ParameterInputType.CALCULATED ? calculationType.name() + ":v1" : null;

                TestParameterResult paramResult = TestParameterResult.builder()
                        .testParameter(param)
                        .reportTestResult(reportTest)
                        .parameterCode(param.getCode())
                        .parameterName(param.getName())
                        .dataType(param.getDataType())
                        .inputType(param.getInputType())
                        .calculationType(calculationType)
                        .calculationVersion(calculationVersion)
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
        }

        Report saved = reportRepository.save(report);
        return mapToResponse(saved);
    }

    // ============================================================
    // 4c. RECALCULATE ALL TESTS IN REPORT
    // ============================================================

    @Override
    @Transactional
    public ReportResponse recalculateReport(String reportRefId) {
        User currentUser = getCurrentUser();
        Report report = findAuthorizedReport(reportRefId, currentUser);

        licenseGuard.requireActiveLicenseForOrganization(report.getOrganization().getId());
        validateDraftStatus(report);

        if (report.getTests() != null) {
            for (ReportTestResult reportTest : report.getTests()) {
                if (reportTest != null) {
                    executeCalculationsForTest(reportTest);
                }
            }
        }

        Report saved = reportRepository.save(report);
        return mapToResponse(saved);
    }

    // ============================================================
    // 5. REMOVE TEST
    // ============================================================

    @Override
    public ReportResponse removeTest(
            String reportRefId,
            String reportTestRefId) {

        if (reportTestRefId == null
                || reportTestRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Report test reference ID is required");
        }

        User currentUser =
                getCurrentUser();

        Report report =
                findAuthorizedReport(
                        reportRefId,
                        currentUser);

        licenseGuard.requireActiveLicenseForOrganization(
                report.getOrganization().getId());

        validateDraftStatus(report);

        String normalizedReportTestRefId =
                reportTestRefId.trim();

        ReportTestResult targetTest =
                report.getTests()
                        .stream()
                        .filter(test ->
                                normalizedReportTestRefId.equals(
                                        test.getRefId()))
                        .findFirst()
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Test not found in report: "
                                                + reportTestRefId));

        report.removeTest(targetTest);

        int order = 1;

        for (ReportTestResult remaining :
                report.getTests()) {

            remaining.setDisplayOrder(order++);
        }

        Report saved =
                reportRepository.save(report);

        return mapToResponse(saved);
    }


    // ============================================================
    // 6. UPDATE PARAMETER VALUES
    // ============================================================

    @Override
    public ReportResponse updateParameterValues(
            String reportRefId,
            String reportTestRefId,
            UpdateReportParametersRequest request) {

        if (request == null
                || request.parameters() == null
                || request.parameters().isEmpty()) {

            throw new IllegalArgumentException(
                    "Parameter values list cannot be empty");
        }

        User currentUser =
                getCurrentUser();

        Report report =
                findAuthorizedReport(
                        reportRefId,
                        currentUser);

        licenseGuard.requireActiveLicenseForOrganization(
                report.getOrganization().getId());

        validateDraftStatus(report);

        verifyLockVersion(
                report,
                request.lockVersion());

        String normalizedReportTestRefId =
                reportTestRefId == null
                        ? null
                        : reportTestRefId.trim();

        if (normalizedReportTestRefId == null
                || normalizedReportTestRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Report test reference ID is required");
        }

        ReportTestResult reportTest =
                report.getTests()
                        .stream()
                        .filter(test ->
                                normalizedReportTestRefId.equals(
                                        test.getRefId()))
                        .findFirst()
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Test not found in report: "
                                                + reportTestRefId));

        Map<String, TestParameterResult> resultsByRefId =
                new HashMap<>();

        Map<String, TestParameterResult> resultsByCode =
                new HashMap<>();

        for (TestParameterResult result :
                reportTest.getParameterResults()) {

            if (result.getTestParameter() != null
                    && result.getTestParameter().getRefId() != null) {

                resultsByRefId.put(
                        result.getTestParameter().getRefId(),
                        result);
            }

            if (result.getParameterCode() != null) {

                resultsByCode.put(
                        result.getParameterCode()
                                .trim()
                                .toUpperCase(),
                        result);
            }
        }

        Set<String> seenCodes =
                new HashSet<>();

        for (TestParameterResultInput input :
                request.parameters()) {

            if (input == null) {
                continue;
            }

            TestParameterResult matched =
                    resolveParameterResult(
                            input,
                            resultsByRefId,
                            resultsByCode);

            String code =
                    matched.getParameterCode()
                            .trim()
                            .toUpperCase();

            if (!seenCodes.add(code)) {

                throw new IllegalArgumentException(
                        "Duplicate parameter submission: "
                                + code);
            }

            /*
             * Calculated parameters are always backend controlled.
             */
            if (matched.getInputType()
                    == ParameterInputType.CALCULATED) {

                if (input.value() != null
                        && !input.value().isBlank()) {

                    throw new IllegalArgumentException(
                            "Calculated parameters cannot be manually provided: "
                                    + code);
                }

                continue;
            }

            String rawValue =
                    input.value() != null
                            ? input.value().trim()
                            : null;

            if (rawValue != null
                    && rawValue.isEmpty()) {

                rawValue = null;
            }

            matched.setValue(rawValue);

            if (rawValue != null
                    && isNumericType(
                            matched.getDataType())) {

                BigDecimal parsedNumber =
                        parseStrictNumeric(
                                code,
                                rawValue);

                matched.setNumericValue(
                        parsedNumber);

                matched.setFlag(
                        classifyFlag(
                                matched,
                                parsedNumber));

            } else {

                matched.setNumericValue(null);
                matched.setFlag(null);
            }
        }

        /*
         * Calculations are executed after all manual values
         * have been applied.
         */
        executeCalculationsForTest(
                reportTest);

        Report saved =
                reportRepository.save(report);

        return mapToResponse(saved);
    }


    // ============================================================
    // 7. REORDER TESTS
    // ============================================================

    @Override
    public ReportResponse reorderTests(
            String reportRefId,
            ReorderReportTestsRequest request) {

        if (request == null
                || request.testOrders() == null
                || request.testOrders().isEmpty()) {

            throw new IllegalArgumentException(
                    "Test order list cannot be empty");
        }

        User currentUser =
                getCurrentUser();

        Report report =
                findAuthorizedReport(
                        reportRefId,
                        currentUser);

        licenseGuard.requireActiveLicenseForOrganization(
                report.getOrganization().getId());

        validateDraftStatus(report);

        verifyLockVersion(
                report,
                request.lockVersion());

        Map<String, ReportTestResult> testsByRefId =
                report.getTests()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        ReportTestResult::getRefId,
                                        test -> test));

        if (request.testOrders().size()
                != report.getTests().size()) {

            throw new IllegalArgumentException(
                    "Reorder request must include all tests of the report");
        }

        Set<Integer> assignedOrders =
                new HashSet<>();

        Set<String> submittedTestIds =
                new HashSet<>();

        for (TestOrderItemInput orderInput :
                request.testOrders()) {

            if (orderInput == null
                    || orderInput.reportTestRefId() == null
                    || orderInput.reportTestRefId().isBlank()) {

                throw new IllegalArgumentException(
                        "Report test reference ID is required");
            }

            String testRefId =
                    orderInput.reportTestRefId().trim();

            ReportTestResult test =
                    testsByRefId.get(testRefId);

            if (test == null) {

                throw new IllegalArgumentException(
                        "Test does not belong to this report: "
                                + testRefId);
            }

            if (!submittedTestIds.add(testRefId)) {

                throw new IllegalArgumentException(
                        "Duplicate test in reorder request: "
                                + testRefId);
            }

            int order =
                    orderInput.displayOrder();

            if (order < 1
                    || order > report.getTests().size()
                    || !assignedOrders.add(order)) {

                throw new IllegalArgumentException(
                        "Invalid or duplicate display order: "
                                + order);
            }

            test.setDisplayOrder(order);
        }

        report.getTests()
                .sort(
                        Comparator.comparingInt(
                                test ->
                                        test.getDisplayOrder() == null
                                                ? Integer.MAX_VALUE
                                                : test.getDisplayOrder()));

        Report saved =
                reportRepository.save(report);

        return mapToResponse(saved);
    }


    // ============================================================
    // 7b. UPDATE HEADER OPTION
    // ============================================================

    @Override
    public ReportResponse updateHeaderOption(
            String reportRefId,
            com.swasthai.report_generator.report.dto.request.UpdateReportHeaderOptionRequest request) {

        if (request == null || request.includeOrganizationHeader() == null) {
            throw new IllegalArgumentException(
                    "includeOrganizationHeader is required");
        }

        User currentUser =
                getCurrentUser();

        Report report =
                findAuthorizedReport(
                        reportRefId,
                        currentUser);

        validateDraftStatus(report);

        report.setIncludeOrganizationHeader(
                Boolean.TRUE.equals(request.includeOrganizationHeader()));

        Report saved =
                reportRepository.save(report);

        return mapToResponse(saved);
    }


    // ============================================================
    // 8. FINALIZE REPORT
    // ============================================================

    @Override
    public ReportResponse finalizeReport(
            String reportRefId) {

        User currentUser =
                getCurrentUser();

        Report report =
                findAuthorizedReport(
                        reportRefId,
                        currentUser);

        /*
         * License must still be active when finalizing.
         */
        licenseGuard.requireActiveLicenseForOrganization(
                report.getOrganization().getId());

        if (report.getStatus()
                == ReportStatus.FINALIZED) {

            throw new IllegalStateException(
                    "Report is already finalized");
        }

        if (report.getTests() == null
                || report.getTests().isEmpty()) {

            throw new IllegalArgumentException(
                    "Cannot finalize a report with no tests");
        }

        /*
         * 8.1 VALIDATE TEST RESULTS
         */

        for (ReportTestResult reportTest :
                report.getTests()) {

            if (reportTest == null) {

                throw new IllegalStateException(
                        "Report contains an invalid test result");
            }

            if (reportTest.getTest() == null) {

                throw new IllegalStateException(
                        "Report test is missing source test configuration");
            }

            if (reportTest.getParameterResults() == null) {
                continue;
            }

            for (TestParameterResult paramResult :
                    reportTest.getParameterResults()) {

                if (paramResult == null) {

                    throw new IllegalStateException(
                            "Report contains an invalid parameter result");
                }

                if (paramResult.getInputType()
                        == ParameterInputType.MANUAL) {

                    /*
                     * At finalization the original TestParameter must
                     * still exist. We do not silently treat a corrupted
                     * result as required.
                     */
                    TestParameter originalParam =
                            paramResult.getTestParameter();

                    if (originalParam == null) {

                        throw new IllegalStateException(
                                "Parameter configuration is missing for parameter: "
                                        + paramResult.getParameterCode());
                    }

                    boolean isRequired =
                            originalParam.isRequired();

                    if (isRequired
                            && (paramResult.getValue() == null
                            || paramResult.getValue().isBlank())) {

                        throw new IllegalArgumentException(
                                "Required parameter is missing in test "
                                        + reportTest.getTest().getCode()
                                        + ": "
                                        + paramResult.getParameterCode());
                    }
                }
            }

            /*
             * Recalculate one final time immediately before
             * finalization so calculated values and flags are
             * authoritative.
             */
            executeCalculationsForTest(
                    reportTest);
        }


        /*
         * 8.2 PATIENT HISTORICAL SNAPSHOT
         */

        Patient patient =
                patientRepository
                        .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                                report.getPatientRefId(),
                                report.getOrganization().getId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Patient record not found for snapshot finalization: "
                                                + report.getPatientRefId()));

        validatePatientForFinalization(
                patient);

        report.setPatientName(
                patient.getName());

        report.setPatientSalutation(
                patient.getSalutation().name());

        report.setPatientCode(
                patient.getPatientCode());

        report.setPatientGender(
                patient.getGender().name());

        report.setPatientDateOfBirthKnown(
                patient.isDateOfBirthKnown());

        report.setPatientDateOfBirth(
                patient.getDateOfBirth());

        report.setPatientPhone(
                patient.getPhone());

        report.setPatientEmail(
                patient.getEmail());

        report.setPatientAddress(
                patient.getAddress());

        report.setPatientWeightKg(
                patient.getWeightKg());


        /*
         * 8.3 AGE SNAPSHOT
         */

        Instant finalizationInstant =
                Instant.now();

        ZoneId reportZone =
                ZoneId.systemDefault();

        LocalDate examinationDate =
                LocalDate.ofInstant(
                        finalizationInstant,
                        reportZone);

        if (patient.isDateOfBirthKnown()) {

            LocalDate dob =
                    patient.getDateOfBirth();

            if (dob == null) {

                throw new IllegalStateException(
                        "Patient date of birth is missing when dateOfBirthKnown is true");
            }

            if (dob.isAfter(examinationDate)) {

                throw new IllegalStateException(
                        "Patient date of birth cannot be after report finalization date");
            }

            Period period =
                    Period.between(
                            dob,
                            examinationDate);

            /*
             * Use Period directly.
             *
             * Do NOT use:
             *
             * ChronoUnit.MONTHS.between(
             *     dob.withDayOfMonth(1),
             *     examinationDate.withDayOfMonth(1)
             * )
             *
             * because that can incorrectly turn a few days
             * into one month.
             */

            if (period.getYears() > 0) {

                report.setPatientAgeAtReportingValue(
                        period.getYears());

                report.setPatientAgeAtReportingUnit(
                        AgeUnit.YEARS.name());

            } else if (period.getMonths() > 0) {

                report.setPatientAgeAtReportingValue(
                        period.getMonths());

                report.setPatientAgeAtReportingUnit(
                        AgeUnit.MONTHS.name());

            } else {

                long days =
                        ChronoUnit.DAYS.between(
                                dob,
                                examinationDate);

                if (days >= 7) {

                    report.setPatientAgeAtReportingValue(
                            (int) (days / 7));

                    report.setPatientAgeAtReportingUnit(
                            AgeUnit.WEEKS.name());

                } else {

                    report.setPatientAgeAtReportingValue(
                            (int) Math.max(0, days));

                    report.setPatientAgeAtReportingUnit(
                            AgeUnit.DAYS.name());
                }
            }

        } else {

            if (patient.getAgeValue() == null
                    || patient.getAgeUnit() == null) {

                throw new IllegalStateException(
                        "Patient age value and unit are required when DOB is unknown");
            }

            if (patient.getAgeValue() <= 0) {

                throw new IllegalStateException(
                        "Patient age must be greater than zero");
            }

            report.setPatientAgeAtReportingValue(
                    patient.getAgeValue());

            report.setPatientAgeAtReportingUnit(
                    patient.getAgeUnit().name());
        }


        /*
         * 8.4 ORGANIZATION + PROFILE SNAPSHOT
         */

        Organization organization =
                report.getOrganization();

        if (organization == null
                || organization.getName() == null
                || organization.getName().isBlank()) {

            throw new IllegalStateException(
                    "Organization details are missing for report finalization");
        }

        /*
         * Organization name is the authoritative organization
         * identity displayed in the report.
         */
        report.setOrganizationName(
                organization.getName());

        /*
         * Keep organization code as internal historical metadata.
         *
         * It is intentionally NOT rendered in the patient-facing PDF.
         */
        report.setOrganizationCode(
                organization.getCode());

        OrganizationProfile profile =
                organizationProfileRepository
                        .findByOrganization_Id(
                                organization.getId())
                        .orElse(null);

        if (profile != null) {
            /*
             * Address snapshot
             */
            report.setOrganizationAddressLine1(
                    profile.getAddressLine1());

            report.setOrganizationAddressLine2(
                    profile.getAddressLine2());

            report.setOrganizationCity(
                    profile.getCity());

            report.setOrganizationState(
                    profile.getState());

            report.setOrganizationPostalCode(
                    profile.getPostalCode());

            report.setOrganizationCountry(
                    profile.getCountry());

            /*
             * Contact snapshot
             */
            report.setOrganizationPhone(
                    profile.getPhone());

            report.setOrganizationAlternatePhone(
                    profile.getAlternatePhone());

            report.setOrganizationEmail(
                    profile.getEmail());

            report.setOrganizationWebsite(
                    profile.getWebsite());

            /*
             * Branding snapshot
             */
            report.setOrganizationLogoStorageKey(
                    profile.getLogoStorageKey());

            /*
             * Signature snapshot
             */
            String signatureStorageKey =
                    profile.getSignatureStorageKey();

            report.setOrganizationSignatureStorageKey(
                    signatureStorageKey);

            report.setOrganizationReportFooterText(
                    profile.getReportFooterText());

            report.setOrganizationReportDisclaimer(
                    profile.getReportDisclaimer());

            /*
             * SIGNATURE OWNER
             *
             * Never assume:
             *
             * "first active ORG_ADMIN"
             *
             * because an organization may have multiple administrators.
             *
             * OrganizationProfile must explicitly identify the current
             * authorized signature owner.
             */
            if (signatureStorageKey != null
                    && !signatureStorageKey.isBlank()) {

                String signatureOwnerRefId =
                        profile.getSignatureOwnerRefId();

                if (signatureOwnerRefId == null
                        || signatureOwnerRefId.isBlank()) {

                    throw new IllegalStateException(
                            "Organization signature is configured but signature owner is not configured");
                }

                User signatureOwner =
                        userRepository
                                .findByRefIdWithOrganization(
                                        signatureOwnerRefId.trim())
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "Configured organization signature owner was not found"));

                /*
                 * Owner must belong to the same organization.
                 */
                if (signatureOwner.getOrganization() == null
                        || !Objects.equals(
                                signatureOwner.getOrganization().getId(),
                                organization.getId())) {

                    throw new IllegalStateException(
                            "Configured signature owner does not belong to the organization");
                }

                /*
                 * Only an active ORG_ADMIN may own the organization
                 * signature in V1.
                 */
                if (signatureOwner.getRole()
                        != Role.ORG_ADMIN) {

                    throw new IllegalStateException(
                            "Configured signature owner must be an ORG_ADMIN");
                }

                if (signatureOwner.getStatus()
                        != UserStatus.ACTIVE) {

                    throw new IllegalStateException(
                            "Configured signature owner is inactive");
                }

                if (signatureOwner.getName() == null
                        || signatureOwner.getName().isBlank()) {

                    throw new IllegalStateException(
                            "Configured signature owner name is missing");
                }

                report.setOrganizationSignatureOwnerName(
                        signatureOwner.getName());

                report.setOrganizationSignatureOwnerEmail(
                        signatureOwner.getEmail());

            } else {
                report.setOrganizationSignatureOwnerName(
                        null);

                report.setOrganizationSignatureOwnerEmail(
                        null);
            }
        } else {
            report.setOrganizationSignatureOwnerName(
                    null);

            report.setOrganizationSignatureOwnerEmail(
                    null);
        }


        /*
         * 8.5 CREATOR SNAPSHOT
         */

        if (report.getCreatedByName() == null
                || report.getCreatedByName().isBlank()) {

            if (report.getCreatedBy() == null) {

                throw new IllegalStateException(
                        "Report creator information is missing");
            }

            report.setCreatedByName(
                    report.getCreatedBy().getName());

            report.setCreatedByEmail(
                    report.getCreatedBy().getEmail());
        }


        /*
         * 8.6 FINALIZER SNAPSHOT
         */

        if (currentUser.getName() == null
                || currentUser.getName().isBlank()) {

            throw new IllegalStateException(
                    "Finalizing user name is missing");
        }

        report.setFinalizedByName(
                currentUser.getName());

        report.setFinalizedByEmail(
                currentUser.getEmail());


        /*
         * 8.7 TEST HISTORICAL SNAPSHOTS
         */

        for (ReportTestResult reportTest :
                report.getTests()) {

            Test test =
                    reportTest.getTest();

            if (test == null) {

                throw new IllegalStateException(
                        "Report test is missing test configuration");
            }

            if (test.getCode() == null
                    || test.getCode().isBlank()) {

                throw new IllegalStateException(
                        "Test code is missing during report finalization");
            }

            if (test.getName() == null
                    || test.getName().isBlank()) {

                throw new IllegalStateException(
                        "Test name is missing during report finalization");
            }

            reportTest.setTestCode(
                    test.getCode());

            reportTest.setTestName(
                    test.getName());

            reportTest.setTestShortName(
                    test.getShortName());

            reportTest.setSampleType(
                    test.getSampleType() != null
                            ? test.getSampleType().name()
                            : null);

            reportTest.setCustomSampleType(
                    test.getCustomSampleType());

            reportTest.setSpecimenContainer(
                    test.getSpecimenContainer());

            reportTest.setReportSection(
                    test.getReportSection());

            if (reportTest.getTestVersion() == null) {

                reportTest.setTestVersion(
                        test.getVersion());
            }
        }


        /*
         * 8.8 FINAL STATE
         */

        report.setStatus(
                ReportStatus.FINALIZED);

        report.setFinalizedBy(
                currentUser);

        report.setFinalizedAt(
                finalizationInstant);

        Report saved =
                reportRepository.save(report);

        return mapToResponse(saved);
    }


    // ============================================================
    // CALCULATION ENGINE
    // ============================================================

    private void executeCalculationsForTest(
            ReportTestResult reportTest) {

        if (reportTest == null) {

            throw new IllegalArgumentException(
                    "Report test cannot be null");
        }

        if (reportTest.getParameterResults() == null) {
            return;
        }

        Map<String, BigDecimal> numericValues =
                new HashMap<>();


        /*
         * First collect manual values.
         */

        for (TestParameterResult result :
                reportTest.getParameterResults()) {

            if (result == null
                    || result.getParameterCode() == null) {
                continue;
            }

            if (result.getInputType()
                    == ParameterInputType.MANUAL
                    && result.getNumericValue() != null) {

                numericValues.put(
                        result.getParameterCode()
                                .trim()
                                .toUpperCase(),
                        result.getNumericValue());
            }
        }


        /*
         * Then calculate derived parameters.
         *
         * CalculationEngine remains authoritative for formulas.
         */

        for (TestParameterResult result :
                reportTest.getParameterResults()) {

            if (result == null) {
                continue;
            }

            if (result.getInputType()
                    != ParameterInputType.CALCULATED) {
                continue;
            }

            CalculationType calculationType =
                    result.getCalculationType();

            if (calculationType == null
                    || calculationType == CalculationType.NONE) {

                continue;
            }

            if (!calculationEngine.isSupported(
                    calculationType)) {

                throw new CalculationException(
                        "Unsupported calculation type: "
                                + calculationType);
            }

            Set<String> requiredDependencies =
                    calculationEngine
                            .getRequiredParameters(
                                    calculationType);

            boolean allDependenciesPresent =
                    true;

            for (String dependency :
                    requiredDependencies) {

                if (dependency == null
                        || !numericValues.containsKey(
                        dependency.trim().toUpperCase())
                        || numericValues.get(
                        dependency.trim().toUpperCase()) == null) {

                    allDependenciesPresent = false;
                    break;
                }
            }

            if (!allDependenciesPresent) {

                result.setNumericValue(null);
                result.setValue(null);
                result.setFlag(null);

                continue;
            }

            BigDecimal calculated =
                    calculationEngine.calculate(
                            calculationType,
                            numericValues);

            if (calculated == null) {

                result.setNumericValue(null);
                result.setValue(null);
                result.setFlag(null);

                continue;
            }

            numericValues.put(
                    result.getParameterCode()
                            .trim()
                            .toUpperCase(),
                    calculated);

            result.setNumericValue(
                    calculated);

            result.setValue(
                    calculated
                            .stripTrailingZeros()
                            .toPlainString());

            /*
             * IMPORTANT:
             *
             * Flag is calculated once by the service and stored.
             * PDF rendering must use this stored flag and must
             * never recalculate it.
             */
            result.setFlag(
                    classifyFlag(
                            result,
                            calculated));
        }
    }


    // ============================================================
    // PARAMETER RESOLUTION
    // ============================================================

    private TestParameterResult resolveParameterResult(
            TestParameterResultInput input,
            Map<String, TestParameterResult> byRefId,
            Map<String, TestParameterResult> byCode) {

        if (input == null) {

            throw new IllegalArgumentException(
                    "Parameter input cannot be null");
        }

        if (input.parameterRefId() != null
                && !input.parameterRefId().isBlank()) {

            String refId =
                    input.parameterRefId().trim();

            TestParameterResult result =
                    byRefId.get(refId);

            if (result == null) {

                throw new IllegalArgumentException(
                        "Parameter refId does not belong to this test: "
                                + input.parameterRefId());
            }

            return result;
        }

        if (input.parameterCode() != null
                && !input.parameterCode().isBlank()) {

            String code =
                    input.parameterCode()
                            .trim()
                            .toUpperCase();

            TestParameterResult result =
                    byCode.get(code);

            if (result == null) {

                throw new IllegalArgumentException(
                        "Parameter code does not belong to this test: "
                                + input.parameterCode());
            }

            return result;
        }

        throw new IllegalArgumentException(
                "Parameter refId or code is required");
    }


    // ============================================================
    // VALIDATION
    // ============================================================

    private void validateDraftStatus(
            Report report) {

        if (report == null) {

            throw new IllegalArgumentException(
                    "Report cannot be null");
        }

        if (report.getStatus()
                == ReportStatus.FINALIZED) {

            throw new IllegalStateException(
                    "Finalized report cannot be modified");
        }
    }


    private void verifyLockVersion(
            Report report,
            Long clientLockVersion) {

        if (clientLockVersion == null) {
            return;
        }

        if (!Objects.equals(
                report.getLockVersion(),
                clientLockVersion)) {

            throw new ObjectOptimisticLockingFailureException(
                    Report.class,
                    "Report has been modified by another transaction. "
                            + "Expected lock version: "
                            + report.getLockVersion()
                            + " but received: "
                            + clientLockVersion);
        }
    }


    private void validatePatientForFinalization(
            Patient patient) {

        if (patient == null) {

            throw new IllegalStateException(
                    "Patient record is required for report finalization");
        }

        if (patient.getName() == null
                || patient.getName().isBlank()) {

            throw new IllegalStateException(
                    "Patient name is required for report finalization");
        }

        if (patient.getSalutation() == null) {

            throw new IllegalStateException(
                    "Patient salutation is required for report finalization");
        }

        if (patient.getPatientCode() == null
                || patient.getPatientCode().isBlank()) {

            throw new IllegalStateException(
                    "Patient code is required for report finalization");
        }

        if (patient.getGender() == null) {

            throw new IllegalStateException(
                    "Patient gender is required for report finalization");
        }

        if (patient.isDateOfBirthKnown()) {

            if (patient.getDateOfBirth() == null) {

                throw new IllegalStateException(
                        "Patient date of birth is required when dateOfBirthKnown is true");
            }

            if (patient.getAgeValue() != null
                    || patient.getAgeUnit() != null) {

                /*
                 * DOB is authoritative when DOB is known.
                 * The stored age fields are ignored.
                 */
            }

        } else {

            if (patient.getDateOfBirth() != null) {

                throw new IllegalStateException(
                        "Patient date of birth must be null when dateOfBirthKnown is false");
            }

            if (patient.getAgeValue() == null
                    || patient.getAgeUnit() == null) {

                throw new IllegalStateException(
                        "Patient age value and unit are required when DOB is unknown");
            }

            if (patient.getAgeValue() <= 0) {

                throw new IllegalStateException(
                        "Patient age must be greater than zero");
            }
        }
    }


    // ============================================================
    // AUTHORIZATION / TENANT ISOLATION
    // ============================================================

    private Report findAuthorizedReport(
            String reportRefId,
            User currentUser) {

        String normalizedRefId =
                normalizeReportRefId(reportRefId);

        if (currentUser == null) {

            throw new AccessDeniedException(
                    "Authentication is required");
        }

        /*
         * SUPER_ADMIN must not directly access clinical reports.
         *
         * Emergency access must go through audited break-glass.
         */
        if (currentUser.getRole()
                == Role.SUPER_ADMIN) {

            throw new AccessDeniedException(
                    "Direct cross-tenant clinical report access is prohibited for SUPER_ADMIN. "
                            + "Use the audited break-glass support endpoint with documented justification.");
        }

        Organization organization =
                getRequiredOrganization(
                        currentUser);

        return reportRepository
                .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                        normalizedRefId,
                        organization.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Report not found: "
                                        + normalizedRefId));
    }


    // ============================================================
    // BREAK-GLASS ACCESS
    // ============================================================

    @Override
    @Transactional
    public ReportResponse breakGlassAccess(
            String reportRefId,
            BreakGlassAccessRequest request,
            String clientIp) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Break-glass access request is required");
        }

        String normalizedReportRefId =
                normalizeReportRefId(reportRefId);

        User currentUser =
                getCurrentUser();

        if (currentUser.getRole()
                != Role.SUPER_ADMIN) {

            throw new AccessDeniedException(
                    "Only SUPER_ADMIN can perform break-glass emergency report access");
        }

        String targetOrgRefId =
                request.organizationRefId() != null
                        ? request.organizationRefId().trim()
                        : null;

        if (targetOrgRefId == null
                || targetOrgRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Target organization reference ID is required");
        }

        Organization organization =
                organizationRepository
                        .findByRefId(targetOrgRefId)
                        .orElse(null);

        if (organization == null) {

            auditLogService.recordBreakGlassAccess(
                    currentUser,
                    null,
                    null,
                    targetOrgRefId,
                    normalizedReportRefId,
                    request.justification(),
                    false,
                    "Target organization not found",
                    clientIp);

            throw new ResourceNotFoundException(
                    "Target organization not found: "
                            + targetOrgRefId);
        }

        Report report =
                reportRepository
                        .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                                normalizedReportRefId,
                                organization.getId())
                        .orElse(null);

        if (report == null) {

            auditLogService.recordBreakGlassAccess(
                    currentUser,
                    organization,
                    null,
                    organization.getRefId(),
                    normalizedReportRefId,
                    request.justification(),
                    false,
                    "Target report not found in specified organization",
                    clientIp);

            throw new ResourceNotFoundException(
                    "Report not found: "
                            + normalizedReportRefId);
        }

        auditLogService.recordBreakGlassAccess(
                currentUser,
                organization,
                report,
                organization.getRefId(),
                report.getRefId(),
                request.justification(),
                true,
                null,
                clientIp);

        return mapToResponse(report);
    }


    private void validatePatientOwnership(
            String patientRefId,
            UUID organizationId) {

        boolean exists =
                patientRepository
                        .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                                patientRefId,
                                organizationId)
                        .isPresent();

        if (!exists) {

            throw new IllegalArgumentException(
                    "Patient not found or does not belong to your organization: "
                            + patientRefId);
        }
    }


    private void validateTestAssignment(
            String testRefId,
            User currentUser) {

        if (currentUser.getRole()
                == Role.SUPER_ADMIN) {

            return;
        }

        if (!organizationTestService.hasTestAccess(
                testRefId)) {

            throw new AccessDeniedException(
                    "Organization does not have active access to test: "
                            + testRefId);
        }
    }


    // ============================================================
    // NUMERIC / FLAG HELPERS
    // ============================================================

    private boolean isNumericType(
            TestParameterDataType dataType) {

        return dataType == TestParameterDataType.DECIMAL
                || dataType == TestParameterDataType.INTEGER;
    }


    private BigDecimal parseStrictNumeric(
            String parameterCode,
            String value) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "Numeric value cannot be blank for parameter: "
                            + parameterCode);
        }

        String normalized =
                value.trim();

        String lower =
                normalized.toLowerCase();

        /*
         * BigDecimal itself rejects NaN/infinity,
         * but explicitly reject scientific notation as part
         * of the API contract.
         */
        if (lower.contains("e")
                || lower.equals("nan")
                || lower.contains("infinity")) {

            throw new IllegalArgumentException(
                    "Invalid numeric format for parameter: "
                            + parameterCode);
        }

        try {

            return new BigDecimal(
                    normalized);

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    "Invalid numeric value for parameter "
                            + parameterCode
                            + ": "
                            + value);
        }
    }


    private ResultFlag classifyFlag(
            TestParameterResult parameter,
            BigDecimal value) {

        if (parameter == null
                || value == null) {

            return null;
        }

        /*
         * Critical ranges take precedence.
         */

        if (parameter.getCriticalLow() != null
                && value.compareTo(
                parameter.getCriticalLow()) < 0) {

            return ResultFlag.CRITICAL_LOW;
        }

        if (parameter.getCriticalHigh() != null
                && value.compareTo(
                parameter.getCriticalHigh()) > 0) {

            return ResultFlag.CRITICAL_HIGH;
        }

        if (parameter.getReferenceMin() != null
                && value.compareTo(
                parameter.getReferenceMin()) < 0) {

            return ResultFlag.LOW;
        }

        if (parameter.getReferenceMax() != null
                && value.compareTo(
                parameter.getReferenceMax()) > 0) {

            return ResultFlag.HIGH;
        }

        return ResultFlag.NORMAL;
    }


    // ============================================================
    // CURRENT USER
    // ============================================================

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal())) {

            throw new AccessDeniedException(
                    "Authentication is required");
        }

        User user = null;

        /*
         * JWT subject is the immutable user refId.
         *
         * Prefer refId only.
         */
        String identifier =
                authentication.getName();

        if (identifier != null
                && !identifier.isBlank()) {

            user =
                    userRepository
                            .findByRefIdWithOrganization(
                                    identifier.trim())
                            .orElse(null);
        }

        /*
         * Security filter may already have placed the
         * authenticated User in details.
         */
        if (user == null
                && authentication.getDetails()
                instanceof User detailsUser) {

            user = detailsUser;
        }

        if (user == null) {

            throw new AccessDeniedException(
                    "Authenticated user not found or invalid");
        }

        /*
         * Immediate lifecycle invalidation.
         */
        if (user.getStatus()
                != UserStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "User account is inactive");
        }

        return user;
    }


    private Organization getRequiredOrganization(
            User user) {

        if (user == null) {

            throw new AccessDeniedException(
                    "Authenticated user is required");
        }

        /*
         * SUPER_ADMIN is allowed to exist without
         * an organization, but operational report APIs
         * must never call this for SUPER_ADMIN.
         */
        Organization organization =
                user.getOrganization();

        if (organization == null
                || organization.getStatus()
                != OrganizationStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "Organization is inactive or not found");
        }

        return organization;
    }


    // ============================================================
    // RESPONSE MAPPING
    // ============================================================

    private ReportResponse mapToResponse(
            Report report) {

        List<ReportTestItemResponse> testItems =
                report.getTests() == null
                        ? Collections.emptyList()
                        : report.getTests()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        test ->
                                                test.getDisplayOrder() == null
                                                        ? Integer.MAX_VALUE
                                                        : test.getDisplayOrder()))
                        .map(this::mapTestItem)
                        .toList();

        return ReportResponse.builder()
                .refId(report.getRefId())
                .organizationRefId(
                        report.getOrganization() != null
                                ? report.getOrganization().getRefId()
                                : null)
                .organizationName(
                        report.getOrganizationName() != null
                                ? report.getOrganizationName()
                                : report.getOrganization() != null
                                ? report.getOrganization().getName()
                                : null)
                .patientRefId(
                        report.getPatientRefId())
                .status(
                        report.getStatus())
                .reportVersion(
                        report.getReportVersion())
                .lockVersion(
                        report.getLockVersion())
                .createdByEmail(
                        report.getCreatedByEmail() != null
                                ? report.getCreatedByEmail()
                                : report.getCreatedBy() != null
                                ? report.getCreatedBy().getEmail()
                                : null)
                .finalizedByEmail(
                        report.getFinalizedByEmail() != null
                                ? report.getFinalizedByEmail()
                                : report.getFinalizedBy() != null
                                ? report.getFinalizedBy().getEmail()
                                : null)
                .finalizedAt(
                        report.getFinalizedAt())
                .createdAt(
                        report.getCreatedAt())
                .updatedAt(
                        report.getUpdatedAt())
                .tests(testItems)
                .includeOrganizationHeader(
                        Boolean.TRUE.equals(
                                report.getIncludeOrganizationHeader()
                        )
                )
                .patientName(report.getPatientName())
                .patientSalutation(report.getPatientSalutation())
                .patientCode(report.getPatientCode())
                .patientGender(report.getPatientGender())
                .patientAgeAtReportingValue(report.getPatientAgeAtReportingValue())
                .patientAgeAtReportingUnit(report.getPatientAgeAtReportingUnit())
                .patientPhone(report.getPatientPhone())
                .createdByName(report.getCreatedByName())
                .finalizedByName(report.getFinalizedByName())
                .organizationAddressLine1(report.getOrganizationAddressLine1())
                .organizationAddressLine2(report.getOrganizationAddressLine2())
                .organizationCity(report.getOrganizationCity())
                .organizationState(report.getOrganizationState())
                .organizationPostalCode(report.getOrganizationPostalCode())
                .organizationCountry(report.getOrganizationCountry())
                .organizationPhone(report.getOrganizationPhone())
                .organizationAlternatePhone(report.getOrganizationAlternatePhone())
                .organizationEmail(report.getOrganizationEmail())
                .organizationWebsite(report.getOrganizationWebsite())
                .organizationSignatureOwnerName(report.getOrganizationSignatureOwnerName())
                .organizationReportFooterText(report.getOrganizationReportFooterText())
                .organizationReportDisclaimer(report.getOrganizationReportDisclaimer())
                .build();
    }


    private ReportTestItemResponse mapTestItem(
            ReportTestResult reportTest) {

        List<ReportParameterItemResponse> params =
                reportTest.getParameterResults() == null
                        ? Collections.emptyList()
                        : reportTest.getParameterResults()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        parameter ->
                                                parameter.getDisplayOrder() == null
                                                        ? Integer.MAX_VALUE
                                                        : parameter.getDisplayOrder()))
                        .map(this::mapParameterItem)
                        .toList();

        return ReportTestItemResponse.builder()
                .refId(
                        reportTest.getRefId())
                .testRefId(
                        reportTest.getTest() != null
                                ? reportTest.getTest().getRefId()
                                : null)
                .testCode(
                        reportTest.getTestCode() != null
                                ? reportTest.getTestCode()
                                : reportTest.getTest() != null
                                ? reportTest.getTest().getCode()
                                : null)
                .testName(
                        reportTest.getTestName() != null
                                ? reportTest.getTestName()
                                : reportTest.getTest() != null
                                ? reportTest.getTest().getName()
                                : null)
                .displayOrder(
                        reportTest.getDisplayOrder())
                .testVersion(
                        reportTest.getTestVersion())
                .parameters(params)
                .build();
    }


    private ReportParameterItemResponse mapParameterItem(
            TestParameterResult result) {

        return ReportParameterItemResponse.builder()
                .refId(
                        result.getRefId())
                .parameterRefId(
                        result.getTestParameter() != null
                                ? result.getTestParameter().getRefId()
                                : null)
                .parameterCode(
                        result.getParameterCode())
                .parameterName(
                        result.getParameterName())
                .dataType(
                        result.getDataType())
                .inputType(
                        result.getInputType())
                .calculationType(
                        result.getCalculationType())
                .calculationVersion(
                        result.getCalculationVersion())
                .unit(
                        result.getUnit())
                .value(
                        result.getValue())
                .numericValue(
                        result.getNumericValue())
                .flag(
                        result.getFlag())
                .referenceMin(
                        result.getReferenceMin())
                .referenceMax(
                        result.getReferenceMax())
                .criticalLow(
                        result.getCriticalLow())
                .criticalHigh(
                        result.getCriticalHigh())
                .displayOrder(
                        result.getDisplayOrder())
                .build();
    }


    // ============================================================
    // 9. DELETE SINGLE REPORT
    // ============================================================

    @Override
    public DeleteReportResponse deleteReport(
            String reportRefId) {

        User currentUser =
                getCurrentUser();

        if (currentUser.getRole()
                != Role.ORG_ADMIN) {

            throw new AccessDeniedException(
                    "Only organization administrators can delete reports");
        }

        Organization organization =
                getRequiredOrganization(
                        currentUser);

        String normalizedRefId =
                normalizeReportRefId(
                        reportRefId);

        Report report =
                reportRepository
                        .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                                normalizedRefId,
                                organization.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Report not found: "
                                                + normalizedRefId));

        validateDeletionEligibility(
                report);

        Instant deletionTime =
                Instant.now();

        report.setDeletedAt(
                deletionTime);

        report.setDeletedBy(
                currentUser);

        Report saved =
                reportRepository.save(report);

        return new DeleteReportResponse(
                saved.getRefId(),
                saved.getDeletedAt());
    }


    // ============================================================
    // 10. BULK REPORT DELETION
    // ============================================================

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BulkDeleteReportsResponse deleteReports(
            BulkDeleteReportsRequest request) {

        if (request == null
                || request.reportRefIds() == null
                || request.reportRefIds().isEmpty()) {

            throw new IllegalArgumentException(
                    "Report reference IDs list cannot be empty");
        }

        User currentUser =
                getCurrentUser();

        if (currentUser.getRole()
                != Role.ORG_ADMIN) {

            throw new AccessDeniedException(
                    "Only organization administrators can delete reports");
        }

        Organization organization =
                getRequiredOrganization(
                        currentUser);

        Set<String> normalizedRefIds =
                new LinkedHashSet<>();

        for (String refId :
                request.reportRefIds()) {

            if (refId == null
                    || refId.isBlank()) {

                throw new IllegalArgumentException(
                        "Report reference ID cannot be blank");
            }

            normalizedRefIds.add(
                    refId.trim());
        }

        if (normalizedRefIds.isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one valid report reference ID is required");
        }

        Instant now =
                Instant.now();

        Instant cutoff =
                now.minus(
                        retentionProperties.getDeleteAfterDays(),
                        ChronoUnit.DAYS);

        List<String> idList =
                new ArrayList<>(
                        normalizedRefIds);

        int batchSize =
                retentionProperties.getBulkBatchSize();

        int totalDeleted = 0;

        for (int i = 0;
             i < idList.size();
             i += batchSize) {

            List<String> chunk =
                    idList.subList(
                            i,
                            Math.min(
                                    i + batchSize,
                                    idList.size()));

            int deletedInBatch =
                    deletionBatchExecutor
                            .softDeleteSelectedBatch(
                                    chunk,
                                    organization.getId(),
                                    currentUser.getId(),
                                    now,
                                    cutoff);

            totalDeleted +=
                    deletedInBatch;
        }

        int skippedCount =
                normalizedRefIds.size()
                        - totalDeleted;

        return new BulkDeleteReportsResponse(
                totalDeleted,
                skippedCount,
                now);
    }


    // ============================================================
    // 11. DATE RANGE REPORT DELETION
    // ============================================================

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BulkDeleteReportsResponse deleteReportsByDateRange(
            DeleteReportsByDateRangeRequest request) {

        if (request == null
                || request.from() == null
                || request.to() == null) {

            throw new IllegalArgumentException(
                    "From and To dates are required");
        }

        if (request.from()
                .isAfter(request.to())) {

            throw new IllegalArgumentException(
                    "From date cannot be after To date");
        }

        User currentUser =
                getCurrentUser();

        if (currentUser.getRole()
                != Role.ORG_ADMIN) {

            throw new AccessDeniedException(
                    "Only organization administrators can delete reports");
        }

        Organization organization =
                getRequiredOrganization(
                        currentUser);

        ZoneId zoneId =
                ZoneId.of(
                        retentionProperties.getTimeZone());

        Instant start =
                request.from()
                        .atStartOfDay(zoneId)
                        .toInstant();

        Instant endExclusive =
                request.to()
                        .plusDays(1)
                        .atStartOfDay(zoneId)
                        .toInstant();

        Instant now =
                Instant.now();

        Instant retentionCutoff =
                now.minus(
                        retentionProperties.getDeleteAfterDays(),
                        ChronoUnit.DAYS);

        Instant effectiveEnd =
                endExclusive.isBefore(
                        retentionCutoff)
                        ? endExclusive
                        : retentionCutoff;

        if (!start.isBefore(
                effectiveEnd)) {

            long skippedYoung =
                    reportRepository
                            .countYoungSkippedReports(
                                    organization.getId(),
                                    start,
                                    endExclusive);

            return new BulkDeleteReportsResponse(
                    0,
                    toSafeInt(skippedYoung),
                    now);
        }

        long skippedYoung = 0;

        if (effectiveEnd.isBefore(
                endExclusive)) {

            skippedYoung =
                    reportRepository
                            .countYoungSkippedReports(
                                    organization.getId(),
                                    effectiveEnd,
                                    endExclusive);
        }

        int batchSize =
                retentionProperties.getBulkBatchSize();

        int totalDeleted = 0;

        while (true) {

            int deletedInBatch =
                    deletionBatchExecutor
                            .softDeleteDateRangeBatch(
                                    organization.getId(),
                                    currentUser.getId(),
                                    start,
                                    effectiveEnd,
                                    now,
                                    batchSize);

            if (deletedInBatch == 0) {
                break;
            }

            totalDeleted +=
                    deletedInBatch;
        }

        return new BulkDeleteReportsResponse(
                totalDeleted,
                toSafeInt(skippedYoung),
                now);
    }


    // ============================================================
    // 12. PURGE EXPIRED REPORTS
    // ============================================================

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int purgeExpiredReports() {

        if (Boolean.FALSE.equals(
                retentionProperties.getPurgeEnabled())) {

            return 0;
        }

        Instant now =
                Instant.now();

        Instant purgeCutoff =
                now.minus(
                        retentionProperties.getPurgeAfterDays(),
                        ChronoUnit.DAYS);

        int batchSize =
                retentionProperties.getPurgeBatchSize();

        int totalPurged = 0;

        while (true) {

            int purgedInBatch =
                    purgeBatchExecutor.purgeBatch(
                            purgeCutoff,
                            batchSize);

            if (purgedInBatch == 0) {
                break;
            }

            totalPurged +=
                    purgedInBatch;
        }

        return totalPurged;
    }


    // ============================================================
    // DELETE HELPERS
    // ============================================================

    private int toSafeInt(
            long value) {

        if (value < 0) {

            throw new IllegalStateException(
                    "Deletion count cannot be negative");
        }

        if (value > Integer.MAX_VALUE) {

            throw new IllegalStateException(
                    "Deletion count exceeds supported response range");
        }

        return (int) value;
    }


    private String normalizeReportRefId(
            String reportRefId) {

        if (reportRefId == null
                || reportRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Report reference ID is required");
        }

        return reportRefId.trim();
    }


    private void validateDeletionEligibility(
            Report report) {

        if (report == null
                || report.getCreatedAt() == null) {

            throw new IllegalStateException(
                    "Report creation timestamp is missing");
        }

        if (retentionProperties.getDeleteAfterDays() <= 0) {
            return;
        }

        Instant cutoff =
                Instant.now()
                        .minus(
                                retentionProperties
                                        .getDeleteAfterDays(),
                                ChronoUnit.DAYS);

        if (!report.getCreatedAt()
                .isBefore(cutoff)) {

            throw new IllegalStateException(
                    "Report has not reached the deletion eligibility period");
        }
    }


    // ============================================================
    // PDF GENERATION
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReportPdf(
            String reportRefId) {

        ReportPdfData pdfData =
                getReportPdfData(
                        reportRefId);

        return pdfRenderer.render(
                pdfData);
    }


    @Override
    @Transactional(readOnly = true)
    public ReportPdfData getReportPdfData(
            String reportRefId) {

        String normalizedRefId =
                normalizeReportRefId(
                        reportRefId);

        User currentUser =
                getCurrentUser();

        Report report =
                findAuthorizedReport(
                        normalizedRefId,
                        currentUser);

        if (report.getStatus()
                != ReportStatus.FINALIZED) {

            throw new IllegalStateException(
                    "PDF can only be generated for finalized reports");
        }

        /*
         * IMPORTANT:
         *
         * ReportPdfDataBuilder must use ONLY historical
         * snapshots stored on Report / ReportTestResult /
         * TestParameterResult.
         *
         * It must NOT fall back to current:
         *
         * - Patient
         * - Organization
         * - OrganizationProfile
         * - User
         * - Test
         *
         * Otherwise an old finalized report could change
         * when master data changes.
         */
        return reportPdfDataBuilder.build(
                report);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getReportQrPngBytes(String reportRefId) {
        String normalizedRefId = normalizeReportRefId(reportRefId);
        User currentUser = getCurrentUser();
        Report report = findAuthorizedReport(normalizedRefId, currentUser);

        if (report.getStatus() != ReportStatus.FINALIZED) {
            throw new IllegalStateException("Verification QR code is only available for finalized reports");
        }

        String verificationUrl = "https://verify.swasthai.com/reports/" + report.getRefId().trim();
        return verificationQrCodeGenerator.generatePngBytes(verificationUrl);
    }
}
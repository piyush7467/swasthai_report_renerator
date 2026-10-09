package com.swasthai.report_generator.test.masterdata.audit;

import com.swasthai.report_generator.test.calculation.CalculationDependencyResolver;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.ClinicalParameterAliases;
import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.Test;
import com.swasthai.report_generator.test.entity.TestCategory;
import com.swasthai.report_generator.test.entity.TestParameter;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Audits existing database master data records to verify natural-key integrity,
 * public refId preservation, referential consistency, and calculation validity.
 */
@Component
@RequiredArgsConstructor
public class ExistingMasterDataAuditor {

    private static final Logger log = LoggerFactory.getLogger(ExistingMasterDataAuditor.class);

    private final TestCategoryRepository testCategoryRepository;
    private final TestRepository testRepository;
    private final TestParameterRepository testParameterRepository;
    private final CalculationEngine calculationEngine;

    public ExistingMasterDataAuditReport audit() {
        log.info("Starting comprehensive audit of existing database master data...");

        List<MasterDataAuditIssue> issues = new ArrayList<>();

        // 1. Audit Categories
        List<TestCategory> categories = testCategoryRepository.findAll();
        int categoriesTotal = categories.size();
        int categoriesValid = 0;
        int categoriesMissingCode = 0;
        int categoriesDuplicateCode = 0;
        int categoriesInvalidRefId = 0;
        int categoriesConflicts = 0;

        Set<String> seenCategoryCodes = new HashSet<>();
        Set<String> seenCategoryRefIds = new HashSet<>();

        for (TestCategory cat : categories) {
            boolean valid = true;
            String code = cat.getCode();
            String refId = cat.getRefId();

            if (code == null || code.isBlank()) {
                categoriesMissingCode++;
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("Category")
                        .refId(refId)
                        .problem("MASTER DATA MIGRATION REQUIRED: Missing stable code")
                        .recommendedAction("Assign a stable machine-readable code via database update.")
                        .build());
            } else {
                String normCode = code.trim().toUpperCase();
                if (!seenCategoryCodes.add(normCode)) {
                    categoriesDuplicateCode++;
                    valid = false;
                    issues.add(MasterDataAuditIssue.builder()
                            .entityType("Category")
                            .code(normCode)
                            .refId(refId)
                            .problem("MASTER DATA CONFLICT: Duplicate category code in existing database")
                            .recommendedAction("Manual deduplication required. Resolve duplicate code.")
                            .build());
                }
            }

            if (refId == null || !refId.startsWith("TC-")) {
                categoriesInvalidRefId++;
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("Category")
                        .code(code)
                        .refId(refId)
                        .problem("Invalid refId prefix (expected 'TC-')")
                        .recommendedAction("Verify refId integrity. Ensure standard prefix.")
                        .build());
            } else if (!seenCategoryRefIds.add(refId)) {
                categoriesInvalidRefId++;
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("Category")
                        .code(code)
                        .refId(refId)
                        .problem("Duplicate public refId detected in database")
                        .recommendedAction("Resolve duplicate refId immediately.")
                        .build());
            }

            if (cat.getName() == null || cat.getName().isBlank()) {
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("Category")
                        .code(code)
                        .refId(refId)
                        .problem("Missing required name")
                        .recommendedAction("Provide non-blank name for category.")
                        .build());
            }

            if (valid) {
                categoriesValid++;
            }
        }

        // 2. Audit Tests
        List<Test> tests = testRepository.findAll();
        int testsTotal = tests.size();
        int testsValid = 0;
        int testsMissingCode = 0;
        int testsDuplicateCode = 0;
        int testsInvalidCategoryRelationship = 0;
        int testsInvalidRefId = 0;
        int testsConflicts = 0;

        Set<String> seenTestCodes = new HashSet<>();
        Set<String> seenTestRefIds = new HashSet<>();

        for (Test test : tests) {
            boolean valid = true;
            String code = test.getCode();
            String refId = test.getRefId();

            if (code == null || code.isBlank()) {
                testsMissingCode++;
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("Test")
                        .refId(refId)
                        .problem("MASTER DATA MIGRATION REQUIRED: Missing stable code")
                        .recommendedAction("Assign a stable machine-readable test code via update.")
                        .build());
            } else {
                String normCode = code.trim().toUpperCase();
                if (!seenTestCodes.add(normCode)) {
                    testsDuplicateCode++;
                    valid = false;
                    issues.add(MasterDataAuditIssue.builder()
                            .entityType("Test")
                            .code(normCode)
                            .refId(refId)
                            .problem("MASTER DATA CONFLICT: Duplicate test code in existing database")
                            .recommendedAction("Manual deduplication required.")
                            .build());
                }
            }

            if (refId == null || !refId.startsWith("TEST-")) {
                testsInvalidRefId++;
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("Test")
                        .code(code)
                        .refId(refId)
                        .problem("Invalid refId prefix (expected 'TEST-')")
                        .recommendedAction("Verify refId integrity.")
                        .build());
            } else if (!seenTestRefIds.add(refId)) {
                testsInvalidRefId++;
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("Test")
                        .code(code)
                        .refId(refId)
                        .problem("Duplicate public refId detected in database")
                        .recommendedAction("Resolve duplicate test refId.")
                        .build());
            }

            if (test.getCategory() == null || test.getCategory().getId() == null) {
                testsInvalidCategoryRelationship++;
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("Test")
                        .code(code)
                        .refId(refId)
                        .problem("Orphan test: missing parent category relationship")
                        .recommendedAction("Link test to an active category.")
                        .build());
            }

            if (test.getName() == null || test.getName().isBlank()) {
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("Test")
                        .code(code)
                        .refId(refId)
                        .problem("Missing required test name")
                        .recommendedAction("Set valid test name.")
                        .build());
            }

            if (test.getSampleType() == null) {
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("Test")
                        .code(code)
                        .refId(refId)
                        .problem("Missing required sampleType")
                        .recommendedAction("Specify valid SampleType enum.")
                        .build());
            }

            if (valid) {
                testsValid++;
            }
        }

        // 3. Audit Parameters
        List<TestParameter> parameters = testParameterRepository.findAll();
        int parametersTotal = parameters.size();
        int parametersValid = 0;
        int parametersMissingCode = 0;
        int parametersDuplicateNaturalKey = 0;
        int parametersInvalidTestRelationship = 0;
        int parametersInvalidCalculation = 0;
        int parametersMissingDependency = 0;
        int parametersCircularDependency = 0;
        int parametersConflicts = 0;

        Map<UUID, List<TestParameter>> paramsByTestId = new HashMap<>();
        Set<String> seenParameterRefIds = new HashSet<>();
        Set<String> seenCompositeKeys = new HashSet<>();

        for (TestParameter param : parameters) {
            boolean valid = true;
            String code = param.getCode();
            String refId = param.getRefId();
            Test test = param.getTest();

            if (code == null || code.isBlank()) {
                parametersMissingCode++;
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("TestParameter")
                        .refId(refId)
                        .problem("MASTER DATA MIGRATION REQUIRED: Missing stable parameter code")
                        .recommendedAction("Assign a stable parameter code.")
                        .build());
            }

            if (test == null || test.getId() == null) {
                parametersInvalidTestRelationship++;
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("TestParameter")
                        .code(code)
                        .refId(refId)
                        .problem("Orphan parameter: missing parent test relationship")
                        .recommendedAction("Link parameter to an active test.")
                        .build());
            } else if (code != null && !code.isBlank()) {
                String composite = test.getId() + ":" + code.trim().toUpperCase();
                if (!seenCompositeKeys.add(composite)) {
                    parametersDuplicateNaturalKey++;
                    valid = false;
                    issues.add(MasterDataAuditIssue.builder()
                            .entityType("TestParameter")
                            .code(code)
                            .refId(refId)
                            .relatedEntity(test.getCode())
                            .problem("Duplicate natural key (test + code) in existing database")
                            .recommendedAction("Deduplicate parameter within test.")
                            .build());
                }
                paramsByTestId.computeIfAbsent(test.getId(), k -> new ArrayList<>()).add(param);
            }

            if (refId == null || !refId.startsWith("PARAM-")) {
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("TestParameter")
                        .code(code)
                        .refId(refId)
                        .problem("Invalid refId prefix (expected 'PARAM-')")
                        .recommendedAction("Verify refId integrity.")
                        .build());
            } else if (!seenParameterRefIds.add(refId)) {
                valid = false;
                issues.add(MasterDataAuditIssue.builder()
                        .entityType("TestParameter")
                        .code(code)
                        .refId(refId)
                        .problem("Duplicate public refId detected in database")
                        .recommendedAction("Resolve duplicate parameter refId.")
                        .build());
            }

            // Calculation validation for individual parameter
            if (param.getInputType() == ParameterInputType.CALCULATED) {
                CalculationType calcType = param.getCalculationType();
                if (calcType == null || calcType == CalculationType.NONE) {
                    parametersInvalidCalculation++;
                    valid = false;
                    issues.add(MasterDataAuditIssue.builder()
                            .entityType("TestParameter")
                            .code(code)
                            .refId(refId)
                            .relatedEntity(test != null ? test.getCode() : "UNKNOWN")
                            .problem("Calculated parameter missing valid CalculationType")
                            .recommendedAction("Assign a recognized CalculationType.")
                            .build());
                } else if (calculationEngine != null && !calculationEngine.isSupported(calcType)) {
                    parametersInvalidCalculation++;
                    valid = false;
                    issues.add(MasterDataAuditIssue.builder()
                            .entityType("TestParameter")
                            .code(code)
                            .refId(refId)
                            .relatedEntity(test != null ? test.getCode() : "UNKNOWN")
                            .problem("CalculationType '" + calcType + "' has no registered calculator in CalculationEngine")
                            .recommendedAction("Implement calculator or fix calculationType.")
                            .build());
                }
            }

            if (valid) {
                parametersValid++;
            }
        }

        // Test-level dependency and cycle checks for parameters
        if (calculationEngine != null) {
            for (Map.Entry<UUID, List<TestParameter>> entry : paramsByTestId.entrySet()) {
                List<TestParameter> testParams = entry.getValue();
                Set<String> availableParamCodes = new HashSet<>();
                for (TestParameter p : testParams) {
                    if (p.getCode() != null) {
                        String norm = p.getCode().trim().toUpperCase();
                        availableParamCodes.add(norm);
                        String canon = ClinicalParameterAliases.getCanonical(norm);
                        if (canon != null) availableParamCodes.add(canon);
                    }
                }

                List<TestParameter> calculatedParams = new ArrayList<>();
                for (TestParameter p : testParams) {
                    if (p.getInputType() == ParameterInputType.CALCULATED && p.getCalculationType() != null
                            && p.getCalculationType() != CalculationType.NONE) {
                        calculatedParams.add(p);
                        Set<String> reqs = calculationEngine.getRequiredParameters(p.getCalculationType());
                        for (String req : reqs) {
                            String normReq = req.trim().toUpperCase();
                            String canonReq = ClinicalParameterAliases.getCanonical(normReq);
                            if (!availableParamCodes.contains(normReq) && (canonReq == null || !availableParamCodes.contains(canonReq))) {
                                parametersMissingDependency++;
                                issues.add(MasterDataAuditIssue.builder()
                                        .entityType("TestParameter")
                                        .code(p.getCode())
                                        .refId(p.getRefId())
                                        .relatedEntity(p.getTest() != null ? p.getTest().getCode() : "UNKNOWN")
                                        .problem("Missing required dependency parameter '" + req + "' for calculation " + p.getCalculationType())
                                        .recommendedAction("Add prerequisite parameter '" + req + "' to this test.")
                                        .build());
                            }
                        }
                    }
                }

                if (calculatedParams.size() > 1) {
                    try {
                        CalculationDependencyResolver.resolveParameterOrder(calculatedParams, calculationEngine);
                    } catch (Exception ex) {
                        parametersCircularDependency++;
                        issues.add(MasterDataAuditIssue.builder()
                                .entityType("TestParameter")
                                .relatedEntity(testParams.get(0).getTest() != null ? testParams.get(0).getTest().getCode() : "UNKNOWN")
                                .problem("Circular calculation dependency graph: " + ex.getMessage())
                                .recommendedAction("Break circular dependencies between calculated parameters.")
                                .build());
                    }
                }
            }
        }

        ExistingMasterDataAuditReport report = ExistingMasterDataAuditReport.builder()
                .categoriesTotal(categoriesTotal)
                .categoriesValid(categoriesValid)
                .categoriesMissingCode(categoriesMissingCode)
                .categoriesDuplicateCode(categoriesDuplicateCode)
                .categoriesInvalidRefId(categoriesInvalidRefId)
                .categoriesConflicts(categoriesConflicts)
                .testsTotal(testsTotal)
                .testsValid(testsValid)
                .testsMissingCode(testsMissingCode)
                .testsDuplicateCode(testsDuplicateCode)
                .testsInvalidCategoryRelationship(testsInvalidCategoryRelationship)
                .testsInvalidRefId(testsInvalidRefId)
                .testsConflicts(testsConflicts)
                .parametersTotal(parametersTotal)
                .parametersValid(parametersValid)
                .parametersMissingCode(parametersMissingCode)
                .parametersDuplicateNaturalKey(parametersDuplicateNaturalKey)
                .parametersInvalidTestRelationship(parametersInvalidTestRelationship)
                .parametersInvalidCalculation(parametersInvalidCalculation)
                .parametersMissingDependency(parametersMissingDependency)
                .parametersCircularDependency(parametersCircularDependency)
                .parametersConflicts(parametersConflicts)
                .issues(issues)
                .build();

        log.info("{}", report.toFormattedReport());
        return report;
    }
}

package com.swasthai.report_generator.test.masterdata.validation;

import com.swasthai.report_generator.test.calculation.CalculationDependencyResolver;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.ClinicalParameterAliases;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties;
import com.swasthai.report_generator.test.masterdata.dto.MasterCategorySeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterParameterSeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterTestSeedDto;
import com.swasthai.report_generator.test.masterdata.loader.MasterDataPayload;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates master data before database mutations.
 * Enforces structural integrity, referential consistency, calculation prerequisites,
 * circular dependency prevention, and database conflict detection.
 */
@Component
@RequiredArgsConstructor
public class MasterDataValidator {

    private static final Logger log = LoggerFactory.getLogger(MasterDataValidator.class);

    private final Validator validator;
    private final TestCategoryRepository testCategoryRepository;
    private final TestRepository testRepository;
    private final TestParameterRepository testParameterRepository;
    private final CalculationEngine calculationEngine;
    private final MasterDataSeedProperties properties;

    public List<MasterDataConflict> validate(MasterDataPayload payload) {
        List<String> errors = new ArrayList<>();
        List<MasterDataConflict> conflicts = new ArrayList<>();

        if (payload == null) {
            return conflicts;
        }

        // 1. Bean Validation (Field Constraints)
        validateBeanConstraints(payload, errors);

        // 2. In-Memory Duplicate Detection
        Map<String, MasterCategorySeedDto> seedCategoriesByCode = validateCategoryDuplicates(payload.categories(), errors);
        Map<String, MasterTestSeedDto> seedTestsByCode = validateTestDuplicates(payload.tests(), errors);
        Map<String, List<MasterParameterSeedDto>> seedParametersByTestCode = validateParameterDuplicates(payload.parameters(), errors);

        // 3. Hierarchy & Reference Validation
        validateCategoryReferences(seedTestsByCode, seedCategoriesByCode, errors);
        validateTestReferences(seedParametersByTestCode, seedTestsByCode, errors);

        // 4. Calculation Configuration & Dependency Validation
        validateCalculationConfigurations(seedParametersByTestCode, seedTestsByCode, errors);

        // 5. Database Conflict Detection
        detectConflicts(payload, conflicts, errors);

        // If validation errors exist, abort immediately
        if (!errors.isEmpty()) {
            throw new MasterDataValidationException("MASTER DATA VALIDATION FAILED", errors);
        }

        return conflicts;
    }

    // ============================================================
    // 1. BEAN CONSTRAINT VALIDATION
    // ============================================================

    private void validateBeanConstraints(MasterDataPayload payload, List<String> errors) {
        if (payload.categories() != null) {
            for (MasterCategorySeedDto cat : payload.categories()) {
                Set<ConstraintViolation<MasterCategorySeedDto>> violations = validator.validate(cat);
                for (ConstraintViolation<MasterCategorySeedDto> v : violations) {
                    errors.add(String.format("File '%s' - Category '%s': %s",
                            cat.sourceFile(), cat.code(), v.getMessage()));
                }
            }
        }

        if (payload.tests() != null) {
            for (MasterTestSeedDto test : payload.tests()) {
                Set<ConstraintViolation<MasterTestSeedDto>> violations = validator.validate(test);
                for (ConstraintViolation<MasterTestSeedDto> v : violations) {
                    errors.add(String.format("File '%s' - Test '%s': %s",
                            test.sourceFile(), test.code(), v.getMessage()));
                }
            }
        }

        if (payload.parameters() != null) {
            for (MasterParameterSeedDto param : payload.parameters()) {
                Set<ConstraintViolation<MasterParameterSeedDto>> violations = validator.validate(param);
                for (ConstraintViolation<MasterParameterSeedDto> v : violations) {
                    errors.add(String.format("File '%s' - Parameter '%s' for Test '%s': %s",
                            param.sourceFile(), param.code(), param.testCode(), v.getMessage()));
                }
            }
        }
    }

    // ============================================================
    // 2. IN-MEMORY DUPLICATE DETECTION
    // ============================================================

    private Map<String, MasterCategorySeedDto> validateCategoryDuplicates(
            List<MasterCategorySeedDto> categories,
            List<String> errors
    ) {
        Map<String, MasterCategorySeedDto> map = new HashMap<>();
        if (categories == null) return map;

        for (MasterCategorySeedDto cat : categories) {
            if (cat.code() == null || cat.code().isBlank()) continue;
            String normalizedCode = cat.code().trim().toUpperCase();
            if (map.containsKey(normalizedCode)) {
                errors.add(String.format("File '%s' - Duplicate category code: '%s'",
                        cat.sourceFile(), normalizedCode));
            } else {
                map.put(normalizedCode, cat);
            }
        }
        return map;
    }

    private Map<String, MasterTestSeedDto> validateTestDuplicates(
            List<MasterTestSeedDto> tests,
            List<String> errors
    ) {
        Map<String, MasterTestSeedDto> map = new HashMap<>();
        if (tests == null) return map;

        for (MasterTestSeedDto test : tests) {
            if (test.code() == null || test.code().isBlank()) continue;
            String normalizedCode = test.code().trim().toUpperCase();
            if (map.containsKey(normalizedCode)) {
                errors.add(String.format("File '%s' - Duplicate test code: '%s'",
                        test.sourceFile(), normalizedCode));
            } else {
                map.put(normalizedCode, test);
            }
        }
        return map;
    }

    private Map<String, List<MasterParameterSeedDto>> validateParameterDuplicates(
            List<MasterParameterSeedDto> parameters,
            List<String> errors
    ) {
        Map<String, List<MasterParameterSeedDto>> byTest = new HashMap<>();
        Set<String> seenKeys = new HashSet<>();

        if (parameters == null) return byTest;

        for (MasterParameterSeedDto param : parameters) {
            if (param.code() == null || param.code().isBlank()
                    || param.testCode() == null || param.testCode().isBlank()) {
                continue;
            }
            String normTest = param.testCode().trim().toUpperCase();
            String normParam = param.code().trim().toUpperCase();
            String compositeKey = normTest + ":" + normParam;

            if (!seenKeys.add(compositeKey)) {
                errors.add(String.format("File '%s' - Duplicate parameter code '%s' for test '%s'",
                        param.sourceFile(), normParam, normTest));
            }

            byTest.computeIfAbsent(normTest, k -> new ArrayList<>()).add(param);
        }
        return byTest;
    }

    // ============================================================
    // 3. HIERARCHY & REFERENCE VALIDATION
    // ============================================================

    private void validateCategoryReferences(
            Map<String, MasterTestSeedDto> seedTestsByCode,
            Map<String, MasterCategorySeedDto> seedCategoriesByCode,
            List<String> errors
    ) {
        for (MasterTestSeedDto test : seedTestsByCode.values()) {
            if (test.categoryCode() == null || test.categoryCode().isBlank()) continue;
            String normCategoryCode = test.categoryCode().trim().toUpperCase();

            boolean existsInSeed = seedCategoriesByCode.containsKey(normCategoryCode);
            boolean existsInDb = testCategoryRepository.existsByCode(normCategoryCode);

            if (!existsInSeed && !existsInDb) {
                errors.add(String.format("File '%s' - Test '%s' references missing category code: '%s'",
                        test.sourceFile(), test.code(), normCategoryCode));
            }
        }
    }

    private void validateTestReferences(
            Map<String, List<MasterParameterSeedDto>> seedParametersByTestCode,
            Map<String, MasterTestSeedDto> seedTestsByCode,
            List<String> errors
    ) {
        for (Map.Entry<String, List<MasterParameterSeedDto>> entry : seedParametersByTestCode.entrySet()) {
            String testCode = entry.getKey();
            boolean existsInSeed = seedTestsByCode.containsKey(testCode);
            boolean existsInDb = testRepository.existsByCode(testCode);

            if (!existsInSeed && !existsInDb) {
                for (MasterParameterSeedDto param : entry.getValue()) {
                    errors.add(String.format("File '%s' - Parameter '%s' references missing test code: '%s'",
                            param.sourceFile(), param.code(), testCode));
                }
            }
        }
    }

    // ============================================================
    // 4. CALCULATION & DEPENDENCY VALIDATION
    // ============================================================

    private void validateCalculationConfigurations(
            Map<String, List<MasterParameterSeedDto>> seedParametersByTestCode,
            Map<String, MasterTestSeedDto> seedTestsByCode,
            List<String> errors
    ) {
        for (Map.Entry<String, List<MasterParameterSeedDto>> entry : seedParametersByTestCode.entrySet()) {
            String testCode = entry.getKey();
            List<MasterParameterSeedDto> testParams = entry.getValue();

            // Collect all parameter codes available for this test (seed + DB)
            Set<String> availableCodes = new HashSet<>();
            Map<String, String> canonicalMap = new HashMap<>();

            for (MasterParameterSeedDto p : testParams) {
                if (p.code() != null) {
                    String norm = p.code().trim().toUpperCase();
                    availableCodes.add(norm);
                    String canon = ClinicalParameterAliases.getCanonical(norm);
                    canonicalMap.put(norm, canon);
                    if (canon != null) availableCodes.add(canon);
                }
            }

            // Also check DB parameters if test exists in DB
            Optional<Test> existingTestOpt = testRepository.findByCode(testCode);
            if (existingTestOpt.isPresent()) {
                List<TestParameter> dbParams = testParameterRepository
                        .findAllByTest_IdOrderByDisplayOrderAsc(existingTestOpt.get().getId());
                for (TestParameter dbP : dbParams) {
                    if (dbP.getCode() != null) {
                        String norm = dbP.getCode().trim().toUpperCase();
                        availableCodes.add(norm);
                        String canon = ClinicalParameterAliases.getCanonical(norm);
                        if (canon != null) availableCodes.add(canon);
                    }
                }
            }

            // Validate individual parameters
            List<TestParameter> calculatedEntities = new ArrayList<>();

            for (MasterParameterSeedDto param : testParams) {
                ParameterInputType inputType = param.getEffectiveInputType();
                CalculationType calcType = param.getEffectiveCalculationType();
                String normParamCode = param.code() != null ? param.code().trim().toUpperCase() : "";

                if (inputType == ParameterInputType.MANUAL) {
                    if (calcType != CalculationType.NONE) {
                        errors.add(String.format(
                                "File '%s' - Test '%s', Parameter '%s': Manual parameter must have calculation type NONE",
                                param.sourceFile(), testCode, normParamCode
                        ));
                    }
                } else if (inputType == ParameterInputType.CALCULATED) {
                    if (calcType == CalculationType.NONE) {
                        errors.add(String.format(
                                "File '%s' - Test '%s', Parameter '%s': Calculated parameter must have a valid calculation type",
                                param.sourceFile(), testCode, normParamCode
                        ));
                        continue;
                    }

                    if (calculationEngine != null && !calculationEngine.isSupported(calcType)) {
                        errors.add(String.format(
                                "File '%s' - Test '%s', Parameter '%s': Unsupported calculation type '%s'",
                                param.sourceFile(), testCode, normParamCode, calcType
                        ));
                        continue;
                    }

                    // Clinical compatibility
                    CalculationType inferred = ClinicalParameterAliases.inferFromCode(normParamCode);
                    if (inferred != null && inferred != calcType) {
                        errors.add(String.format(
                                "File '%s' - Test '%s', Parameter '%s': Calculation type '%s' is not clinically compatible with code. Did you mean %s?",
                                param.sourceFile(), testCode, normParamCode, calcType, inferred
                        ));
                    }

                    // Data type compatibility
                    if (param.dataType() != null) {
                        if (calculationEngine != null && !calculationEngine.isResultDataTypeSupported(calcType, param.dataType())) {
                            errors.add(String.format(
                                    "File '%s' - Test '%s', Parameter '%s': Data type '%s' is not supported for calculation type '%s'",
                                    param.sourceFile(), testCode, normParamCode, param.dataType(), calcType
                            ));
                        } else if (param.dataType() != TestParameterDataType.DECIMAL && param.dataType() != TestParameterDataType.INTEGER) {
                            errors.add(String.format(
                                    "File '%s' - Test '%s', Parameter '%s': Calculated parameter must have numeric data type",
                                    param.sourceFile(), testCode, normParamCode
                            ));
                        }
                    }

                    // Check required dependency parameters
                    if (calculationEngine != null) {
                        Set<String> reqParameters = calculationEngine.getRequiredParameters(calcType);
                        for (String reqCode : reqParameters) {
                            String normReq = reqCode.trim().toUpperCase();
                            String canonReq = ClinicalParameterAliases.getCanonical(normReq);
                            if (!availableCodes.contains(normReq) && (canonReq == null || !availableCodes.contains(canonReq))) {
                                errors.add(String.format(
                                        "File '%s' - Test '%s', Parameter '%s': Missing required calculation dependency '%s'",
                                        param.sourceFile(), testCode, normParamCode, reqCode
                                        ));
                            }
                        }
                    }

                    // Prepare entity for circular dependency check
                    TestParameter tp = new TestParameter();
                    tp.setCode(normParamCode);
                    tp.setInputType(ParameterInputType.CALCULATED);
                    tp.setCalculationType(calcType);
                    calculatedEntities.add(tp);
                }

                // Range validation
                validateRangeSanity(param, testCode, errors);
            }

            // Circular dependency detection
            if (calculatedEntities.size() > 1 && calculationEngine != null) {
                try {
                    CalculationDependencyResolver.resolveParameterOrder(calculatedEntities, calculationEngine);
                } catch (Exception ex) {
                    errors.add(String.format(
                            "Test '%s': Circular calculation dependency detected among parameters: %s",
                            testCode, ex.getMessage()
                    ));
                }
            }
        }
    }

    private void validateRangeSanity(MasterParameterSeedDto param, String testCode, List<String> errors) {
        if (param.referenceMin() != null && param.referenceMax() != null) {
            if (param.referenceMin().compareTo(param.referenceMax()) > 0) {
                errors.add(String.format(
                        "File '%s' - Test '%s', Parameter '%s': referenceMin (%s) cannot exceed referenceMax (%s)",
                        param.sourceFile(), testCode, param.code(), param.referenceMin(), param.referenceMax()
                ));
            }
        }
        if (param.criticalLow() != null && param.criticalHigh() != null) {
            if (param.criticalLow().compareTo(param.criticalHigh()) > 0) {
                errors.add(String.format(
                        "File '%s' - Test '%s', Parameter '%s': criticalLow (%s) cannot exceed criticalHigh (%s)",
                        param.sourceFile(), testCode, param.code(), param.criticalLow(), param.criticalHigh()
                ));
            }
        }
    }

    // ============================================================
    // 5. DATABASE CONFLICT DETECTION
    // ============================================================

    private void detectConflicts(
            MasterDataPayload payload,
            List<MasterDataConflict> conflicts,
            List<String> errors
    ) {
        // Categories
        if (payload.categories() != null) {
            for (MasterCategorySeedDto seed : payload.categories()) {
                if (seed.code() == null) continue;
                String code = seed.code().trim().toUpperCase();
                Optional<TestCategory> dbCatOpt = testCategoryRepository.findByCode(code);
                if (dbCatOpt.isPresent()) {
                    TestCategory dbCat = dbCatOpt.get();
                    if (seed.name() != null && !seed.name().trim().equalsIgnoreCase(dbCat.getName())) {
                        conflicts.add(MasterDataConflict.builder()
                                .entityType("Category")
                                .code(code)
                                .field("name")
                                .existingValue(dbCat.getName())
                                .seedValue(seed.name().trim())
                                .build());
                    }
                }
            }
        }

        // Tests
        if (payload.tests() != null) {
            for (MasterTestSeedDto seed : payload.tests()) {
                if (seed.code() == null) continue;
                String code = seed.code().trim().toUpperCase();
                Optional<Test> dbTestOpt = testRepository.findByCode(code);
                if (dbTestOpt.isPresent()) {
                    Test dbTest = dbTestOpt.get();
                    if (seed.name() != null && !seed.name().trim().equalsIgnoreCase(dbTest.getName())) {
                        conflicts.add(MasterDataConflict.builder()
                                .entityType("Test")
                                .code(code)
                                .field("name")
                                .existingValue(dbTest.getName())
                                .seedValue(seed.name().trim())
                                .build());
                    }
                    if (seed.sampleType() != null && seed.sampleType() != dbTest.getSampleType()) {
                        conflicts.add(MasterDataConflict.builder()
                                .entityType("Test")
                                .code(code)
                                .field("sampleType")
                                .existingValue(dbTest.getSampleType())
                                .seedValue(seed.sampleType())
                                .build());
                    }
                    if (seed.testType() != null && seed.testType() != dbTest.getTestType()) {
                        conflicts.add(MasterDataConflict.builder()
                                .entityType("Test")
                                .code(code)
                                .field("testType")
                                .existingValue(dbTest.getTestType())
                                .seedValue(seed.testType())
                                .build());
                    }
                    if (seed.categoryCode() != null && dbTest.getCategory() != null) {
                        String dbCategoryCode = dbTest.getCategory().getCode();
                        String seedCategoryCode = seed.categoryCode().trim().toUpperCase();
                        if (dbCategoryCode != null && !dbCategoryCode.equalsIgnoreCase(seedCategoryCode)) {
                            conflicts.add(MasterDataConflict.builder()
                                    .entityType("Test")
                                    .code(code)
                                    .field("category")
                                    .existingValue(dbCategoryCode)
                                    .seedValue(seedCategoryCode)
                                    .build());
                        }
                    }
                }
            }
        }

        // Parameters
        if (payload.parameters() != null) {
            for (MasterParameterSeedDto seed : payload.parameters()) {
                if (seed.code() == null || seed.testCode() == null) continue;
                String testCode = seed.testCode().trim().toUpperCase();
                String paramCode = seed.code().trim().toUpperCase();

                Optional<Test> dbTestOpt = testRepository.findByCode(testCode);
                if (dbTestOpt.isPresent()) {
                    Optional<TestParameter> dbParamOpt = testParameterRepository
                            .findByTest_IdAndCode(dbTestOpt.get().getId(), paramCode);
                    if (dbParamOpt.isPresent()) {
                        TestParameter dbParam = dbParamOpt.get();
                        if (seed.name() != null && !seed.name().trim().equalsIgnoreCase(dbParam.getName())) {
                            conflicts.add(MasterDataConflict.builder()
                                    .entityType("TestParameter")
                                    .code(paramCode)
                                    .relatedCode(testCode)
                                    .field("name")
                                    .existingValue(dbParam.getName())
                                    .seedValue(seed.name().trim())
                                    .build());
                        }
                        if (seed.dataType() != null && seed.dataType() != dbParam.getDataType()) {
                            conflicts.add(MasterDataConflict.builder()
                                    .entityType("TestParameter")
                                    .code(paramCode)
                                    .relatedCode(testCode)
                                    .field("dataType")
                                    .existingValue(dbParam.getDataType())
                                    .seedValue(seed.dataType())
                                    .build());
                        }
                        if (seed.inputType() != null && seed.inputType() != dbParam.getInputType()) {
                            conflicts.add(MasterDataConflict.builder()
                                    .entityType("TestParameter")
                                    .code(paramCode)
                                    .relatedCode(testCode)
                                    .field("inputType")
                                    .existingValue(dbParam.getInputType())
                                    .seedValue(seed.inputType())
                                    .build());
                        }
                        if (seed.calculationType() != null && seed.calculationType() != dbParam.getCalculationType()) {
                            conflicts.add(MasterDataConflict.builder()
                                    .entityType("TestParameter")
                                    .code(paramCode)
                                    .relatedCode(testCode)
                                    .field("calculationType")
                                    .existingValue(dbParam.getCalculationType())
                                    .seedValue(seed.calculationType())
                                    .build());
                        }
                    }
                }
            }
        }

        if (!conflicts.isEmpty()) {
            for (MasterDataConflict c : conflicts) {
                log.warn("MASTER DATA CONFLICT DETECTED: {}", c);
            }
            if (properties.isFailOnConflict()) {
                for (MasterDataConflict c : conflicts) {
                    errors.add(c.toString());
                }
            }
        }
    }
}

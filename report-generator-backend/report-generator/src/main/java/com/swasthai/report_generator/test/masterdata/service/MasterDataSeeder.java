package com.swasthai.report_generator.test.masterdata.service;

import com.swasthai.report_generator.test.entity.Test;
import com.swasthai.report_generator.test.entity.TestCategory;
import com.swasthai.report_generator.test.entity.TestParameter;
import com.swasthai.report_generator.test.masterdata.dto.MasterCategorySeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterParameterSeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterTestSeedDto;
import com.swasthai.report_generator.test.masterdata.loader.MasterDataLoader;
import com.swasthai.report_generator.test.masterdata.loader.MasterDataPayload;
import com.swasthai.report_generator.test.masterdata.validation.MasterDataConflict;
import com.swasthai.report_generator.test.masterdata.validation.MasterDataValidationException;
import com.swasthai.report_generator.test.masterdata.validation.MasterDataValidator;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Executes master data seeding with strict idempotence, atomic transactions,
 * and comprehensive pre-validation.
 */
@Service
@RequiredArgsConstructor
public class MasterDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(MasterDataSeeder.class);

    private final MasterDataLoader loader;
    private final MasterDataValidator validator;
    private final TestCategoryRepository testCategoryRepository;
    private final TestRepository testRepository;
    private final TestParameterRepository testParameterRepository;

    @Transactional
    public MasterDataSeedSummary seed() {
        log.info("Starting Master Data Seeding...");

        // 1. Load All JSON Data
        MasterDataPayload payload = loader.load();

        // 2. Comprehensive Pre-Mutation Validation
        List<MasterDataConflict> conflicts = validator.validate(payload);

        // 3. Perform Idempotent Database Mutations
        int categoriesFound = payload.categories() != null ? payload.categories().size() : 0;
        int testsFound = payload.tests() != null ? payload.tests().size() : 0;
        int parametersFound = payload.parameters() != null ? payload.parameters().size() : 0;

        int categoriesInserted = 0;
        int categoriesSkipped = 0;

        int testsInserted = 0;
        int testsSkipped = 0;

        int parametersInserted = 0;
        int parametersSkipped = 0;

        // Categories
        java.util.Map<String, TestCategory> resolvedCategories = new java.util.HashMap<>();
        if (payload.categories() != null) {
            for (MasterCategorySeedDto dto : payload.categories()) {
                String code = dto.code().trim().toUpperCase();
                Optional<TestCategory> existing = testCategoryRepository.findByCode(code);
                if (existing.isPresent()) {
                    resolvedCategories.put(code, existing.get());
                    categoriesSkipped++;
                } else {
                    TestCategory category = TestCategory.builder()
                            .code(code)
                            .name(dto.name().trim())
                            .description(dto.description() != null ? dto.description().trim() : null)
                            .status(dto.getEffectiveStatus())
                            .build();
                    TestCategory saved = testCategoryRepository.save(category);
                    resolvedCategories.put(code, saved != null ? saved : category);
                    categoriesInserted++;
                }
            }
        }

        // Tests
        java.util.Map<String, Test> resolvedTests = new java.util.HashMap<>();
        if (payload.tests() != null) {
            for (MasterTestSeedDto dto : payload.tests()) {
                String code = dto.code().trim().toUpperCase();
                Optional<Test> existing = testRepository.findByCode(code);
                if (existing.isPresent()) {
                    resolvedTests.put(code, existing.get());
                    testsSkipped++;
                } else {
                    String categoryCode = dto.categoryCode().trim().toUpperCase();
                    TestCategory category = resolvedCategories.containsKey(categoryCode)
                            ? resolvedCategories.get(categoryCode)
                            : testCategoryRepository.findByCode(categoryCode)
                                    .orElseThrow(() -> new MasterDataValidationException(
                                            String.format("Internal error: Category '%s' could not be resolved for test '%s'", categoryCode, code)));

                    Test test = Test.builder()
                            .category(category)
                            .code(code)
                            .name(dto.name().trim())
                            .shortName(dto.shortName() != null ? dto.shortName().trim() : null)
                            .testType(dto.getEffectiveTestType())
                            .description(dto.description() != null ? dto.description().trim() : null)
                            .sampleType(dto.sampleType())
                            .customSampleType(dto.customSampleType() != null ? dto.customSampleType().trim() : null)
                            .specimenContainer(dto.specimenContainer() != null ? dto.specimenContainer().trim() : null)
                            .sampleVolume(dto.sampleVolume())
                            .sampleVolumeUnit(dto.sampleVolumeUnit() != null ? dto.sampleVolumeUnit().trim() : null)
                            .fastingRequired(dto.isEffectiveFastingRequired())
                            .patientPreparation(dto.patientPreparation() != null ? dto.patientPreparation().trim() : null)
                            .collectionInstructions(dto.collectionInstructions() != null ? dto.collectionInstructions().trim() : null)
                            .turnaroundTimeHours(dto.turnaroundTimeHours())
                            .prioritySupported(dto.isEffectivePrioritySupported())
                            .outsourced(dto.isEffectiveOutsourced())
                            .laboratoryInstructions(dto.laboratoryInstructions() != null ? dto.laboratoryInstructions().trim() : null)
                            .reportSection(dto.reportSection() != null ? dto.reportSection().trim() : null)
                            .displayOrder(dto.getEffectiveDisplayOrder())
                            .reportDescription(dto.reportDescription() != null ? dto.reportDescription().trim() : null)
                            .interpretationGuidance(dto.interpretationGuidance() != null ? dto.interpretationGuidance().trim() : null)
                            .basePrice(dto.basePrice())
                            .currency(dto.getEffectiveCurrency())
                            .billingCode(dto.billingCode() != null ? dto.billingCode().trim().toUpperCase() : null)
                            .status(dto.getEffectiveStatus())
                            .build();

                    Test saved = testRepository.save(test);
                    resolvedTests.put(code, saved != null ? saved : test);
                    testsInserted++;
                }
            }
        }

        // Parameters
        if (payload.parameters() != null) {
            for (MasterParameterSeedDto dto : payload.parameters()) {
                String testCode = dto.testCode().trim().toUpperCase();
                String paramCode = dto.code().trim().toUpperCase();

                Test test = resolvedTests.containsKey(testCode)
                        ? resolvedTests.get(testCode)
                        : testRepository.findByCode(testCode)
                                .orElseThrow(() -> new MasterDataValidationException(
                                        String.format("Internal error: Test '%s' could not be resolved for parameter '%s'", testCode, paramCode)));

                Optional<TestParameter> existing = test.getId() != null
                        ? testParameterRepository.findByTest_IdAndCode(test.getId(), paramCode)
                        : Optional.empty();

                if (existing.isPresent()) {
                    parametersSkipped++;
                } else {
                    TestParameter parameter = TestParameter.builder()
                            .test(test)
                            .code(paramCode)
                            .name(dto.name().trim())
                            .description(dto.description() != null ? dto.description().trim() : null)
                            .dataType(dto.dataType())
                            .inputType(dto.getEffectiveInputType())
                            .calculationType(dto.getEffectiveCalculationType())
                            .unit(dto.unit() != null ? dto.unit().trim() : null)
                            .required(dto.isEffectiveRequired())
                            .displayOrder(dto.displayOrder())
                            .referenceMin(dto.referenceMin())
                            .referenceMax(dto.referenceMax())
                            .criticalLow(dto.criticalLow())
                            .criticalHigh(dto.criticalHigh())
                            .reportDescription(dto.reportDescription() != null ? dto.reportDescription().trim() : null)
                            .interpretationGuidance(dto.interpretationGuidance() != null ? dto.interpretationGuidance().trim() : null)
                            .status(dto.getEffectiveStatus())
                            .build();

                    testParameterRepository.save(parameter);
                    parametersInserted++;
                }
            }
        }

        MasterDataSeedSummary summary = MasterDataSeedSummary.builder()
                .categoriesFound(categoriesFound)
                .categoriesInserted(categoriesInserted)
                .categoriesSkipped(categoriesSkipped)
                .testsFound(testsFound)
                .testsInserted(testsInserted)
                .testsSkipped(testsSkipped)
                .parametersFound(parametersFound)
                .parametersInserted(parametersInserted)
                .parametersSkipped(parametersSkipped)
                .conflicts(conflicts)
                .validationPassed(true)
                .build();

        log.info("{}", summary.toFormattedSummary());
        return summary;
    }
}

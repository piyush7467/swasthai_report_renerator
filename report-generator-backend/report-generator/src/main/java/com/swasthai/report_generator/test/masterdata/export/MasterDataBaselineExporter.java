package com.swasthai.report_generator.test.masterdata.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.swasthai.report_generator.test.entity.Test;
import com.swasthai.report_generator.test.entity.TestCategory;
import com.swasthai.report_generator.test.entity.TestParameter;
import com.swasthai.report_generator.test.masterdata.dto.MasterCategorySeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterParameterSeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterTestSeedDto;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utility to safely export existing valid database master data into JSON format
 * matching the seeder JSON structure without overwriting source seed files.
 */
@Component
@RequiredArgsConstructor
public class MasterDataBaselineExporter {

    private static final Logger log = LoggerFactory.getLogger(MasterDataBaselineExporter.class);

    private final TestCategoryRepository testCategoryRepository;
    private final TestRepository testRepository;
    private final TestParameterRepository testParameterRepository;
    private final ObjectMapper objectMapper;

    public record ExportSummary(
            int categoriesExported,
            int testsExported,
            int parametersExported,
            Path outputDirectory
    ) {}

    public ExportSummary exportToDirectory(Path outputDir) throws IOException {
        if (outputDir == null) {
            outputDir = Paths.get("target", "exported-master-data");
        }

        Files.createDirectories(outputDir);
        Path paramDir = outputDir.resolve("test-parameters");
        Files.createDirectories(paramDir);

        ObjectMapper prettyMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);

        // 1. Export valid categories
        List<TestCategory> dbCategories = testCategoryRepository.findAll();
        List<MasterCategorySeedDto> exportedCategories = new ArrayList<>();
        for (TestCategory cat : dbCategories) {
            if (cat.getCode() != null && !cat.getCode().isBlank() && cat.getName() != null) {
                exportedCategories.add(MasterCategorySeedDto.builder()
                        .code(cat.getCode().trim().toUpperCase())
                        .name(cat.getName().trim())
                        .description(cat.getDescription())
                        .status(cat.getStatus())
                        .build());
            }
        }
        Path categoriesFile = outputDir.resolve("categories.json");
        prettyMapper.writeValue(categoriesFile.toFile(), exportedCategories);

        // 2. Export valid tests
        List<Test> dbTests = testRepository.findAll();
        List<MasterTestSeedDto> exportedTests = new ArrayList<>();
        Map<UUID, Test> validTestsById = new HashMap<>();

        for (Test test : dbTests) {
            if (test.getCode() != null && !test.getCode().isBlank()
                    && test.getCategory() != null && test.getCategory().getCode() != null
                    && test.getName() != null && test.getSampleType() != null) {

                validTestsById.put(test.getId(), test);
                exportedTests.add(MasterTestSeedDto.builder()
                        .code(test.getCode().trim().toUpperCase())
                        .name(test.getName().trim())
                        .shortName(test.getShortName())
                        .categoryCode(test.getCategory().getCode().trim().toUpperCase())
                        .testType(test.getTestType())
                        .description(test.getDescription())
                        .sampleType(test.getSampleType())
                        .customSampleType(test.getCustomSampleType())
                        .specimenContainer(test.getSpecimenContainer())
                        .sampleVolume(test.getSampleVolume())
                        .sampleVolumeUnit(test.getSampleVolumeUnit())
                        .fastingRequired(test.isFastingRequired())
                        .patientPreparation(test.getPatientPreparation())
                        .collectionInstructions(test.getCollectionInstructions())
                        .turnaroundTimeHours(test.getTurnaroundTimeHours())
                        .prioritySupported(test.isPrioritySupported())
                        .outsourced(test.isOutsourced())
                        .laboratoryInstructions(test.getLaboratoryInstructions())
                        .reportSection(test.getReportSection())
                        .displayOrder(test.getDisplayOrder())
                        .reportDescription(test.getReportDescription())
                        .interpretationGuidance(test.getInterpretationGuidance())
                        .basePrice(test.getBasePrice())
                        .currency(test.getCurrency())
                        .billingCode(test.getBillingCode())
                        .status(test.getStatus())
                        .build());
            }
        }
        Path testsFile = outputDir.resolve("tests.json");
        prettyMapper.writeValue(testsFile.toFile(), exportedTests);

        // 3. Export valid parameters grouped by test
        List<TestParameter> dbParams = testParameterRepository.findAll();
        Map<String, List<MasterParameterSeedDto>> paramsByTestCode = new HashMap<>();
        int parametersExported = 0;

        for (TestParameter p : dbParams) {
            if (p.getCode() != null && !p.getCode().isBlank()
                    && p.getTest() != null && validTestsById.containsKey(p.getTest().getId())
                    && p.getName() != null && p.getDataType() != null) {

                String testCode = p.getTest().getCode().trim().toUpperCase();
                MasterParameterSeedDto dto = MasterParameterSeedDto.builder()
                        .code(p.getCode().trim().toUpperCase())
                        .testCode(testCode)
                        .name(p.getName().trim())
                        .description(p.getDescription())
                        .dataType(p.getDataType())
                        .inputType(p.getInputType())
                        .calculationType(p.getCalculationType())
                        .unit(p.getUnit())
                        .required(p.isRequired())
                        .displayOrder(p.getDisplayOrder() != null ? p.getDisplayOrder() : 1)
                        .referenceMin(p.getReferenceMin())
                        .referenceMax(p.getReferenceMax())
                        .criticalLow(p.getCriticalLow())
                        .criticalHigh(p.getCriticalHigh())
                        .reportDescription(p.getReportDescription())
                        .interpretationGuidance(p.getInterpretationGuidance())
                        .status(p.getStatus())
                        .build();

                paramsByTestCode.computeIfAbsent(testCode, k -> new ArrayList<>()).add(dto);
                parametersExported++;
            }
        }

        for (Map.Entry<String, List<MasterParameterSeedDto>> entry : paramsByTestCode.entrySet()) {
            String testCode = entry.getKey();
            Path pFile = paramDir.resolve(testCode.toLowerCase() + ".json");
            prettyMapper.writeValue(pFile.toFile(), entry.getValue());
        }

        ExportSummary summary = new ExportSummary(
                exportedCategories.size(),
                exportedTests.size(),
                parametersExported,
                outputDir
        );

        log.info("Safe Master Data Export completed to: {}\nCategories: {}, Tests: {}, Parameters: {}",
                outputDir.toAbsolutePath(),
                summary.categoriesExported(),
                summary.testsExported(),
                summary.parametersExported());

        return summary;
    }
}

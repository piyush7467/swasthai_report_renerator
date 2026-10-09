package com.swasthai.report_generator.test.masterdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.calculators.*;
import com.swasthai.report_generator.test.calculation.impl.CalculationEngineImpl;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.entity.Test;
import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditReport;
import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditor;
import com.swasthai.report_generator.test.masterdata.audit.MasterDataAuditIssue;
import com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties;
import com.swasthai.report_generator.test.masterdata.dto.MasterCategorySeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterParameterSeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterTestSeedDto;
import com.swasthai.report_generator.test.masterdata.export.MasterDataBaselineExporter;
import com.swasthai.report_generator.test.masterdata.loader.MasterDataLoader;
import com.swasthai.report_generator.test.masterdata.loader.MasterDataPayload;
import com.swasthai.report_generator.test.masterdata.runner.MasterDataAuditRunner;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeedSummary;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeeder;
import com.swasthai.report_generator.test.masterdata.validation.MasterDataValidationException;
import com.swasthai.report_generator.test.masterdata.validation.MasterDataValidator;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite verifying the 25 Master Data Audit and Migration requirements
 * specified in Section 27.
 */
@ExtendWith(MockitoExtension.class)
class ExistingMasterDataAuditTest {

    @Mock
    private TestCategoryRepository testCategoryRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestParameterRepository testParameterRepository;

    @Mock
    private MasterDataLoader loader;

    private CalculationEngine calculationEngine;
    private Validator validator;
    private MasterDataSeedProperties properties;
    private MasterDataValidator masterDataValidator;
    private MasterDataSeeder seeder;
    private ExistingMasterDataAuditor auditor;
    private MasterDataAuditRunner auditRunner;
    private MasterDataBaselineExporter exporter;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();

        calculationEngine = new CalculationEngineImpl(List.of(
                new GlobulinCalculator(),
                new AGRatioCalculator(),
                new VLDLCalculator(),
                new LDLFriedewaldCalculator(),
                new LDLtoHDLratioCalculator(),
                new MCVCalculator(),
                new MCHCalculator(),
                new MCHCCalculator(),
                new IndirectBilirubinCalculator()
        ));

        properties = new MasterDataSeedProperties();
        properties.setEnabled(true);
        properties.setFailOnConflict(false);
        properties.setAuditOnStartup(false);

        masterDataValidator = new MasterDataValidator(
                validator,
                testCategoryRepository,
                testRepository,
                testParameterRepository,
                calculationEngine,
                properties
        );

        seeder = new MasterDataSeeder(
                loader,
                masterDataValidator,
                testCategoryRepository,
                testRepository,
                testParameterRepository
        );

        auditor = new ExistingMasterDataAuditor(
                testCategoryRepository,
                testRepository,
                testParameterRepository,
                calculationEngine
        );

        auditRunner = new MasterDataAuditRunner(properties, auditor);
        exporter = new MasterDataBaselineExporter(
                testCategoryRepository,
                testRepository,
                testParameterRepository,
                new ObjectMapper()
        );
    }

    // ============================================================
    // 1. Existing category matched by code
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("1. Existing category matched by code: skips re-inserting and preserves entity")
    void test1_ExistingCategoryMatchedByCode() {
        TestCategory existing = TestCategory.builder()
                .id(UUID.randomUUID())
                .code("HEM")
                .name("Hematology")
                .refId("TC-123456")
                .build();

        when(testCategoryRepository.findByCode("HEM")).thenReturn(Optional.of(existing));
        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .categories(List.of(MasterCategorySeedDto.builder().code("HEM").name("Hematology").build()))
                .build());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.categoriesFound()).isEqualTo(1);
        assertThat(summary.categoriesInserted()).isZero();
        assertThat(summary.categoriesSkipped()).isEqualTo(1);
        verify(testCategoryRepository, never()).save(any());
    }

    // ============================================================
    // 2. Existing category refId preserved
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("2. Existing category refId preserved: public refId remains untouched")
    void test2_ExistingCategoryRefIdPreserved() {
        String existingRefId = "TC-ORIGINAL-999";
        TestCategory existing = TestCategory.builder()
                .id(UUID.randomUUID())
                .code("BIO")
                .name("Biochemistry")
                .refId(existingRefId)
                .build();

        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(existing));
        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .categories(List.of(MasterCategorySeedDto.builder().code("BIO").name("Biochemistry").build()))
                .build());

        seeder.seed();

        assertThat(existing.getRefId()).isEqualTo(existingRefId);
        verify(testCategoryRepository, never()).save(existing);
    }

    // ============================================================
    // 3. Existing test matched by code
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("3. Existing test matched by code: resolves via natural key, skipped")
    void test3_ExistingTestMatchedByCode() {
        TestCategory cat = TestCategory.builder().id(UUID.randomUUID()).code("HEM").name("Hematology").refId("TC-1").build();
        Test existingTest = Test.builder()
                .id(UUID.randomUUID())
                .code("CBC")
                .name("Complete Blood Count")
                .category(cat)
                .refId("TEST-CBC-001")
                .sampleType(SampleType.WHOLE_BLOOD)
                .build();

        when(testCategoryRepository.existsByCode("HEM")).thenReturn(true);
        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(existingTest));
        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .tests(List.of(MasterTestSeedDto.builder()
                        .code("CBC").name("Complete Blood Count").categoryCode("HEM")
                        .sampleType(SampleType.WHOLE_BLOOD).build()))
                .build());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.testsFound()).isEqualTo(1);
        assertThat(summary.testsInserted()).isZero();
        assertThat(summary.testsSkipped()).isEqualTo(1);
        verify(testRepository, never()).save(any());
    }

    // ============================================================
    // 4. Existing test refId preserved
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("4. Existing test refId preserved: public refId untouched")
    void test4_ExistingTestRefIdPreserved() {
        String existingTestRefId = "TEST-ORIGINAL-777";
        TestCategory cat = TestCategory.builder().id(UUID.randomUUID()).code("HEM").refId("TC-1").build();
        Test existingTest = Test.builder()
                .id(UUID.randomUUID())
                .code("CBC")
                .name("Complete Blood Count")
                .category(cat)
                .refId(existingTestRefId)
                .sampleType(SampleType.WHOLE_BLOOD)
                .build();

        when(testCategoryRepository.existsByCode("HEM")).thenReturn(true);
        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(existingTest));
        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .tests(List.of(MasterTestSeedDto.builder()
                        .code("CBC").name("Complete Blood Count").categoryCode("HEM")
                        .sampleType(SampleType.WHOLE_BLOOD).build()))
                .build());

        seeder.seed();

        assertThat(existingTest.getRefId()).isEqualTo(existingTestRefId);
        verify(testRepository, never()).save(existingTest);
    }

    // ============================================================
    // 5. Existing parameter matched by test + code
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("5. Existing parameter matched by test + code: natural composite key lookup")
    void test5_ExistingParameterMatchedByTestAndCode() {
        UUID testId = UUID.randomUUID();
        Test test = Test.builder().id(testId).code("CBC").refId("TEST-1").build();
        TestParameter param = TestParameter.builder()
                .id(UUID.randomUUID())
                .test(test)
                .code("WBC")
                .name("White Blood Cells")
                .refId("PARAM-WBC-001")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .build();

        when(testRepository.existsByCode("CBC")).thenReturn(true);
        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(test));
        when(testParameterRepository.findByTest_IdAndCode(testId, "WBC")).thenReturn(Optional.of(param));
        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .parameters(List.of(MasterParameterSeedDto.builder()
                        .testCode("CBC").code("WBC").name("White Blood Cells")
                        .dataType(TestParameterDataType.DECIMAL).inputType(ParameterInputType.MANUAL)
                        .displayOrder(1).build()))
                .build());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.parametersFound()).isEqualTo(1);
        assertThat(summary.parametersInserted()).isZero();
        assertThat(summary.parametersSkipped()).isEqualTo(1);
        verify(testParameterRepository, never()).save(any());
    }

    // ============================================================
    // 6. Existing parameter refId preserved
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("6. Existing parameter refId preserved: public refId intact")
    void test6_ExistingParameterRefIdPreserved() {
        String existingParamRefId = "PARAM-STABLE-333";
        UUID testId = UUID.randomUUID();
        Test test = Test.builder().id(testId).code("CBC").refId("TEST-1").build();
        TestParameter param = TestParameter.builder()
                .id(UUID.randomUUID())
                .test(test)
                .code("WBC")
                .name("White Blood Cells")
                .refId(existingParamRefId)
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .build();

        when(testRepository.existsByCode("CBC")).thenReturn(true);
        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(test));
        when(testParameterRepository.findByTest_IdAndCode(testId, "WBC")).thenReturn(Optional.of(param));
        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .parameters(List.of(MasterParameterSeedDto.builder()
                        .testCode("CBC").code("WBC").name("White Blood Cells")
                        .dataType(TestParameterDataType.DECIMAL).inputType(ParameterInputType.MANUAL)
                        .displayOrder(1).build()))
                .build());

        seeder.seed();

        assertThat(param.getRefId()).isEqualTo(existingParamRefId);
        verify(testParameterRepository, never()).save(param);
    }

    // ============================================================
    // 7. New category inserted
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("7. New category inserted: inserted when not present in DB")
    void test7_NewCategoryInserted() {
        when(testCategoryRepository.findByCode("IMM")).thenReturn(Optional.empty());
        TestCategory saved = TestCategory.builder().id(UUID.randomUUID()).code("IMM").name("Immunology").build();
        when(testCategoryRepository.save(any())).thenReturn(saved);

        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .categories(List.of(MasterCategorySeedDto.builder().code("IMM").name("Immunology").build()))
                .build());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.categoriesInserted()).isEqualTo(1);
        verify(testCategoryRepository, times(1)).save(any());
    }

    // ============================================================
    // 8. New test inserted
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("8. New test inserted: linked to parent category and saved")
    void test8_NewTestInserted() {
        TestCategory cat = TestCategory.builder().id(UUID.randomUUID()).code("BIO").name("Biochemistry").build();
        when(testCategoryRepository.existsByCode("BIO")).thenReturn(true);
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(cat));
        when(testRepository.findByCode("LFT")).thenReturn(Optional.empty());

        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .tests(List.of(MasterTestSeedDto.builder()
                        .code("LFT").name("Liver Function Test").categoryCode("BIO")
                        .sampleType(SampleType.SERUM).build()))
                .build());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.testsInserted()).isEqualTo(1);
        verify(testRepository, times(1)).save(any());
    }

    // ============================================================
    // 9. New parameter inserted
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("9. New parameter inserted: linked to test and saved")
    void test9_NewParameterInserted() {
        UUID testId = UUID.randomUUID();
        Test test = Test.builder().id(testId).code("CBC").name("Complete Blood Count").build();
        when(testRepository.existsByCode("CBC")).thenReturn(true);
        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(test));
        when(testParameterRepository.findByTest_IdAndCode(testId, "PLT")).thenReturn(Optional.empty());

        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .parameters(List.of(MasterParameterSeedDto.builder()
                        .testCode("CBC").code("PLT").name("Platelet Count")
                        .dataType(TestParameterDataType.INTEGER).inputType(ParameterInputType.MANUAL)
                        .displayOrder(1).build()))
                .build());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.parametersInserted()).isEqualTo(1);
        verify(testParameterRepository, times(1)).save(any());
    }

    // ============================================================
    // 10. Existing + new records together
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("10. Existing + new records together: correctly segregates skips and inserts")
    void test10_ExistingAndNewRecordsTogether() {
        TestCategory existingCat = TestCategory.builder().id(UUID.randomUUID()).code("HEM").name("Hematology").build();
        when(testCategoryRepository.findByCode("HEM")).thenReturn(Optional.of(existingCat));
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.empty());
        TestCategory savedBio = TestCategory.builder().id(UUID.randomUUID()).code("BIO").name("Biochemistry").build();
        when(testCategoryRepository.save(any())).thenReturn(savedBio);

        Test existingTest = Test.builder().id(UUID.randomUUID()).code("CBC").category(existingCat).name("CBC").sampleType(SampleType.WHOLE_BLOOD).build();
        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(existingTest));
        when(testRepository.findByCode("LFT")).thenReturn(Optional.empty());

        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .categories(List.of(
                        MasterCategorySeedDto.builder().code("HEM").name("Hematology").build(),
                        MasterCategorySeedDto.builder().code("BIO").name("Biochemistry").build()
                ))
                .tests(List.of(
                        MasterTestSeedDto.builder().code("CBC").name("CBC").categoryCode("HEM").sampleType(SampleType.WHOLE_BLOOD).build(),
                        MasterTestSeedDto.builder().code("LFT").name("LFT").categoryCode("BIO").sampleType(SampleType.SERUM).build()
                ))
                .build());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.categoriesInserted()).isEqualTo(1);
        assertThat(summary.categoriesSkipped()).isEqualTo(1);
        assertThat(summary.testsInserted()).isEqualTo(1);
        assertThat(summary.testsSkipped()).isEqualTo(1);
    }

    // ============================================================
    // 11. Exact seed rerun
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("11. Exact seed rerun: idempotent execution without duplicate inserts")
    void test11_ExactSeedRerun_Idempotent() {
        TestCategory cat = TestCategory.builder().id(UUID.randomUUID()).code("HEM").name("Hematology").build();
        Test test = Test.builder().id(UUID.randomUUID()).code("CBC").name("CBC").category(cat).sampleType(SampleType.WHOLE_BLOOD).build();

        when(testCategoryRepository.findByCode("HEM")).thenReturn(Optional.of(cat));
        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(test));

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(MasterCategorySeedDto.builder().code("HEM").name("Hematology").build()))
                .tests(List.of(MasterTestSeedDto.builder().code("CBC").name("CBC").categoryCode("HEM").sampleType(SampleType.WHOLE_BLOOD).build()))
                .build();
        when(loader.load()).thenReturn(payload);

        MasterDataSeedSummary firstRun = seeder.seed();
        assertThat(firstRun.categoriesSkipped()).isEqualTo(1);
        assertThat(firstRun.testsSkipped()).isEqualTo(1);
        assertThat(firstRun.categoriesInserted()).isZero();
        assertThat(firstRun.testsInserted()).isZero();

        MasterDataSeedSummary secondRun = seeder.seed();
        assertThat(secondRun.categoriesSkipped()).isEqualTo(1);
        assertThat(secondRun.testsSkipped()).isEqualTo(1);
        verify(testCategoryRepository, never()).save(any());
        verify(testRepository, never()).save(any());
    }

    // ============================================================
    // 12. Additive seed
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("12. Additive seed: adding new delta items touches only new items")
    void test12_AdditiveSeed() {
        TestCategory cat = TestCategory.builder().id(UUID.randomUUID()).code("HEM").name("Hematology").build();
        when(testCategoryRepository.findByCode("HEM")).thenReturn(Optional.of(cat));
        when(testRepository.findByCode("CBC")).thenReturn(Optional.empty());

        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .categories(List.of(MasterCategorySeedDto.builder().code("HEM").name("Hematology").build()))
                .tests(List.of(MasterTestSeedDto.builder().code("CBC").name("CBC").categoryCode("HEM").sampleType(SampleType.WHOLE_BLOOD).build()))
                .build());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.categoriesSkipped()).isEqualTo(1);
        assertThat(summary.categoriesInserted()).isZero();
        assertThat(summary.testsInserted()).isEqualTo(1);
        verify(testCategoryRepository, never()).save(any());
        verify(testRepository, times(1)).save(any());
    }

    // ============================================================
    // 13. Missing category code
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("13. Missing category code: audit reports MASTER DATA MIGRATION REQUIRED without guessing")
    void test13_MissingCategoryCode_Audited() {
        TestCategory corrupted = TestCategory.builder()
                .id(UUID.randomUUID())
                .name("Missing Code Category")
                .code(null)
                .refId("TC-CORRUPT-1")
                .build();

        when(testCategoryRepository.findAll()).thenReturn(List.of(corrupted));
        when(testRepository.findAll()).thenReturn(Collections.emptyList());
        when(testParameterRepository.findAll()).thenReturn(Collections.emptyList());

        ExistingMasterDataAuditReport report = auditor.audit();

        assertThat(report.categoriesMissingCode()).isEqualTo(1);
        assertThat(report.categoriesValid()).isZero();
        assertThat(report.issues()).hasSize(1);
        MasterDataAuditIssue issue = report.issues().get(0);
        assertThat(issue.problem()).contains("MASTER DATA MIGRATION REQUIRED: Missing stable code");
        assertThat(issue.refId()).isEqualTo("TC-CORRUPT-1");
    }

    // ============================================================
    // 14. Missing test code
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("14. Missing test code: audit flags unmigrated test with refId")
    void test14_MissingTestCode_Audited() {
        TestCategory cat = TestCategory.builder().id(UUID.randomUUID()).code("HEM").refId("TC-1").name("Hematology").build();
        Test corruptedTest = Test.builder()
                .id(UUID.randomUUID())
                .name("Test Without Code")
                .code("")
                .refId("TEST-CORRUPT-1")
                .category(cat)
                .sampleType(SampleType.SERUM)
                .build();

        when(testCategoryRepository.findAll()).thenReturn(List.of(cat));
        when(testRepository.findAll()).thenReturn(List.of(corruptedTest));
        when(testParameterRepository.findAll()).thenReturn(Collections.emptyList());

        ExistingMasterDataAuditReport report = auditor.audit();

        assertThat(report.testsMissingCode()).isEqualTo(1);
        assertThat(report.issues())
                .anyMatch(i -> i.entityType().equals("Test") && i.problem().contains("Missing stable code"));
    }

    // ============================================================
    // 15. Missing parameter code
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("15. Missing parameter code: audit flags unmigrated parameter")
    void test15_MissingParameterCode_Audited() {
        Test test = Test.builder().id(UUID.randomUUID()).code("CBC").refId("TEST-1").name("CBC").sampleType(SampleType.SERUM).build();
        TestParameter corruptedParam = TestParameter.builder()
                .id(UUID.randomUUID())
                .name("Parameter Without Code")
                .code("  ")
                .refId("PARAM-CORRUPT-1")
                .test(test)
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .build();

        when(testCategoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(testRepository.findAll()).thenReturn(List.of(test));
        when(testParameterRepository.findAll()).thenReturn(List.of(corruptedParam));

        ExistingMasterDataAuditReport report = auditor.audit();

        assertThat(report.parametersMissingCode()).isEqualTo(1);
        assertThat(report.issues())
                .anyMatch(i -> i.entityType().equals("TestParameter") && i.problem().contains("Missing stable parameter code"));
    }

    // ============================================================
    // 16. Duplicate DB code
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("16. Duplicate DB code: audit flags conflict requiring manual deduplication")
    void test16_DuplicateDbCode_Audited() {
        TestCategory cat1 = TestCategory.builder().id(UUID.randomUUID()).code("BIO").name("Biochem 1").refId("TC-1").build();
        TestCategory cat2 = TestCategory.builder().id(UUID.randomUUID()).code("BIO").name("Biochem 2").refId("TC-2").build();

        when(testCategoryRepository.findAll()).thenReturn(List.of(cat1, cat2));
        when(testRepository.findAll()).thenReturn(Collections.emptyList());
        when(testParameterRepository.findAll()).thenReturn(Collections.emptyList());

        ExistingMasterDataAuditReport report = auditor.audit();

        assertThat(report.categoriesDuplicateCode()).isEqualTo(1);
        assertThat(report.issues())
                .anyMatch(i -> i.problem().contains("Duplicate category code in existing database"));
    }

    // ============================================================
    // 17. Duplicate JSON code
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("17. Duplicate JSON code: validation rejects payload before any database mutation")
    void test17_DuplicateJsonCode_RejectedByValidator() {
        MasterCategorySeedDto cat1 = MasterCategorySeedDto.builder().code("BIO").name("Bio 1").build();
        MasterCategorySeedDto cat2 = MasterCategorySeedDto.builder().code("BIO").name("Bio 2").build();

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(cat1, cat2))
                .build();

        when(loader.load()).thenReturn(payload);

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("Duplicate category code: 'BIO'");

        verify(testCategoryRepository, never()).save(any());
    }

    // ============================================================
    // 18. Existing record configuration conflict
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("18. Existing record configuration conflict: detected and reported")
    void test18_ExistingRecordConfigurationConflict() {
        TestCategory cat = TestCategory.builder().id(UUID.randomUUID()).code("BIO").name("Existing Name").refId("TC-1").build();
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(cat));

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(MasterCategorySeedDto.builder().code("BIO").name("Seed Different Name").build()))
                .build();

        when(loader.load()).thenReturn(payload);

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.conflicts()).hasSize(1);
        assertThat(summary.conflicts().get(0).field()).isEqualTo("name");
        assertThat(summary.conflicts().get(0).existingValue()).isEqualTo("Existing Name");
        assertThat(summary.conflicts().get(0).seedValue()).isEqualTo("Seed Different Name");
        assertThat(cat.getName()).isEqualTo("Existing Name"); // Preserved
    }

    // ============================================================
    // 19. Category relationship conflict
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("19. Category relationship conflict: prevents silent parent reassignment")
    void test19_CategoryRelationshipConflict() {
        TestCategory existingCat = TestCategory.builder().id(UUID.randomUUID()).code("HEM").name("Hematology").refId("TC-1").build();
        Test existingTest = Test.builder()
                .id(UUID.randomUUID())
                .code("CBC")
                .name("CBC")
                .category(existingCat)
                .sampleType(SampleType.WHOLE_BLOOD)
                .refId("TEST-1")
                .build();

        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(existingTest));
        when(testCategoryRepository.existsByCode("BIO")).thenReturn(true);

        MasterDataPayload payload = MasterDataPayload.builder()
                .tests(List.of(MasterTestSeedDto.builder()
                        .code("CBC").name("CBC").categoryCode("BIO")
                        .sampleType(SampleType.WHOLE_BLOOD).build()))
                .build();

        when(loader.load()).thenReturn(payload);

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.conflicts()).hasSize(1);
        assertThat(summary.conflicts().get(0).field()).isEqualTo("category");
        assertThat(summary.conflicts().get(0).existingValue()).isEqualTo("HEM");
        assertThat(summary.conflicts().get(0).seedValue()).isEqualTo("BIO");
        assertThat(existingTest.getCategory().getCode()).isEqualTo("HEM"); // Intact
    }

    // ============================================================
    // 20. Calculation dependency conflict
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("20. Calculation dependency conflict: missing required calculation dependency caught")
    void test20_CalculationDependencyConflict() {
        MasterTestSeedDto testDto = MasterTestSeedDto.builder()
                .code("LFT").name("Liver Function Test").categoryCode("BIO")
                .sampleType(SampleType.SERUM).build();

        // AG_RATIO requires ALBUMIN and GLOBULIN, but only AG_RATIO is provided
        MasterParameterSeedDto calcParam = MasterParameterSeedDto.builder()
                .testCode("LFT").code("AG_RATIO").name("A/G Ratio")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.AG_RATIO)
                .displayOrder(1).build();

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(MasterCategorySeedDto.builder().code("BIO").name("Bio").build()))
                .tests(List.of(testDto))
                .parameters(List.of(calcParam))
                .build();

        when(loader.load()).thenReturn(payload);

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("Missing required calculation dependency");

        verify(testCategoryRepository, never()).save(any());
    }

    // ============================================================
    // 21. Fail-on-conflict behavior
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("21. Fail-on-conflict behavior: throws exception when failOnConflict is enabled")
    void test21_FailOnConflictBehavior() {
        properties.setFailOnConflict(true);

        TestCategory cat = TestCategory.builder().id(UUID.randomUUID()).code("BIO").name("Existing Name").refId("TC-1").build();
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(cat));

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(MasterCategorySeedDto.builder().code("BIO").name("New Name In Seed").build()))
                .build();

        when(loader.load()).thenReturn(payload);

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("Field 'name' differs");

        verify(testCategoryRepository, never()).save(any());
    }

    // ============================================================
    // 22. No deletion when JSON record is absent
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("22. No deletion when JSON record is absent: additive only, never deletes")
    void test22_NoDeletionWhenJsonRecordAbsent() {
        // Database has records, but JSON payload is completely empty
        when(loader.load()).thenReturn(MasterDataPayload.empty());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.categoriesInserted()).isZero();
        assertThat(summary.categoriesSkipped()).isZero();
        verify(testCategoryRepository, never()).delete(any(TestCategory.class));
        verify(testCategoryRepository, never()).deleteAll();
        verify(testRepository, never()).delete(any(Test.class));
        verify(testRepository, never()).deleteAll();
        verify(testParameterRepository, never()).delete(any(TestParameter.class));
        verify(testParameterRepository, never()).deleteAll();
    }

    // ============================================================
    // 23. Transaction rollback
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("23. Transaction rollback: validation error aborts transaction before persist")
    void test23_TransactionRollbackOnValidationError() {
        MasterCategorySeedDto invalidCat = MasterCategorySeedDto.builder()
                .code("") // Blank code is invalid
                .name("Invalid Category")
                .build();

        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .categories(List.of(invalidCat))
                .build());

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class);

        verify(testCategoryRepository, never()).save(any());
        verify(testRepository, never()).save(any());
        verify(testParameterRepository, never()).save(any());
    }

    // ============================================================
    // 24. Public refId remains unchanged
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("24. Public refId remains unchanged: existing IDs remain completely untouched across runs")
    void test24_PublicRefIdRemainsUnchanged() {
        UUID catId = UUID.randomUUID();
        String catRefId = "TC-STABLE-111";
        TestCategory cat = TestCategory.builder().id(catId).code("BIO").name("Biochemistry").refId(catRefId).build();

        UUID testId = UUID.randomUUID();
        String testRefId = "TEST-STABLE-222";
        Test test = Test.builder().id(testId).code("LFT").name("LFT").category(cat).sampleType(SampleType.SERUM).refId(testRefId).build();

        UUID paramId = UUID.randomUUID();
        String paramRefId = "PARAM-STABLE-333";
        TestParameter param = TestParameter.builder().id(paramId).code("ALT").name("ALT").test(test).refId(paramRefId)
                .dataType(TestParameterDataType.INTEGER).inputType(ParameterInputType.MANUAL).build();

        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(cat));
        when(testRepository.findByCode("LFT")).thenReturn(Optional.of(test));
        when(testParameterRepository.findByTest_IdAndCode(testId, "ALT")).thenReturn(Optional.of(param));

        when(loader.load()).thenReturn(MasterDataPayload.builder()
                .categories(List.of(MasterCategorySeedDto.builder().code("BIO").name("Biochemistry").build()))
                .tests(List.of(MasterTestSeedDto.builder().code("LFT").name("LFT").categoryCode("BIO").sampleType(SampleType.SERUM).build()))
                .parameters(List.of(MasterParameterSeedDto.builder().testCode("LFT").code("ALT").name("ALT")
                        .dataType(TestParameterDataType.INTEGER).inputType(ParameterInputType.MANUAL).displayOrder(1).build()))
                .build());

        seeder.seed();

        assertThat(cat.getRefId()).isEqualTo(catRefId);
        assertThat(test.getRefId()).isEqualTo(testRefId);
        assertThat(param.getRefId()).isEqualTo(paramRefId);
    }

    // ============================================================
    // 25. Frontend/public API compatibility
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("25. Frontend/public API compatibility: refIds conform to standard TC-, TEST-, PARAM- prefixes")
    void test25_FrontendPublicApiCompatibility() {
        String catRefId = RefIdGenerator.generate("TC-");
        String testRefId = RefIdGenerator.generate("TEST-");
        String paramRefId = RefIdGenerator.generate("PARAM-");

        assertThat(catRefId).startsWith("TC-").hasSizeGreaterThan(10);
        assertThat(testRefId).startsWith("TEST-").hasSizeGreaterThan(10);
        assertThat(paramRefId).startsWith("PARAM-").hasSizeGreaterThan(10);

        UUID catId = UUID.randomUUID();
        UUID testId = UUID.randomUUID();
        UUID paramId = UUID.randomUUID();

        TestCategory cat = TestCategory.builder().id(catId).code("HEM").name("Hematology").refId(catRefId).build();
        Test test = Test.builder().id(testId).code("CBC").name("CBC").refId(testRefId).category(cat).sampleType(SampleType.WHOLE_BLOOD).build();
        TestParameter param = TestParameter.builder().id(paramId).code("HB").name("Hemoglobin").refId(paramRefId).test(test)
                .dataType(TestParameterDataType.DECIMAL).inputType(ParameterInputType.MANUAL).build();

        when(testCategoryRepository.findAll()).thenReturn(List.of(cat));
        when(testRepository.findAll()).thenReturn(List.of(test));
        when(testParameterRepository.findAll()).thenReturn(List.of(param));

        ExistingMasterDataAuditReport report = auditor.audit();

        assertThat(report.hasIssues()).isFalse();
        assertThat(report.categoriesValid()).isEqualTo(1);
        assertThat(report.testsValid()).isEqualTo(1);
        assertThat(report.parametersValid()).isEqualTo(1);
    }

    // ============================================================
    // Startup Audit Runner & Exporter Tests
    // ============================================================
    @org.junit.jupiter.api.Test
    @DisplayName("Startup Audit Runner executes audit when enabled and aborts on fail-on-conflict")
    void testStartupAuditRunner_AbortsWhenFailOnConflictEnabled() {
        properties.setAuditOnStartup(true);
        properties.setFailOnConflict(true);

        TestCategory corrupted = TestCategory.builder().id(UUID.randomUUID()).refId("TC-BAD").name("Bad Cat").code(null).build();
        when(testCategoryRepository.findAll()).thenReturn(List.of(corrupted));
        when(testRepository.findAll()).thenReturn(Collections.emptyList());
        when(testParameterRepository.findAll()).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> auditRunner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Startup aborted: Master data audit found conflicts/issues");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Baseline Exporter writes valid DB records into expected JSON structures without altering source")
    void testBaselineExporter_WritesJsonStructures(@TempDir Path tempDir) throws IOException {
        TestCategory cat = TestCategory.builder().id(UUID.randomUUID()).code("HEM").name("Hematology").status(TestCategoryStatus.ACTIVE).refId("TC-1").build();
        Test test = Test.builder().id(UUID.randomUUID()).code("CBC").name("CBC").category(cat).sampleType(SampleType.WHOLE_BLOOD).status(TestStatus.ACTIVE).refId("TEST-1").build();
        TestParameter param = TestParameter.builder().id(UUID.randomUUID()).code("HB").name("Hemoglobin").test(test).dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL).displayOrder(1).status(TestParameterStatus.ACTIVE).refId("PARAM-1").build();

        when(testCategoryRepository.findAll()).thenReturn(List.of(cat));
        when(testRepository.findAll()).thenReturn(List.of(test));
        when(testParameterRepository.findAll()).thenReturn(List.of(param));

        MasterDataBaselineExporter.ExportSummary summary = exporter.exportToDirectory(tempDir);

        assertThat(summary.categoriesExported()).isEqualTo(1);
        assertThat(summary.testsExported()).isEqualTo(1);
        assertThat(summary.parametersExported()).isEqualTo(1);
        assertThat(tempDir.resolve("categories.json")).exists();
        assertThat(tempDir.resolve("tests.json")).exists();
        assertThat(tempDir.resolve("test-parameters").resolve("cbc.json")).exists();
    }
}

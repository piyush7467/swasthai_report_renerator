package com.swasthai.report_generator.test.masterdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.calculators.*;
import com.swasthai.report_generator.test.calculation.impl.CalculationEngineImpl;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties;
import com.swasthai.report_generator.test.masterdata.dto.MasterCategorySeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterParameterSeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterTestSeedDto;
import com.swasthai.report_generator.test.masterdata.loader.MasterDataLoader;
import com.swasthai.report_generator.test.masterdata.loader.MasterDataPayload;
import com.swasthai.report_generator.test.masterdata.runner.MasterDataSeedRunner;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeedSummary;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeeder;
import com.swasthai.report_generator.test.masterdata.validation.MasterDataConflict;
import com.swasthai.report_generator.test.masterdata.validation.MasterDataValidationException;
import com.swasthai.report_generator.test.masterdata.validation.MasterDataValidator;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterDataSeederTest {

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
    }

    // ============================================================
    // 1. EMPTY JSON FILES
    // ============================================================
    @Test
    @DisplayName("1. Empty JSON files -> 0 inserted, 0 skipped, validation passed")
    void testEmptyJson_SuccessfulNoOp() {
        when(loader.load()).thenReturn(MasterDataPayload.empty());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.categoriesFound()).isZero();
        assertThat(summary.categoriesInserted()).isZero();
        assertThat(summary.testsFound()).isZero();
        assertThat(summary.testsInserted()).isZero();
        assertThat(summary.parametersFound()).isZero();
        assertThat(summary.parametersInserted()).isZero();
        assertThat(summary.validationPassed()).isTrue();
        verifyNoInteractions(testCategoryRepository);
    }

    // ============================================================
    // 2. FIRST SEED
    // ============================================================
    @Test
    @DisplayName("2. First seed -> Inserts missing categories, tests, and parameters")
    void testFirstSeed_InsertsAll() {
        MasterCategorySeedDto catDto = MasterCategorySeedDto.builder()
                .code("BIO").name("Biochemistry").sourceFile("categories.json").build();
        MasterTestSeedDto testDto = MasterTestSeedDto.builder()
                .code("LIPID").name("Lipid Profile").categoryCode("BIO")
                .sampleType(SampleType.SERUM).sourceFile("tests.json").build();
        MasterParameterSeedDto paramDto = MasterParameterSeedDto.builder()
                .code("CHOL").testCode("LIPID").name("Cholesterol")
                .dataType(TestParameterDataType.DECIMAL).inputType(ParameterInputType.MANUAL)
                .displayOrder(1).sourceFile("lipid.json").build();

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(catDto))
                .tests(List.of(testDto))
                .parameters(List.of(paramDto))
                .build();

        when(loader.load()).thenReturn(payload);
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.empty());
        when(testRepository.findByCode("LIPID")).thenReturn(Optional.empty());

        TestCategory savedCat = TestCategory.builder().id(UUID.randomUUID()).code("BIO").name("Biochemistry").build();
        when(testCategoryRepository.save(any())).thenReturn(savedCat);

        com.swasthai.report_generator.test.entity.Test savedTest = com.swasthai.report_generator.test.entity.Test.builder()
                .id(UUID.randomUUID()).code("LIPID").category(savedCat).build();
        when(testRepository.save(any())).thenReturn(savedTest);

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.categoriesInserted()).isEqualTo(1);
        assertThat(summary.testsInserted()).isEqualTo(1);
        assertThat(summary.parametersInserted()).isEqualTo(1);
        verify(testCategoryRepository, times(1)).save(any());
        verify(testRepository, times(1)).save(any());
        verify(testParameterRepository, times(1)).save(any());
    }

    // ============================================================
    // 3. SECOND IDENTICAL SEED (IDEMPOTENT SKIP)
    // ============================================================
    @Test
    @DisplayName("3. Second seed -> All records exist, 0 inserted, all skipped")
    void testSecondSeed_IdempotentSkip() {
        MasterCategorySeedDto catDto = MasterCategorySeedDto.builder()
                .code("BIO").name("Biochemistry").sourceFile("categories.json").build();
        MasterTestSeedDto testDto = MasterTestSeedDto.builder()
                .code("LIPID").name("Lipid Profile").categoryCode("BIO")
                .sampleType(SampleType.SERUM).sourceFile("tests.json").build();
        MasterParameterSeedDto paramDto = MasterParameterSeedDto.builder()
                .code("CHOL").testCode("LIPID").name("Cholesterol")
                .dataType(TestParameterDataType.DECIMAL).inputType(ParameterInputType.MANUAL)
                .displayOrder(1).sourceFile("lipid.json").build();

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(catDto))
                .tests(List.of(testDto))
                .parameters(List.of(paramDto))
                .build();

        when(loader.load()).thenReturn(payload);

        UUID catId = UUID.randomUUID();
        UUID testId = UUID.randomUUID();
        TestCategory dbCat = TestCategory.builder().id(catId).code("BIO").name("Biochemistry").build();
        com.swasthai.report_generator.test.entity.Test dbTest = com.swasthai.report_generator.test.entity.Test.builder()
                .id(testId).code("LIPID").name("Lipid Profile").sampleType(SampleType.SERUM).category(dbCat).build();
        TestParameter dbParam = TestParameter.builder().id(UUID.randomUUID()).test(dbTest).code("CHOL").name("Cholesterol")
                .dataType(TestParameterDataType.DECIMAL).inputType(ParameterInputType.MANUAL).build();

        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(dbCat));
        when(testRepository.findByCode("LIPID")).thenReturn(Optional.of(dbTest));
        when(testParameterRepository.findByTest_IdAndCode(testId, "CHOL")).thenReturn(Optional.of(dbParam));

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.categoriesInserted()).isZero();
        assertThat(summary.categoriesSkipped()).isEqualTo(1);
        assertThat(summary.testsInserted()).isZero();
        assertThat(summary.testsSkipped()).isEqualTo(1);
        assertThat(summary.parametersInserted()).isZero();
        assertThat(summary.parametersSkipped()).isEqualTo(1);
        verify(testCategoryRepository, never()).save(any());
        verify(testRepository, never()).save(any());
        verify(testParameterRepository, never()).save(any());
    }

    // ============================================================
    // 4. ADDING NEW CATEGORIES
    // ============================================================
    @Test
    @DisplayName("4. Adding new categories -> Existing category skipped, new category inserted")
    void testAddNewCategory_PreservesExisting() {
        MasterCategorySeedDto cat1 = MasterCategorySeedDto.builder().code("BIO").name("Biochemistry").build();
        MasterCategorySeedDto cat2 = MasterCategorySeedDto.builder().code("HEM").name("Hematology").build();

        MasterDataPayload payload = MasterDataPayload.builder().categories(List.of(cat1, cat2)).build();
        when(loader.load()).thenReturn(payload);

        TestCategory existingBio = TestCategory.builder().code("BIO").name("Biochemistry").build();
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(existingBio));
        when(testCategoryRepository.findByCode("HEM")).thenReturn(Optional.empty());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.categoriesFound()).isEqualTo(2);
        assertThat(summary.categoriesSkipped()).isEqualTo(1);
        assertThat(summary.categoriesInserted()).isEqualTo(1);
        verify(testCategoryRepository, times(1)).save(any(TestCategory.class));
    }

    // ============================================================
    // 5. ADDING NEW TESTS
    // ============================================================
    @Test
    @DisplayName("5. Adding new tests -> Existing test skipped, new test inserted")
    void testAddNewTest_PreservesExisting() {
        MasterTestSeedDto test1 = MasterTestSeedDto.builder().code("CBC").name("Complete Blood Count")
                .categoryCode("HEM").sampleType(SampleType.WHOLE_BLOOD).build();
        MasterTestSeedDto test2 = MasterTestSeedDto.builder().code("ESR").name("Erythrocyte Sedimentation Rate")
                .categoryCode("HEM").sampleType(SampleType.WHOLE_BLOOD).build();

        MasterDataPayload payload = MasterDataPayload.builder().tests(List.of(test1, test2)).build();
        when(loader.load()).thenReturn(payload);

        when(testCategoryRepository.existsByCode("HEM")).thenReturn(true);
        TestCategory dbHem = TestCategory.builder().code("HEM").name("Hematology").build();
        when(testCategoryRepository.findByCode("HEM")).thenReturn(Optional.of(dbHem));

        com.swasthai.report_generator.test.entity.Test existingCbc = com.swasthai.report_generator.test.entity.Test.builder()
                .code("CBC").name("Complete Blood Count").sampleType(SampleType.WHOLE_BLOOD).category(dbHem).build();
        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(existingCbc));
        when(testRepository.findByCode("ESR")).thenReturn(Optional.empty());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.testsFound()).isEqualTo(2);
        assertThat(summary.testsSkipped()).isEqualTo(1);
        assertThat(summary.testsInserted()).isEqualTo(1);
        verify(testRepository, times(1)).save(any(com.swasthai.report_generator.test.entity.Test.class));
    }

    // ============================================================
    // 6. ADDING NEW PARAMETERS
    // ============================================================
    @Test
    @DisplayName("6. Adding new parameters -> Existing parameter skipped, new parameter inserted")
    void testAddNewParameter_PreservesExisting() {
        MasterParameterSeedDto p1 = MasterParameterSeedDto.builder().code("HB").testCode("CBC").name("Hemoglobin")
                .dataType(TestParameterDataType.DECIMAL).inputType(ParameterInputType.MANUAL).displayOrder(1).build();
        MasterParameterSeedDto p2 = MasterParameterSeedDto.builder().code("WBC").testCode("CBC").name("White Blood Cells")
                .dataType(TestParameterDataType.INTEGER).inputType(ParameterInputType.MANUAL).displayOrder(2).build();

        MasterDataPayload payload = MasterDataPayload.builder().parameters(List.of(p1, p2)).build();
        when(loader.load()).thenReturn(payload);

        UUID testId = UUID.randomUUID();
        when(testRepository.existsByCode("CBC")).thenReturn(true);
        com.swasthai.report_generator.test.entity.Test dbCbc = com.swasthai.report_generator.test.entity.Test.builder()
                .id(testId).code("CBC").name("Complete Blood Count").build();
        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(dbCbc));

        TestParameter existingHb = TestParameter.builder().code("HB").name("Hemoglobin").build();
        when(testParameterRepository.findByTest_IdAndCode(testId, "HB")).thenReturn(Optional.of(existingHb));
        when(testParameterRepository.findByTest_IdAndCode(testId, "WBC")).thenReturn(Optional.empty());

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.parametersFound()).isEqualTo(2);
        assertThat(summary.parametersSkipped()).isEqualTo(1);
        assertThat(summary.parametersInserted()).isEqualTo(1);
        verify(testParameterRepository, times(1)).save(any(TestParameter.class));
    }

    // ============================================================
    // 7. DUPLICATE CATEGORY CODE
    // ============================================================
    @Test
    @DisplayName("7. Duplicate category code in seed -> Validation fails before DB mutation")
    void testDuplicateCategoryCodeInSeed_FailsValidation() {
        MasterCategorySeedDto cat1 = MasterCategorySeedDto.builder().code("BIO").name("Biochemistry").sourceFile("categories.json").build();
        MasterCategorySeedDto cat2 = MasterCategorySeedDto.builder().code("bio").name("Biochemistry Duplicate").sourceFile("categories.json").build();

        MasterDataPayload payload = MasterDataPayload.builder().categories(List.of(cat1, cat2)).build();
        when(loader.load()).thenReturn(payload);

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("Duplicate category code: 'BIO'");
        verify(testCategoryRepository, never()).save(any());
    }

    // ============================================================
    // 8. DUPLICATE TEST CODE
    // ============================================================
    @Test
    @DisplayName("8. Duplicate test code in seed -> Validation fails before DB mutation")
    void testDuplicateTestCodeInSeed_FailsValidation() {
        MasterCategorySeedDto cat = MasterCategorySeedDto.builder().code("BIO").name("Biochemistry").build();
        MasterTestSeedDto test1 = MasterTestSeedDto.builder().code("LFT").name("Liver Function Test").categoryCode("BIO")
                .sampleType(SampleType.SERUM).sourceFile("tests.json").build();
        MasterTestSeedDto test2 = MasterTestSeedDto.builder().code("lft").name("Liver Function Dup").categoryCode("BIO")
                .sampleType(SampleType.SERUM).sourceFile("tests.json").build();

        MasterDataPayload payload = MasterDataPayload.builder().categories(List.of(cat)).tests(List.of(test1, test2)).build();
        when(loader.load()).thenReturn(payload);

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("Duplicate test code: 'LFT'");
        verify(testRepository, never()).save(any());
    }

    // ============================================================
    // 9. DUPLICATE PARAMETER CODE
    // ============================================================
    @Test
    @DisplayName("9. Duplicate parameter code for the same test -> Validation fails before DB mutation")
    void testDuplicateParameterCodeInSameTest_FailsValidation() {
        MasterCategorySeedDto cat = MasterCategorySeedDto.builder().code("BIO").name("Biochemistry").build();
        MasterTestSeedDto test = MasterTestSeedDto.builder().code("LFT").name("Liver Function").categoryCode("BIO")
                .sampleType(SampleType.SERUM).build();
        MasterParameterSeedDto p1 = MasterParameterSeedDto.builder().code("ALT").testCode("LFT").name("Alanine Transaminase")
                .dataType(TestParameterDataType.DECIMAL).displayOrder(1).sourceFile("lft.json").build();
        MasterParameterSeedDto p2 = MasterParameterSeedDto.builder().code("alt").testCode("LFT").name("ALT Duplicate")
                .dataType(TestParameterDataType.DECIMAL).displayOrder(2).sourceFile("lft.json").build();

        MasterDataPayload payload = MasterDataPayload.builder().categories(List.of(cat)).tests(List.of(test)).parameters(List.of(p1, p2)).build();
        when(loader.load()).thenReturn(payload);

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("Duplicate parameter code 'ALT' for test 'LFT'");
        verify(testParameterRepository, never()).save(any());
    }

    // ============================================================
    // 10. MISSING CATEGORY REFERENCE
    // ============================================================
    @Test
    @DisplayName("10. Missing category reference in test -> Validation fails before DB mutation")
    void testMissingCategoryReference_FailsValidation() {
        MasterTestSeedDto test = MasterTestSeedDto.builder().code("CBC").name("Complete Blood Count")
                .categoryCode("UNKNOWN_CAT").sampleType(SampleType.WHOLE_BLOOD).sourceFile("tests.json").build();

        MasterDataPayload payload = MasterDataPayload.builder().tests(List.of(test)).build();
        when(loader.load()).thenReturn(payload);
        when(testCategoryRepository.existsByCode("UNKNOWN_CAT")).thenReturn(false);

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("references missing category code: 'UNKNOWN_CAT'");
        verify(testRepository, never()).save(any());
    }

    // ============================================================
    // 11. MISSING TEST REFERENCE
    // ============================================================
    @Test
    @DisplayName("11. Missing test reference in parameter -> Validation fails before DB mutation")
    void testMissingTestReference_FailsValidation() {
        MasterParameterSeedDto param = MasterParameterSeedDto.builder().code("HB").testCode("UNKNOWN_TEST")
                .name("Hemoglobin").dataType(TestParameterDataType.DECIMAL).displayOrder(1).sourceFile("params.json").build();

        MasterDataPayload payload = MasterDataPayload.builder().parameters(List.of(param)).build();
        when(loader.load()).thenReturn(payload);
        when(testRepository.existsByCode("UNKNOWN_TEST")).thenReturn(false);

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("references missing test code: 'UNKNOWN_TEST'");
        verify(testParameterRepository, never()).save(any());
    }

    // ============================================================
    // 12. EXISTING RECORD CONFLICT DETECTION
    // ============================================================
    @Test
    @DisplayName("12. Existing record conflict -> Detected and reported; rejected in strict mode")
    void testConflictDetection_ReportsFieldDiscrepancy() {
        MasterCategorySeedDto seedCat = MasterCategorySeedDto.builder().code("BIO").name("Biochemistry New Name").build();
        MasterDataPayload payload = MasterDataPayload.builder().categories(List.of(seedCat)).build();
        when(loader.load()).thenReturn(payload);

        TestCategory dbCat = TestCategory.builder().code("BIO").name("Biochemistry Old Name").build();
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(dbCat));

        // When fail-on-conflict = true
        properties.setFailOnConflict(true);

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("Field 'name' differs -> Existing DB: 'Biochemistry Old Name', Seed JSON: 'Biochemistry New Name'");

        // When fail-on-conflict = false -> logs warning, skips, succeeds
        properties.setFailOnConflict(false);
        MasterDataSeedSummary summary = seeder.seed();
        assertThat(summary.conflictCount()).isEqualTo(1);
        assertThat(summary.categoriesSkipped()).isEqualTo(1);
        assertThat(summary.categoriesInserted()).isZero();
    }

    // ============================================================
    // 13. INVALID CALCULATION TYPE
    // ============================================================
    @Test
    @DisplayName("13. Calculated parameter with NONE or unsupported calculation type -> Fails validation")
    void testInvalidCalculationType_FailsValidation() {
        MasterTestSeedDto test = MasterTestSeedDto.builder().code("CBC").name("CBC").categoryCode("HEM")
                .sampleType(SampleType.WHOLE_BLOOD).build();
        MasterParameterSeedDto p = MasterParameterSeedDto.builder().code("CALC1").testCode("CBC")
                .name("Calc Param").dataType(TestParameterDataType.DECIMAL).inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.NONE).displayOrder(1).sourceFile("cbc.json").build();

        MasterDataPayload payload = MasterDataPayload.builder().tests(List.of(test)).parameters(List.of(p)).build();
        when(loader.load()).thenReturn(payload);
        when(testCategoryRepository.existsByCode("HEM")).thenReturn(true);

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("Calculated parameter must have a valid calculation type");
    }

    // ============================================================
    // 14. MISSING CALCULATION DEPENDENCY
    // ============================================================
    @Test
    @DisplayName("14. Calculated parameter missing required dependencies -> Fails validation")
    void testMissingCalculationDependency_FailsValidation() {
        MasterTestSeedDto test = MasterTestSeedDto.builder().code("LFT").name("Liver Function").categoryCode("BIO")
                .sampleType(SampleType.SERUM).build();
        // Globulin requires TP (Total Protein) and ALB (Albumin)
        MasterParameterSeedDto globulin = MasterParameterSeedDto.builder().code("GLOB").testCode("LFT")
                .name("Globulin").dataType(TestParameterDataType.DECIMAL).inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.GLOBULIN).displayOrder(1).sourceFile("lft.json").build();

        MasterDataPayload payload = MasterDataPayload.builder().tests(List.of(test)).parameters(List.of(globulin)).build();
        when(loader.load()).thenReturn(payload);
        when(testCategoryRepository.existsByCode("BIO")).thenReturn(true);
        when(testRepository.findByCode("LFT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("Missing required calculation dependency");
    }

    // ============================================================
    // 15. CIRCULAR DEPENDENCY
    // ============================================================
    @Test
    @DisplayName("15. Circular calculation dependency graph -> Detected and fails validation")
    void testCircularDependency_FailsValidation() {
        CalculationEngine mockEngine = mock(CalculationEngine.class);
        when(mockEngine.isSupported(any())).thenReturn(true);
        when(mockEngine.isResultDataTypeSupported(any(), any())).thenReturn(true);
        // MCV requires MCH and MCH requires MCV -> Circular!
        when(mockEngine.getRequiredParameters(CalculationType.MCV)).thenReturn(Set.of("MCH"));
        when(mockEngine.getRequiredParameters(CalculationType.MCH)).thenReturn(Set.of("MCV"));

        MasterDataValidator validatorWithMockEngine = new MasterDataValidator(
                validator,
                testCategoryRepository,
                testRepository,
                testParameterRepository,
                mockEngine,
                properties
        );
        MasterDataSeeder seederWithCycle = new MasterDataSeeder(
                loader,
                validatorWithMockEngine,
                testCategoryRepository,
                testRepository,
                testParameterRepository
        );

        MasterTestSeedDto test = MasterTestSeedDto.builder().code("TEST").name("Test Panel").categoryCode("BIO")
                .sampleType(SampleType.SERUM).build();
        MasterParameterSeedDto p1 = MasterParameterSeedDto.builder().code("MCV").testCode("TEST")
                .name("MCV").dataType(TestParameterDataType.DECIMAL).inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.MCV).displayOrder(1).sourceFile("test.json").build();
        MasterParameterSeedDto p2 = MasterParameterSeedDto.builder().code("MCH").testCode("TEST")
                .name("MCH").dataType(TestParameterDataType.DECIMAL).inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.MCH).displayOrder(2).sourceFile("test.json").build();

        MasterDataPayload payload = MasterDataPayload.builder().tests(List.of(test)).parameters(List.of(p1, p2)).build();
        when(loader.load()).thenReturn(payload);
        when(testCategoryRepository.existsByCode("BIO")).thenReturn(true);

        assertThatThrownBy(seederWithCycle::seed)
                .isInstanceOf(MasterDataValidationException.class)
                .hasMessageContaining("Circular calculation dependency detected");
    }

    // ============================================================
    // 16. TRANSACTION ROLLBACK / NO PARTIAL DB CHANGES
    // ============================================================
    @Test
    @DisplayName("16. Validation failure produces zero partial database changes")
    void testValidationFailure_NoPartialDbChanges() {
        MasterCategorySeedDto validCat = MasterCategorySeedDto.builder().code("BIO").name("Biochemistry").build();
        MasterTestSeedDto invalidTest = MasterTestSeedDto.builder().code("CBC").name("CBC")
                .categoryCode("NON_EXISTENT").sampleType(SampleType.WHOLE_BLOOD).build();

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(validCat))
                .tests(List.of(invalidTest))
                .build();
        when(loader.load()).thenReturn(payload);
        when(testCategoryRepository.existsByCode("NON_EXISTENT")).thenReturn(false);

        assertThatThrownBy(() -> seeder.seed())
                .isInstanceOf(MasterDataValidationException.class);

        verify(testCategoryRepository, never()).save(any());
        verify(testRepository, never()).save(any());
    }

    // ============================================================
    // 17. REFID PRESERVATION ON EXISTING RECORDS
    // ============================================================
    @Test
    @DisplayName("17. Existing records retain their refIds and are not modified")
    void testRefIdPreserved_ExistingRecordsRetainRefIds() {
        MasterCategorySeedDto catDto = MasterCategorySeedDto.builder().code("BIO").name("Biochemistry").build();
        MasterDataPayload payload = MasterDataPayload.builder().categories(List.of(catDto)).build();
        when(loader.load()).thenReturn(payload);

        String originalRefId = "TC-abc123456789";
        TestCategory existing = TestCategory.builder().id(UUID.randomUUID()).refId(originalRefId)
                .code("BIO").name("Biochemistry").build();
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(existing));

        MasterDataSeedSummary summary = seeder.seed();

        assertThat(summary.categoriesSkipped()).isEqualTo(1);
        assertThat(existing.getRefId()).isEqualTo(originalRefId);
        verify(testCategoryRepository, never()).save(any());
    }

    // ============================================================
    // 18. NO DUPLICATE ENTITIES
    // ============================================================
    @Test
    @DisplayName("18. Master data seeder never creates duplicate records in DB")
    void testNoDuplicateEntities_InDb() {
        MasterCategorySeedDto catDto = MasterCategorySeedDto.builder().code("BIO").name("Biochemistry").build();
        MasterDataPayload payload = MasterDataPayload.builder().categories(List.of(catDto)).build();
        when(loader.load()).thenReturn(payload);

        TestCategory existing = TestCategory.builder().id(UUID.randomUUID()).code("BIO").name("Biochemistry").build();
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(existing));

        seeder.seed();

        verify(testCategoryRepository, never()).save(any());
    }

    // ============================================================
    // 19. NEW RECORDS RECEIVE REFIDS
    // ============================================================
    @Test
    @DisplayName("19. New entities receive properly formatted refIds via RefIdGenerator prefix")
    void testNewRecordsReceiveRefIds() {
        String catRefId = RefIdGenerator.generate("TC");
        String testRefId = RefIdGenerator.generate("TEST");
        String paramRefId = RefIdGenerator.generate("PARAM");

        assertThat(catRefId).startsWith("TC-").hasSize(15);
        assertThat(testRefId).startsWith("TEST-").hasSize(17);
        assertThat(paramRefId).startsWith("PARAM-").hasSize(18);
    }

    // ============================================================
    // 20. SEED DISABLED DOES NOTHING
    // ============================================================
    @Test
    @DisplayName("20. Startup runner does nothing when seed is disabled")
    void testSeederDisabled_DoesNotExecute() {
        properties.setEnabled(false);
        MasterDataSeeder mockSeeder = mock(MasterDataSeeder.class);
        MasterDataSeedRunner runner = new MasterDataSeedRunner(properties, mockSeeder);

        runner.run(mock(ApplicationArguments.class));

        verify(mockSeeder, never()).seed();
    }

    // ============================================================
    // 21. SEED ENABLED EXECUTES SAFELY
    // ============================================================
    @Test
    @DisplayName("21. Startup runner triggers seeder when enabled")
    void testSeederEnabled_ExecutesSuccessfully() {
        properties.setEnabled(true);
        MasterDataSeeder mockSeeder = mock(MasterDataSeeder.class);
        MasterDataSeedRunner runner = new MasterDataSeedRunner(properties, mockSeeder);

        runner.run(mock(ApplicationArguments.class));

        verify(mockSeeder, times(1)).seed();
    }

    // ============================================================
    // 22. EXISTING APIS CONTINUE WORKING
    // ============================================================
    @Test
    @DisplayName("22. Category & Test entity builders correctly populate all public API fields")
    void testExistingApis_ContinueWorking() {
        TestCategory cat = TestCategory.builder().code("HEM").name("Hematology").build();
        com.swasthai.report_generator.test.entity.Test test = com.swasthai.report_generator.test.entity.Test.builder()
                .code("CBC").name("Complete Blood Count").category(cat).sampleType(SampleType.WHOLE_BLOOD)
                .fastingRequired(false).prioritySupported(true).build();
        TestParameter param = TestParameter.builder()
                .code("HB").name("Hemoglobin").test(test).dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL).displayOrder(1).required(true).build();

        assertThat(test.getCode()).isEqualTo("CBC");
        assertThat(test.getCategory().getCode()).isEqualTo("HEM");
        assertThat(param.getCode()).isEqualTo("HB");
        assertThat(param.getTest().getCode()).isEqualTo("CBC");
        assertThat(param.getDataType()).isEqualTo(TestParameterDataType.DECIMAL);
    }
}

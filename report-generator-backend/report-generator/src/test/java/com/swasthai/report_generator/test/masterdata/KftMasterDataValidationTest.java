package com.swasthai.report_generator.test.masterdata;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeedSummary;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeeder;
import com.swasthai.report_generator.test.masterdata.validation.MasterDataConflict;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KftMasterDataValidationTest {

    @Mock
    private TestCategoryRepository testCategoryRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestParameterRepository testParameterRepository;

    private MasterDataLoader loader;
    private MasterDataValidator validator;
    private CalculationEngine calculationEngine;
    private MasterDataSeedProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MasterDataSeedProperties();
        properties.setEnabled(true);
        properties.setResourcePath("classpath:master-data");
        properties.setFailOnConflict(false);

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        loader = new MasterDataLoader(objectMapper, new PathMatchingResourcePatternResolver(), properties);

        calculationEngine = new CalculationEngineImpl(List.of(
                new BUNCreatinineRatioCalculator(),
                new UreaCreatinineRatioCalculator(),
                new EgfrCkdEpi2021Calculator(),
                new AnionGapCalculator(),
                new AnionGapKCalculator()
        ));

        Validator jpaValidator = Validation.buildDefaultValidatorFactory().getValidator();

        validator = new MasterDataValidator(
                jpaValidator,
                testCategoryRepository,
                testRepository,
                testParameterRepository,
                calculationEngine,
                properties
        );
    }

    @Test
    @DisplayName("1. Load and parse KFT category, test, and parameter master data JSON files")
    void testLoadKftMasterDataPayload() {
        List<MasterCategorySeedDto> categories = loader.loadCategories("classpath:master-data/categories.json");
        List<MasterTestSeedDto> tests = loader.loadTests("classpath:master-data/tests.json");
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/kft.json");

        assertThat(categories).isNotEmpty();
        assertThat(tests).isNotEmpty();
        assertThat(parameters).hasSize(6);

        // Verify BIO Category
        MasterCategorySeedDto bioCat = categories.stream()
                .filter(c -> "BIO".equalsIgnoreCase(c.code()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("BIO category not found in categories.json"));
        assertThat(bioCat.name()).isEqualTo("Biochemistry");
        assertThat(bioCat.status()).isEqualTo(TestCategoryStatus.ACTIVE);

        // Verify KFT Test
        MasterTestSeedDto kftTest = tests.stream()
                .filter(t -> "KFT".equalsIgnoreCase(t.code()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("KFT test not found in tests.json"));
        assertThat(kftTest.name()).isEqualTo("Kidney Function Test");
        assertThat(kftTest.shortName()).isEqualTo("KFT");
        assertThat(kftTest.categoryCode()).isEqualTo("BIO");
        assertThat(kftTest.sampleType()).isEqualTo(SampleType.SERUM);
        assertThat(kftTest.testType()).isEqualTo(TestType.PANEL);
        assertThat(kftTest.status()).isEqualTo(TestStatus.ACTIVE);

        // Verify all 6 parameters reference KFT
        for (MasterParameterSeedDto p : parameters) {
            assertThat(p.testCode()).isEqualTo("KFT");
            assertThat(p.code()).isNotBlank();
            assertThat(p.name()).isNotBlank();
            assertThat(p.dataType()).isEqualTo(TestParameterDataType.DECIMAL);
            assertThat(p.displayOrder()).isPositive();
            assertThat(p.status()).isEqualTo(TestParameterStatus.ACTIVE);
            assertThat(p.required()).isTrue();
        }
    }

    @Test
    @DisplayName("2. Verify KFT parameter codes, input types, and calculation types")
    void testKftParameterCalculationMappings() {
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/kft.json");
        Map<String, MasterParameterSeedDto> byCode = new HashMap<>();
        for (MasterParameterSeedDto p : parameters) {
            byCode.put(p.code().toUpperCase(), p);
        }

        // All 6 are manual parameters with calculation type NONE
        List<String> expectedCodes = List.of("CREA", "UREA", "NA", "K", "CL", "CA");
        assertThat(byCode.keySet()).containsExactlyInAnyOrderElementsOf(expectedCodes);

        for (String code : expectedCodes) {
            MasterParameterSeedDto param = byCode.get(code);
            assertThat(param).as("Missing parameter: %s", code).isNotNull();
            assertThat(param.inputType()).isEqualTo(ParameterInputType.MANUAL);
            assertThat(param.calculationType()).isEqualTo(CalculationType.NONE);
        }

        // Verify standard clinical units
        assertThat(byCode.get("CREA").unit()).isEqualTo("mg/dL");
        assertThat(byCode.get("UREA").unit()).isEqualTo("mg/dL");
        assertThat(byCode.get("NA").unit()).isEqualTo("mmol/L");
        assertThat(byCode.get("K").unit()).isEqualTo("mmol/L");
        assertThat(byCode.get("CL").unit()).isEqualTo("mmol/L");
        assertThat(byCode.get("CA").unit()).isEqualTo("mg/dL");
    }

    @Test
    @DisplayName("3. Verify database constraint compatibility (chk_tpr_calculation_consistency)")
    void testDatabaseConstraintCompatibility() {
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/kft.json");

        for (MasterParameterSeedDto param : parameters) {
            ParameterInputType inputType = param.getEffectiveInputType();
            CalculationType calcType = param.getEffectiveCalculationType();

            boolean condition1 = (inputType == ParameterInputType.CALCULATED && calcType != CalculationType.NONE);
            boolean condition2 = (inputType != ParameterInputType.CALCULATED && calcType == CalculationType.NONE);

            assertThat(condition1 || condition2)
                    .as("Parameter %s violates chk_tpr_calculation_consistency: inputType=%s, calcType=%s",
                            param.code(), inputType, calcType)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("4. MasterDataValidator passes cleanly for KFT category, test, and parameters")
    void testMasterDataValidatorPassesForKft() {
        MasterCategorySeedDto bioCat = loader.loadCategories("classpath:master-data/categories.json").stream()
                .filter(c -> "BIO".equalsIgnoreCase(c.code())).findFirst().orElseThrow();
        MasterTestSeedDto kftTest = loader.loadTests("classpath:master-data/tests.json").stream()
                .filter(t -> "KFT".equalsIgnoreCase(t.code())).findFirst().orElseThrow();
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/kft.json");

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(bioCat))
                .tests(List.of(kftTest))
                .parameters(parameters)
                .build();

        List<MasterDataConflict> conflicts = validator.validate(payload);
        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("5. Unapproved clinical ranges remain null pending laboratory sign-off")
    void testUnapprovedIntervalsRemainNull() {
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/kft.json");

        for (MasterParameterSeedDto p : parameters) {
            assertThat(p.referenceMin())
                    .as("Parameter %s must not have invented referenceMin", p.code())
                    .isNull();
            assertThat(p.referenceMax())
                    .as("Parameter %s must not have invented referenceMax", p.code())
                    .isNull();
            assertThat(p.criticalLow())
                    .as("Parameter %s must not have invented criticalLow", p.code())
                    .isNull();
            assertThat(p.criticalHigh())
                    .as("Parameter %s must not have invented criticalHigh", p.code())
                    .isNull();
        }
    }

    @Test
    @DisplayName("6. Simulate idempotent KFT seeding: inserts on missing, skips on existing")
    void testSimulateIdempotentKftSeeding() {
        MasterCategorySeedDto bioCat = loader.loadCategories("classpath:master-data/categories.json").stream()
                .filter(c -> "BIO".equalsIgnoreCase(c.code())).findFirst().orElseThrow();
        MasterTestSeedDto kftTest = loader.loadTests("classpath:master-data/tests.json").stream()
                .filter(t -> "KFT".equalsIgnoreCase(t.code())).findFirst().orElseThrow();
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/kft.json");

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(bioCat))
                .tests(List.of(kftTest))
                .parameters(parameters)
                .build();

        MasterDataLoader mockLoader = mock(MasterDataLoader.class);
        when(mockLoader.load()).thenReturn(payload);

        MasterDataSeeder seeder = new MasterDataSeeder(
                mockLoader,
                validator,
                testCategoryRepository,
                testRepository,
                testParameterRepository
        );

        // --- Run 1: Empty database -> 1 category, 1 test, 6 parameters inserted ---
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.empty());
        when(testRepository.findByCode("KFT")).thenReturn(Optional.empty());

        TestCategory savedCat = TestCategory.builder().id(UUID.randomUUID()).code("BIO").name("Biochemistry").build();
        com.swasthai.report_generator.test.entity.Test savedTest = com.swasthai.report_generator.test.entity.Test.builder()
                .id(UUID.randomUUID()).code("KFT").name("Kidney Function Test").category(savedCat).build();

        when(testCategoryRepository.save(any(TestCategory.class))).thenReturn(savedCat);
        when(testRepository.save(any(com.swasthai.report_generator.test.entity.Test.class))).thenReturn(savedTest);
        when(testParameterRepository.findByTest_IdAndCode(any(), any())).thenReturn(Optional.empty());
        when(testParameterRepository.save(any(com.swasthai.report_generator.test.entity.TestParameter.class))).thenAnswer(inv -> inv.getArgument(0));

        MasterDataSeedSummary firstRun = seeder.seed();

        assertThat(firstRun.categoriesInserted()).isEqualTo(1);
        assertThat(firstRun.testsInserted()).isEqualTo(1);
        assertThat(firstRun.parametersInserted()).isEqualTo(6);
        assertThat(firstRun.categoriesSkipped()).isZero();
        assertThat(firstRun.testsSkipped()).isZero();
        assertThat(firstRun.parametersSkipped()).isZero();

        // --- Run 2: Existing database -> 1 category, 1 test, 6 parameters skipped ---
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(savedCat));
        when(testRepository.findByCode("KFT")).thenReturn(Optional.of(savedTest));
        when(testParameterRepository.findByTest_IdAndCode(eq(savedTest.getId()), anyString()))
                .thenAnswer(inv -> Optional.of(com.swasthai.report_generator.test.entity.TestParameter.builder()
                        .code(inv.getArgument(1)).test(savedTest).build()));

        MasterDataSeedSummary secondRun = seeder.seed();

        assertThat(secondRun.categoriesInserted()).isZero();
        assertThat(secondRun.testsInserted()).isZero();
        assertThat(secondRun.parametersInserted()).isZero();
        assertThat(secondRun.categoriesSkipped()).isEqualTo(1);
        assertThat(secondRun.testsSkipped()).isEqualTo(1);
        assertThat(secondRun.parametersSkipped()).isEqualTo(6);
    }
}

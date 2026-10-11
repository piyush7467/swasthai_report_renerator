package com.swasthai.report_generator.test.masterdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.test.calculation.CalculationDependencyResolver;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.calculators.AGRatioCalculator;
import com.swasthai.report_generator.test.calculation.calculators.GlobulinCalculator;
import com.swasthai.report_generator.test.calculation.calculators.IndirectBilirubinCalculator;
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

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LftMasterDataValidationTest {

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
                new IndirectBilirubinCalculator(),
                new GlobulinCalculator(),
                new AGRatioCalculator()
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
    @DisplayName("1. Load and parse LFT category, test, and parameter master data JSON files")
    void testLoadLftMasterDataPayload() {
        List<MasterCategorySeedDto> categories = loader.loadCategories("classpath:master-data/categories.json");
        List<MasterTestSeedDto> tests = loader.loadTests("classpath:master-data/tests.json");
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/lft.json");

        assertThat(categories).isNotEmpty();
        assertThat(tests).isNotEmpty();
        assertThat(parameters).hasSize(10);

        // Verify BIO Category
        MasterCategorySeedDto bioCat = categories.stream()
                .filter(c -> "BIO".equalsIgnoreCase(c.code()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("BIO category not found in categories.json"));
        assertThat(bioCat.name()).isEqualTo("Biochemistry");
        assertThat(bioCat.status()).isEqualTo(TestCategoryStatus.ACTIVE);

        // Verify LFT Test
        MasterTestSeedDto lftTest = tests.stream()
                .filter(t -> "LFT".equalsIgnoreCase(t.code()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("LFT test not found in tests.json"));
        assertThat(lftTest.name()).isEqualTo("Liver Function Test");
        assertThat(lftTest.shortName()).isEqualTo("LFT");
        assertThat(lftTest.categoryCode()).isEqualTo("BIO");
        assertThat(lftTest.sampleType()).isEqualTo(SampleType.SERUM);
        assertThat(lftTest.testType()).isEqualTo(TestType.PANEL);
        assertThat(lftTest.status()).isEqualTo(TestStatus.ACTIVE);

        // Verify all 10 parameters reference LFT
        for (MasterParameterSeedDto p : parameters) {
            assertThat(p.testCode()).isEqualTo("LFT");
            assertThat(p.code()).isNotBlank();
            assertThat(p.name()).isNotBlank();
            assertThat(p.dataType()).isEqualTo(TestParameterDataType.DECIMAL);
            assertThat(p.displayOrder()).isPositive();
        }
    }

    @Test
    @DisplayName("2. Verify LFT parameter codes, input types, and calculation types")
    void testLftParameterCalculationMappings() {
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/lft.json");
        Map<String, MasterParameterSeedDto> byCode = new HashMap<>();
        for (MasterParameterSeedDto p : parameters) {
            byCode.put(p.code().toUpperCase(), p);
        }

        // Manual parameters
        List<String> manualCodes = List.of("TBIL", "DBIL", "AST", "ALT", "ALP", "TP", "ALB");
        for (String code : manualCodes) {
            MasterParameterSeedDto param = byCode.get(code);
            assertThat(param).as("Missing parameter: %s", code).isNotNull();
            assertThat(param.inputType()).isEqualTo(ParameterInputType.MANUAL);
            assertThat(param.calculationType()).isEqualTo(CalculationType.NONE);
        }

        // Calculated parameters
        MasterParameterSeedDto ibil = byCode.get("IBIL");
        assertThat(ibil).isNotNull();
        assertThat(ibil.inputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(ibil.calculationType()).isEqualTo(CalculationType.INDIRECT_BILIRUBIN);
        assertThat(ibil.unit()).isEqualTo("mg/dL");

        MasterParameterSeedDto glob = byCode.get("GLOB");
        assertThat(glob).isNotNull();
        assertThat(glob.inputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(glob.calculationType()).isEqualTo(CalculationType.GLOBULIN);
        assertThat(glob.unit()).isEqualTo("g/dL");

        MasterParameterSeedDto agRatio = byCode.get("AG_RATIO");
        assertThat(agRatio).isNotNull();
        assertThat(agRatio.inputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(agRatio.calculationType()).isEqualTo(CalculationType.AG_RATIO);
        assertThat(agRatio.unit()).isEqualTo("ratio");
    }

    @Test
    @DisplayName("3. Verify database constraint compatibility (chk_tpr_calculation_consistency)")
    void testDatabaseConstraintCompatibility() {
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/lft.json");

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
    @DisplayName("4. MasterDataValidator passes cleanly for LFT category, test, and parameters")
    void testMasterDataValidatorPassesForLft() {
        MasterCategorySeedDto bioCat = loader.loadCategories("classpath:master-data/categories.json").stream()
                .filter(c -> "BIO".equalsIgnoreCase(c.code())).findFirst().orElseThrow();
        MasterTestSeedDto lftTest = loader.loadTests("classpath:master-data/tests.json").stream()
                .filter(t -> "LFT".equalsIgnoreCase(t.code())).findFirst().orElseThrow();
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/lft.json");

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(bioCat))
                .tests(List.of(lftTest))
                .parameters(parameters)
                .build();

        List<MasterDataConflict> conflicts = validator.validate(payload);
        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("5. Dependency ordering: GLOBULIN resolved before AG_RATIO")
    void testDependencyOrdering_GlobulinBeforeAgRatio() {
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/lft.json");

        List<com.swasthai.report_generator.test.entity.TestParameter> calculatedEntities = parameters.stream()
                .filter(p -> p.inputType() == ParameterInputType.CALCULATED)
                .map(p -> {
                    com.swasthai.report_generator.test.entity.TestParameter tp = new com.swasthai.report_generator.test.entity.TestParameter();
                    tp.setCode(p.code().toUpperCase());
                    tp.setInputType(p.inputType());
                    tp.setCalculationType(p.calculationType());
                    return tp;
                })
                .toList();

        List<com.swasthai.report_generator.test.entity.TestParameter> ordered =
                CalculationDependencyResolver.resolveParameterOrder(calculatedEntities, calculationEngine);

        int globIndex = -1;
        int agRatioIndex = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if ("GLOB".equalsIgnoreCase(ordered.get(i).getCode())) {
                globIndex = i;
            }
            if ("AG_RATIO".equalsIgnoreCase(ordered.get(i).getCode())) {
                agRatioIndex = i;
            }
        }

        assertThat(globIndex).isNotNegative();
        assertThat(agRatioIndex).isNotNegative();
        assertThat(globIndex).as("GLOBULIN must execute before AG_RATIO").isLessThan(agRatioIndex);
    }

    @Test
    @DisplayName("6. Unapproved reference intervals for ALP, TP, ALB remain null pending lab owner sign-off")
    void testUnapprovedIntervalsRemainNull() {
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/lft.json");
        Map<String, MasterParameterSeedDto> byCode = new HashMap<>();
        for (MasterParameterSeedDto p : parameters) {
            byCode.put(p.code().toUpperCase(), p);
        }

        // Unapproved/conflicting intervals must NOT enter production seed data as hardcoded values
        assertThat(byCode.get("ALP").referenceMin()).isNull();
        assertThat(byCode.get("ALP").referenceMax()).isNull();
        assertThat(byCode.get("TP").referenceMin()).isNull();
        assertThat(byCode.get("TP").referenceMax()).isNull();
        assertThat(byCode.get("ALB").referenceMin()).isNull();
        assertThat(byCode.get("ALB").referenceMax()).isNull();

        // Critical limits are uninvented and null for all 10
        for (MasterParameterSeedDto p : parameters) {
            assertThat(p.criticalLow()).isNull();
            assertThat(p.criticalHigh()).isNull();
        }

        // Agreed ranges are populated
        assertThat(byCode.get("TBIL").referenceMin()).isEqualByComparingTo("0.2");
        assertThat(byCode.get("TBIL").referenceMax()).isEqualByComparingTo("1.2");
        assertThat(byCode.get("DBIL").referenceMin()).isEqualByComparingTo("0.2");
        assertThat(byCode.get("DBIL").referenceMax()).isEqualByComparingTo("0.4");
        assertThat(byCode.get("IBIL").referenceMin()).isEqualByComparingTo("0.0");
        assertThat(byCode.get("IBIL").referenceMax()).isEqualByComparingTo("0.8");
        assertThat(byCode.get("AST").referenceMin()).isEqualByComparingTo("0.0");
        assertThat(byCode.get("AST").referenceMax()).isEqualByComparingTo("40.0");
        assertThat(byCode.get("ALT").referenceMin()).isEqualByComparingTo("0.0");
        assertThat(byCode.get("ALT").referenceMax()).isEqualByComparingTo("40.0");
        assertThat(byCode.get("GLOB").referenceMin()).isEqualByComparingTo("2.3");
        assertThat(byCode.get("GLOB").referenceMax()).isEqualByComparingTo("3.6");
        assertThat(byCode.get("AG_RATIO").referenceMin()).isEqualByComparingTo("1.0");
        assertThat(byCode.get("AG_RATIO").referenceMax()).isEqualByComparingTo("2.3");
    }

    @Test
    @DisplayName("7. Simulate idempotent LFT seeding: inserts on missing, skips on existing")
    void testSimulateIdempotentLftSeeding() {
        MasterCategorySeedDto bioCat = loader.loadCategories("classpath:master-data/categories.json").stream()
                .filter(c -> "BIO".equalsIgnoreCase(c.code())).findFirst().orElseThrow();
        MasterTestSeedDto lftTest = loader.loadTests("classpath:master-data/tests.json").stream()
                .filter(t -> "LFT".equalsIgnoreCase(t.code())).findFirst().orElseThrow();
        List<MasterParameterSeedDto> parameters = loader.loadParameters("classpath:master-data/test-parameters/lft.json");

        MasterDataPayload payload = MasterDataPayload.builder()
                .categories(List.of(bioCat))
                .tests(List.of(lftTest))
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

        // --- Run 1: Empty database -> 1 category, 1 test, 10 parameters inserted ---
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.empty());
        when(testRepository.findByCode("LFT")).thenReturn(Optional.empty());

        TestCategory savedCat = TestCategory.builder().id(UUID.randomUUID()).code("BIO").name("Biochemistry").build();
        com.swasthai.report_generator.test.entity.Test savedTest = com.swasthai.report_generator.test.entity.Test.builder()
                .id(UUID.randomUUID()).code("LFT").name("Liver Function Test").category(savedCat).build();

        when(testCategoryRepository.save(any(TestCategory.class))).thenReturn(savedCat);
        when(testRepository.save(any(com.swasthai.report_generator.test.entity.Test.class))).thenReturn(savedTest);
        when(testParameterRepository.findByTest_IdAndCode(any(), any())).thenReturn(Optional.empty());
        when(testParameterRepository.save(any(com.swasthai.report_generator.test.entity.TestParameter.class))).thenAnswer(inv -> inv.getArgument(0));

        MasterDataSeedSummary firstRun = seeder.seed();

        assertThat(firstRun.categoriesInserted()).isEqualTo(1);
        assertThat(firstRun.testsInserted()).isEqualTo(1);
        assertThat(firstRun.parametersInserted()).isEqualTo(10);
        assertThat(firstRun.categoriesSkipped()).isZero();
        assertThat(firstRun.testsSkipped()).isZero();
        assertThat(firstRun.parametersSkipped()).isZero();

        // --- Run 2: Existing database -> 1 category, 1 test, 10 parameters skipped ---
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.of(savedCat));
        when(testRepository.findByCode("LFT")).thenReturn(Optional.of(savedTest));
        when(testParameterRepository.findByTest_IdAndCode(eq(savedTest.getId()), anyString()))
                .thenAnswer(inv -> Optional.of(com.swasthai.report_generator.test.entity.TestParameter.builder()
                        .code(inv.getArgument(1)).test(savedTest).build()));

        MasterDataSeedSummary secondRun = seeder.seed();

        assertThat(secondRun.categoriesInserted()).isZero();
        assertThat(secondRun.testsInserted()).isZero();
        assertThat(secondRun.parametersInserted()).isZero();
        assertThat(secondRun.categoriesSkipped()).isEqualTo(1);
        assertThat(secondRun.testsSkipped()).isEqualTo(1);
        assertThat(secondRun.parametersSkipped()).isEqualTo(10);
    }
}

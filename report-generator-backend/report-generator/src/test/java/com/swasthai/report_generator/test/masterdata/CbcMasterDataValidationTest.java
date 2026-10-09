package com.swasthai.report_generator.test.masterdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.calculators.MCHCalculator;
import com.swasthai.report_generator.test.calculation.calculators.MCHCCalculator;
import com.swasthai.report_generator.test.calculation.calculators.MCVCalculator;
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
class CbcMasterDataValidationTest {

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
                new MCVCalculator(),
                new MCHCalculator(),
                new MCHCCalculator()
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

    @org.junit.jupiter.api.Test
    @DisplayName("Should successfully load and parse CBC master data JSON files from classpath")
    void shouldLoadCbcMasterDataPayload() {
        MasterDataPayload payload = loader.load();

        assertThat(payload).isNotNull();
        assertThat(payload.categories()).hasSize(1);
        assertThat(payload.tests()).hasSize(1);
        assertThat(payload.parameters()).hasSize(15);

        // Verify Category
        MasterCategorySeedDto cat = payload.categories().get(0);
        assertThat(cat.code()).isEqualTo("HEM");
        assertThat(cat.name()).isEqualTo("Hematology");
        assertThat(cat.status()).isEqualTo(TestCategoryStatus.ACTIVE);

        // Verify Test
        MasterTestSeedDto test = payload.tests().get(0);
        assertThat(test.code()).isEqualTo("CBC");
        assertThat(test.name()).isEqualTo("Complete Blood Count");
        assertThat(test.categoryCode()).isEqualTo("HEM");
        assertThat(test.sampleType()).isEqualTo(SampleType.WHOLE_BLOOD);
        assertThat(test.status()).isEqualTo(TestStatus.ACTIVE);

        // Verify Parameters
        Set<String> paramCodes = new HashSet<>();
        for (MasterParameterSeedDto param : payload.parameters()) {
            assertThat(param.testCode()).isEqualTo("CBC");
            assertThat(param.code()).isNotBlank();
            assertThat(param.name()).isNotBlank();
            assertThat(param.dataType()).isEqualTo(TestParameterDataType.DECIMAL);
            assertThat(param.displayOrder()).isPositive();
            assertThat(paramCodes.add(param.code())).as("Duplicate parameter code: %s", param.code()).isTrue();
        }

        // Verify calculated parameters
        assertThat(paramCodes).contains("MCV", "MCH", "MCHC");
        MasterParameterSeedDto mcv = payload.parameters().stream().filter(p -> p.code().equals("MCV")).findFirst().orElseThrow();
        assertThat(mcv.inputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(mcv.calculationType()).isEqualTo(CalculationType.MCV);
        assertThat(mcv.unit()).isEqualTo("fL");

        MasterParameterSeedDto mch = payload.parameters().stream().filter(p -> p.code().equals("MCH")).findFirst().orElseThrow();
        assertThat(mch.inputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(mch.calculationType()).isEqualTo(CalculationType.MCH);
        assertThat(mch.unit()).isEqualTo("pg");

        MasterParameterSeedDto mchc = payload.parameters().stream().filter(p -> p.code().equals("MCHC")).findFirst().orElseThrow();
        assertThat(mchc.inputType()).isEqualTo(ParameterInputType.CALCULATED);
        assertThat(mchc.calculationType()).isEqualTo(CalculationType.MCHC);
        assertThat(mchc.unit()).isEqualTo("g/dL");

        // Verify manual input parameters
        for (MasterParameterSeedDto p : payload.parameters()) {
            if (!List.of("MCV", "MCH", "MCHC").contains(p.code())) {
                assertThat(p.inputType()).isEqualTo(ParameterInputType.MANUAL);
                assertThat(p.calculationType()).isEqualTo(CalculationType.NONE);
            }
        }
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Should pass all MasterDataValidator rules for CBC without validation errors or conflicts")
    void shouldPassMasterDataValidation() {
        MasterDataPayload payload = loader.load();

        List<MasterDataConflict> conflicts = validator.validate(payload);

        assertThat(conflicts).isEmpty();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Should simulate idempotent MasterDataSeeder execution: inserts on empty DB, skips on subsequent run")
    void shouldSimulateIdempotentSeeding() {
        MasterDataPayload payload = loader.load();

        MasterDataSeeder seeder = new MasterDataSeeder(
                loader,
                validator,
                testCategoryRepository,
                testRepository,
                testParameterRepository
        );

        // Setup mock behavior for first run (empty DB)
        when(testCategoryRepository.findByCode("HEM")).thenReturn(Optional.empty());
        when(testRepository.findByCode("CBC")).thenReturn(Optional.empty());

        TestCategory savedCat = TestCategory.builder().id(UUID.randomUUID()).code("HEM").name("Hematology").build();
        com.swasthai.report_generator.test.entity.Test savedTest = com.swasthai.report_generator.test.entity.Test.builder()
                .id(UUID.randomUUID()).code("CBC").name("Complete Blood Count").category(savedCat).build();

        when(testCategoryRepository.save(any(TestCategory.class))).thenReturn(savedCat);
        when(testRepository.save(any(com.swasthai.report_generator.test.entity.Test.class))).thenReturn(savedTest);
        when(testParameterRepository.findByTest_IdAndCode(any(), any())).thenReturn(Optional.empty());
        when(testParameterRepository.save(any(TestParameter.class))).thenAnswer(inv -> inv.getArgument(0));

        MasterDataSeedSummary firstRun = seeder.seed();

        assertThat(firstRun.categoriesInserted()).isEqualTo(1);
        assertThat(firstRun.testsInserted()).isEqualTo(1);
        assertThat(firstRun.parametersInserted()).isEqualTo(15);
        assertThat(firstRun.categoriesSkipped()).isEqualTo(0);
        assertThat(firstRun.testsSkipped()).isEqualTo(0);
        assertThat(firstRun.parametersSkipped()).isEqualTo(0);

        // Setup mock behavior for second run (existing DB records)
        when(testCategoryRepository.findByCode("HEM")).thenReturn(Optional.of(savedCat));
        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(savedTest));
        when(testParameterRepository.findByTest_IdAndCode(eq(savedTest.getId()), anyString()))
                .thenAnswer(inv -> Optional.of(TestParameter.builder().code(inv.getArgument(1)).test(savedTest).build()));

        MasterDataSeedSummary secondRun = seeder.seed();

        assertThat(secondRun.categoriesInserted()).isEqualTo(0);
        assertThat(secondRun.testsInserted()).isEqualTo(0);
        assertThat(secondRun.parametersInserted()).isEqualTo(0);
        assertThat(secondRun.categoriesSkipped()).isEqualTo(1);
        assertThat(secondRun.testsSkipped()).isEqualTo(1);
        assertThat(secondRun.parametersSkipped()).isEqualTo(15);
    }
}

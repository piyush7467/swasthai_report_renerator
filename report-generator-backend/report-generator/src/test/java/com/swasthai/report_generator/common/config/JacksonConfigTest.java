package com.swasthai.report_generator.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.swasthai.report_generator.test.entity.SampleType;
import com.swasthai.report_generator.test.entity.TestParameterDataType;
import com.swasthai.report_generator.test.masterdata.dto.MasterCategorySeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterParameterSeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterTestSeedDto;
import com.swasthai.report_generator.test.masterdata.export.MasterDataBaselineExporter;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JacksonConfig.class);

    @Test
    @DisplayName("Should provide a com.fasterxml.jackson.databind.ObjectMapper bean in the Spring context")
    void shouldProvideJackson2ObjectMapperBean() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ObjectMapper.class);

            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper).isNotNull();
            assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
            assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
        });
    }

    @Test
    @DisplayName("Should not override an existing ObjectMapper bean if one is already registered")
    void shouldBackOffWhenCustomObjectMapperPresent() {
        new ApplicationContextRunner()
                .withUserConfiguration(CustomMapperConfig.class, JacksonConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ObjectMapper.class);
                    assertThat(context.getBean(ObjectMapper.class)).isSameAs(CustomMapperConfig.CUSTOM_INSTANCE);
                });
    }

    @Test
    @DisplayName("Should wire into MasterDataBaselineExporter without missing bean error")
    void shouldInjectIntoMasterDataBaselineExporter() {
        contextRunner
                .withBean(TestCategoryRepository.class, () -> Mockito.mock(TestCategoryRepository.class))
                .withBean(TestRepository.class, () -> Mockito.mock(TestRepository.class))
                .withBean(TestParameterRepository.class, () -> Mockito.mock(TestParameterRepository.class))
                .withBean(MasterDataBaselineExporter.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MasterDataBaselineExporter.class);
                    MasterDataBaselineExporter exporter = context.getBean(MasterDataBaselineExporter.class);
                    assertThat(exporter).isNotNull();
                });
    }

    @Test
    @DisplayName("Should wire into MasterDataLoader without missing bean error")
    void shouldInjectIntoMasterDataLoader() {
        contextRunner
                .withBean(org.springframework.core.io.support.ResourcePatternResolver.class, () -> Mockito.mock(org.springframework.core.io.support.ResourcePatternResolver.class))
                .withBean(com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties.class, com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties::new)
                .withBean(com.swasthai.report_generator.test.masterdata.loader.MasterDataLoader.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(com.swasthai.report_generator.test.masterdata.loader.MasterDataLoader.class);
                    com.swasthai.report_generator.test.masterdata.loader.MasterDataLoader loader =
                            context.getBean(com.swasthai.report_generator.test.masterdata.loader.MasterDataLoader.class);
                    assertThat(loader).isNotNull();
                });
    }

    @Test
    @DisplayName("Should correctly serialize and deserialize master data seed DTOs")
    void shouldSerializeAndDeserializeSeedDtos() {
        contextRunner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);

            MasterCategorySeedDto catDto = MasterCategorySeedDto.builder()
                    .code("BIOCHEM")
                    .name("Biochemistry")
                    .description("Biochemistry tests")
                    .build();

            String catJson = mapper.writeValueAsString(catDto);
            assertThat(catJson).contains("BIOCHEM").contains("Biochemistry");

            MasterCategorySeedDto deserializedCat = mapper.readValue(catJson, MasterCategorySeedDto.class);
            assertThat(deserializedCat.code()).isEqualTo("BIOCHEM");
            assertThat(deserializedCat.name()).isEqualTo("Biochemistry");

            MasterTestSeedDto testDto = MasterTestSeedDto.builder()
                    .code("LIPID")
                    .categoryCode("BIOCHEM")
                    .name("Lipid Profile")
                    .sampleType(SampleType.SERUM)
                    .build();

            String testJson = mapper.writeValueAsString(testDto);
            MasterTestSeedDto deserializedTest = mapper.readValue(testJson, MasterTestSeedDto.class);
            assertThat(deserializedTest.code()).isEqualTo("LIPID");
            assertThat(deserializedTest.categoryCode()).isEqualTo("BIOCHEM");

            MasterParameterSeedDto paramDto = MasterParameterSeedDto.builder()
                    .code("CHOL")
                    .testCode("LIPID")
                    .name("Cholesterol")
                    .dataType(TestParameterDataType.DECIMAL)
                    .build();

            String paramJson = mapper.writeValueAsString(paramDto);
            MasterParameterSeedDto deserializedParam = mapper.readValue(paramJson, MasterParameterSeedDto.class);
            assertThat(deserializedParam.code()).isEqualTo("CHOL");
            assertThat(deserializedParam.testCode()).isEqualTo("LIPID");
        });
    }

    @Configuration
    static class CustomMapperConfig {
        static final ObjectMapper CUSTOM_INSTANCE = new ObjectMapper();

        @Bean
        public ObjectMapper customObjectMapper() {
            return CUSTOM_INSTANCE;
        }
    }
}

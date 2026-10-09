package com.swasthai.report_generator.test.masterdata.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties;
import com.swasthai.report_generator.test.masterdata.dto.MasterCategorySeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterParameterSeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterTestSeedDto;
import com.swasthai.report_generator.test.masterdata.validation.MasterDataValidationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads master data JSON resources from the classpath or filesystem.
 */
@Component
@RequiredArgsConstructor
public class MasterDataLoader {

    private static final Logger log = LoggerFactory.getLogger(MasterDataLoader.class);

    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourcePatternResolver;
    private final MasterDataSeedProperties properties;

    public MasterDataPayload load() {
        String basePath = properties.getResourcePath();
        if (basePath == null || basePath.isBlank()) {
            basePath = "classpath:master-data";
        }
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        List<MasterCategorySeedDto> categories = loadCategories(basePath + "/categories.json");
        List<MasterTestSeedDto> tests = loadTests(basePath + "/tests.json");
        List<MasterParameterSeedDto> parameters = loadParameters(basePath + "/test-parameters/**/*.json");

        log.info("Loaded master data resources -> Categories: {}, Tests: {}, Parameters: {}",
                categories.size(), tests.size(), parameters.size());

        return MasterDataPayload.builder()
                .categories(categories)
                .tests(tests)
                .parameters(parameters)
                .build();
    }

    public List<MasterCategorySeedDto> loadCategories(String locationPattern) {
        Resource resource = resolveSingleResource(locationPattern);
        if (resource == null || !resource.exists()) {
            log.debug("No categories.json found at pattern: {}", locationPattern);
            return Collections.emptyList();
        }

        try (InputStream is = resource.getInputStream()) {
            List<MasterCategorySeedDto> raw = objectMapper.readValue(is, new TypeReference<List<MasterCategorySeedDto>>() {});
            if (raw == null) {
                return Collections.emptyList();
            }
            String sourceName = resource.getFilename() != null ? resource.getFilename() : locationPattern;
            List<MasterCategorySeedDto> result = new ArrayList<>(raw.size());
            for (MasterCategorySeedDto item : raw) {
                if (item != null) {
                    result.add(item.toBuilder().sourceFile(sourceName).build());
                }
            }
            return result;
        } catch (Exception ex) {
            String sourceName = resource.getFilename() != null ? resource.getFilename() : locationPattern;
            throw new MasterDataValidationException(
                    String.format("Failed to parse JSON file '%s': %s", sourceName, ex.getMessage()), ex
            );
        }
    }

    public List<MasterTestSeedDto> loadTests(String locationPattern) {
        Resource resource = resolveSingleResource(locationPattern);
        if (resource == null || !resource.exists()) {
            log.debug("No tests.json found at pattern: {}", locationPattern);
            return Collections.emptyList();
        }

        try (InputStream is = resource.getInputStream()) {
            List<MasterTestSeedDto> raw = objectMapper.readValue(is, new TypeReference<List<MasterTestSeedDto>>() {});
            if (raw == null) {
                return Collections.emptyList();
            }
            String sourceName = resource.getFilename() != null ? resource.getFilename() : locationPattern;
            List<MasterTestSeedDto> result = new ArrayList<>(raw.size());
            for (MasterTestSeedDto item : raw) {
                if (item != null) {
                    result.add(item.toBuilder().sourceFile(sourceName).build());
                }
            }
            return result;
        } catch (Exception ex) {
            String sourceName = resource.getFilename() != null ? resource.getFilename() : locationPattern;
            throw new MasterDataValidationException(
                    String.format("Failed to parse JSON file '%s': %s", sourceName, ex.getMessage()), ex
            );
        }
    }

    public List<MasterParameterSeedDto> loadParameters(String locationPattern) {
        List<MasterParameterSeedDto> allParameters = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources(locationPattern);
            if (resources == null || resources.length == 0) {
                log.debug("No parameter JSON files found matching: {}", locationPattern);
                return Collections.emptyList();
            }

            for (Resource resource : resources) {
                if (!resource.exists() || !resource.isReadable()) {
                    continue;
                }
                String sourceName = resource.getFilename() != null ? resource.getFilename() : resource.getDescription();
                try (InputStream is = resource.getInputStream()) {
                    List<MasterParameterSeedDto> raw = objectMapper.readValue(is, new TypeReference<List<MasterParameterSeedDto>>() {});
                    if (raw != null) {
                        for (MasterParameterSeedDto item : raw) {
                            if (item != null) {
                                allParameters.add(item.toBuilder().sourceFile(sourceName).build());
                            }
                        }
                    }
                } catch (Exception ex) {
                    throw new MasterDataValidationException(
                            String.format("Failed to parse parameter JSON file '%s': %s", sourceName, ex.getMessage()), ex
                    );
                }
            }
        } catch (MasterDataValidationException mdve) {
            throw mdve;
        } catch (Exception ex) {
            log.debug("Error while resolving parameter resources pattern '{}': {}", locationPattern, ex.getMessage());
        }

        return allParameters;
    }

    private Resource resolveSingleResource(String locationPattern) {
        try {
            Resource[] resources = resourcePatternResolver.getResources(locationPattern);
            if (resources != null && resources.length > 0) {
                return resources[0];
            }
        } catch (Exception e) {
            log.debug("Could not resolve resource pattern '{}': {}", locationPattern, e.getMessage());
        }
        return resourcePatternResolver.getResource(locationPattern);
    }
}

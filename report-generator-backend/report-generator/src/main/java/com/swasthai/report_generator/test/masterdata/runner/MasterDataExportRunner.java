package com.swasthai.report_generator.test.masterdata.runner;

import com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties;
import com.swasthai.report_generator.test.masterdata.export.MasterDataBaselineExporter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Startup runner that exports existing valid database master data into JSON format
 * matching the seeder JSON structure when enabled by configuration.
 * Runs at @Order(45) after MasterDataAuditRunner (@Order(40)) and before MasterDataSeedRunner (@Order(50)).
 */
@Component
@Order(45)
@RequiredArgsConstructor
public class MasterDataExportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MasterDataExportRunner.class);

    private final MasterDataSeedProperties properties;
    private final MasterDataBaselineExporter exporter;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isExportOnStartup()) {
            log.debug("Master data startup export is disabled (master-data.seed.export-on-startup=false). Skipping startup export.");
            return;
        }

        log.info("Master data startup export is enabled. Exporting baseline master data to: {}", properties.getExportPath());
        try {
            MasterDataBaselineExporter.ExportSummary summary = exporter.exportToDirectory(Paths.get(properties.getExportPath()));
            log.info("Baseline master data successfully exported: {} categories, {} tests, {} parameters to {}",
                    summary.categoriesExported(),
                    summary.testsExported(),
                    summary.parametersExported(),
                    summary.outputDirectory().toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to export baseline master data to: {}", properties.getExportPath(), e);
            if (properties.isFailOnConflict()) {
                throw new IllegalStateException("Startup aborted: Master data baseline export failed and fail-on-conflict is enabled.", e);
            }
        }
    }
}

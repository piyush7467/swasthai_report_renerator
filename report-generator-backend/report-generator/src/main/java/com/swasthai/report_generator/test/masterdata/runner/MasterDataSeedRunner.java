package com.swasthai.report_generator.test.masterdata.runner;

import com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeeder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Startup runner that executes master data seeding when enabled by configuration.
 */
@Component
@Order(50)
@RequiredArgsConstructor
public class MasterDataSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MasterDataSeedRunner.class);

    private final MasterDataSeedProperties properties;
    private final MasterDataSeeder seeder;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Master data seeding is disabled (master-data.seed.enabled=false). Skipping startup seed.");
            return;
        }

        log.info("Master data seeding is enabled. Executing master data sync...");
        seeder.seed();
    }
}

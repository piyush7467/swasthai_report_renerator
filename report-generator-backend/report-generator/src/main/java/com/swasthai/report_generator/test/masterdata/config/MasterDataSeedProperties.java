package com.swasthai.report_generator.test.masterdata.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for master data seeding.
 */
@Component
@ConfigurationProperties(prefix = "master-data.seed")
@Getter
@Setter
public class MasterDataSeedProperties {

    /**
     * Whether master data seeding is enabled on application startup.
     * Default is false (production safe).
     */
    private boolean enabled = false;

    /**
     * Whether detected conflicts between seed data and existing database records
     * should fail validation and abort the startup process.
     */
    private boolean failOnConflict = false;

    /**
     * Whether existing database master data should be audited on startup.
     * Default is false.
     */
    private boolean auditOnStartup = false;

    /**
     * Base resource path where seed JSON files are stored.
     */
    private String resourcePath = "classpath:master-data";
}

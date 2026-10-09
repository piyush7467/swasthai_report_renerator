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
     * Whether valid database master data should be automatically exported as JSON on startup.
     * Default is false.
     */
    private boolean exportOnStartup = false;

    /**
     * Directory path where exported JSON files will be written.
     * Default is "target/exported-master-data".
     */
    private String exportPath = "target/exported-master-data";

    /**
     * Base resource path where seed JSON files are stored.
     */
    private String resourcePath = "classpath:master-data";
}

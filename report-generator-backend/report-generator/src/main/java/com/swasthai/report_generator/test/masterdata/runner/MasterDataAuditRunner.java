package com.swasthai.report_generator.test.masterdata.runner;

import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditReport;
import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditor;
import com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Startup runner that audits existing database master data when enabled by configuration.
 * Runs at @Order(40) before MasterDataSeedRunner (@Order(50)).
 */
@Component
@Order(40)
@RequiredArgsConstructor
public class MasterDataAuditRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MasterDataAuditRunner.class);

    private final MasterDataSeedProperties properties;
    private final ExistingMasterDataAuditor auditor;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isAuditOnStartup()) {
            log.debug("Master data startup audit is disabled (master-data.seed.audit-on-startup=false). Skipping startup audit.");
            return;
        }

        log.info("Master data startup audit is enabled. Auditing existing database master data...");
        ExistingMasterDataAuditReport report = auditor.audit();

        if (report.hasIssues()) {
            log.warn("Existing database master data audit discovered issues/conflicts:\n{}", report.toFormattedReport());
            if (properties.isFailOnConflict()) {
                throw new IllegalStateException("Startup aborted: Master data audit found conflicts/issues in database and fail-on-conflict is enabled.");
            }
        } else {
            log.info("Existing database master data audit completed successfully with 0 issues.");
        }
    }
}

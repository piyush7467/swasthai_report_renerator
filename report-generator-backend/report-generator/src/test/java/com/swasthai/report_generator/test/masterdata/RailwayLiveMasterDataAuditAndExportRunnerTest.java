package com.swasthai.report_generator.test.masterdata;

import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditReport;
import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditor;
import com.swasthai.report_generator.test.masterdata.export.MasterDataBaselineExporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dedicated runner for executing Phase 2 (Read-only Database Audit & CBC Investigation)
 * and Phase 3 (Baseline Master Data Export) against the live Railway PostgreSQL database.
 *
 * Enable execution by supplying -DrunRailwayAudit=true
 * Example:
 *   .\mvnw.cmd test -Dtest=RailwayLiveMasterDataAuditAndExportRunnerTest -DrunRailwayAudit=true
 */
@SpringBootTest
@Tag("railway-live")
@EnabledIfSystemProperty(named = "runRailwayAudit", matches = "true")
class RailwayLiveMasterDataAuditAndExportRunnerTest {

    @Autowired
    private ExistingMasterDataAuditor auditor;

    @Autowired
    private MasterDataBaselineExporter exporter;

    @Test
    @DisplayName("Execute Phase 2 (Audit & CBC Investigation) and Phase 3 (Baseline Export) against Railway PostgreSQL")
    void executeAuditAndExport() throws IOException {
        System.out.println("\n==================================================");
        System.out.println("  PHASE 2: RUNNING LIVE READ-ONLY AUDIT ON RAILWAY ");
        System.out.println("==================================================");

        ExistingMasterDataAuditReport report = auditor.audit();

        System.out.println(report.toFormattedReport());

        System.out.println("\n==================================================");
        System.out.println("  PHASE 3: EXPORTING BASELINE MASTER DATA         ");
        System.out.println("==================================================");

        Path exportDir = Paths.get("target", "exported-master-data");
        MasterDataBaselineExporter.ExportSummary summary = exporter.exportToDirectory(exportDir);

        System.out.println(String.format("Export location:      %s", summary.outputDirectory().toAbsolutePath()));
        System.out.println(String.format("Categories exported:  %d", summary.categoriesExported()));
        System.out.println(String.format("Tests exported:       %d", summary.testsExported()));
        System.out.println(String.format("Parameters exported:  %d", summary.parametersExported()));
        System.out.println("==================================================\n");

        assertThat(report).isNotNull();
    }
}

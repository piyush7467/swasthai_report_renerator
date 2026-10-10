package com.swasthai.report_generator.test.masterdata.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditReport;
import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditor;
import com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties;
import com.swasthai.report_generator.test.masterdata.export.MasterDataBaselineExporter;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeedSummary;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeeder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Super Admin administrative controller for triggering master data audit, export, and seeding on demand.
 */
@RestController
@RequestMapping("/api/v1/admin/master-data")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class MasterDataAdminController {

    private static final Logger log = LoggerFactory.getLogger(MasterDataAdminController.class);

    private final ExistingMasterDataAuditor auditor;
    private final MasterDataBaselineExporter exporter;
    private final MasterDataSeeder seeder;
    private final MasterDataSeedProperties properties;

    /**
     * Run a live audit against existing database master data.
     * Read-only; does not modify any database records.
     */
    @GetMapping("/audit")
    public ApiResponse<ExistingMasterDataAuditReport> auditMasterData() {
        String actor = getCurrentUserEmail();
        log.info("[AUDIT] Super Admin '{}' initiated live master data audit", actor);
        ExistingMasterDataAuditReport report = auditor.audit();
        log.info("[AUDIT] Live master data audit completed for '{}'. Result: categoriesValid={}/{}, testsValid={}/{}, parametersValid={}/{}",
                actor, report.categoriesValid(), report.categoriesTotal(),
                report.testsValid(), report.testsTotal(),
                report.parametersValid(), report.parametersTotal());
        return ApiResponse.success("Existing master data audit completed successfully", report);
    }

    /**
     * Export all existing valid database master data into JSON catalog format
     * in the configured target export directory without touching source seed files.
     */
    @PostMapping("/export")
    public ApiResponse<MasterDataBaselineExporter.ExportSummary> exportMasterData(
            @RequestParam(required = false) String targetDirectory) throws IOException {
        String actor = getCurrentUserEmail();
        String path = (targetDirectory != null && !targetDirectory.isBlank())
                ? targetDirectory
                : properties.getExportPath();
        log.info("[AUDIT] Super Admin '{}' initiated master data baseline export to target path '{}'", actor, path);
        MasterDataBaselineExporter.ExportSummary summary = exporter.exportToDirectory(Paths.get(path));
        log.info("[AUDIT] Master data baseline export completed for '{}'. Exported: categories={}, tests={}, parameters={}, path={}",
                actor, summary.categoriesExported(), summary.testsExported(), summary.parametersExported(), summary.outputDirectory());
        return ApiResponse.success("Master data exported successfully to " + summary.outputDirectory().toAbsolutePath(), summary);
    }

    /**
     * Trigger master data seeding process manually.
     * Idempotent: skips existing records, only inserts missing records.
     */
    @PostMapping("/seed")
    public ApiResponse<MasterDataSeedSummary> seedMasterData() {
        String actor = getCurrentUserEmail();
        log.info("[AUDIT] Super Admin '{}' triggered on-demand master data seeding", actor);
        MasterDataSeedSummary summary = seeder.seed();
        log.info("[AUDIT] Master data seeding completed for '{}'. Summary: categoriesInserted={}, categoriesSkipped={}, testsInserted={}, testsSkipped={}, parametersInserted={}, parametersSkipped={}, conflicts={}",
                actor,
                summary.categoriesInserted(), summary.categoriesSkipped(),
                summary.testsInserted(), summary.testsSkipped(),
                summary.parametersInserted(), summary.parametersSkipped(),
                summary.conflicts() != null ? summary.conflicts().size() : 0);
        return ApiResponse.success("Master data seeding completed successfully", summary);
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "ANONYMOUS";
    }
}

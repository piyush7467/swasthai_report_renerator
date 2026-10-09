package com.swasthai.report_generator.test.masterdata.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditReport;
import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditor;
import com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties;
import com.swasthai.report_generator.test.masterdata.export.MasterDataBaselineExporter;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeedSummary;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
        ExistingMasterDataAuditReport report = auditor.audit();
        return ApiResponse.success("Existing master data audit completed successfully", report);
    }

    /**
     * Export all existing valid database master data into JSON catalog format
     * in the configured target export directory without touching source seed files.
     */
    @PostMapping("/export")
    public ApiResponse<MasterDataBaselineExporter.ExportSummary> exportMasterData(
            @RequestParam(required = false) String targetDirectory) throws IOException {
        String path = (targetDirectory != null && !targetDirectory.isBlank())
                ? targetDirectory
                : properties.getExportPath();
        MasterDataBaselineExporter.ExportSummary summary = exporter.exportToDirectory(Paths.get(path));
        return ApiResponse.success("Master data exported successfully to " + summary.outputDirectory().toAbsolutePath(), summary);
    }

    /**
     * Trigger master data seeding process manually.
     * Idempotent: skips existing records, only inserts missing records.
     */
    @PostMapping("/seed")
    public ApiResponse<MasterDataSeedSummary> seedMasterData() {
        MasterDataSeedSummary summary = seeder.seed();
        return ApiResponse.success("Master data seeding completed successfully", summary);
    }
}

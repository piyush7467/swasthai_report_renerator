package com.swasthai.report_generator.test.masterdata;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditReport;
import com.swasthai.report_generator.test.masterdata.audit.ExistingMasterDataAuditor;
import com.swasthai.report_generator.test.masterdata.config.MasterDataSeedProperties;
import com.swasthai.report_generator.test.masterdata.controller.MasterDataAdminController;
import com.swasthai.report_generator.test.masterdata.export.MasterDataBaselineExporter;
import com.swasthai.report_generator.test.masterdata.runner.MasterDataExportRunner;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeedSummary;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterDataExportAndAdminControllerTest {

    @Mock
    private ExistingMasterDataAuditor auditor;

    @Mock
    private MasterDataBaselineExporter exporter;

    @Mock
    private MasterDataSeeder seeder;

    private MasterDataSeedProperties properties;
    private MasterDataExportRunner exportRunner;
    private MasterDataAdminController adminController;

    @BeforeEach
    void setUp() {
        properties = new MasterDataSeedProperties();
        exportRunner = new MasterDataExportRunner(properties, exporter);
        adminController = new MasterDataAdminController(auditor, exporter, seeder, properties);
    }

    @Test
    @DisplayName("ExportRunner does not run when export-on-startup is false")
    void exportRunner_disabled_skipsExport() throws IOException {
        properties.setExportOnStartup(false);

        exportRunner.run(new DefaultApplicationArguments());

        verify(exporter, never()).exportToDirectory(any());
    }

    @Test
    @DisplayName("ExportRunner runs when export-on-startup is true")
    void exportRunner_enabled_executesExport() throws IOException {
        properties.setExportOnStartup(true);
        properties.setExportPath("target/test-export");

        MasterDataBaselineExporter.ExportSummary summary =
                new MasterDataBaselineExporter.ExportSummary(2, 5, 10, Paths.get("target/test-export"));
        when(exporter.exportToDirectory(Paths.get("target/test-export"))).thenReturn(summary);

        exportRunner.run(new DefaultApplicationArguments());

        verify(exporter, times(1)).exportToDirectory(Paths.get("target/test-export"));
    }

    @Test
    @DisplayName("ExportRunner throws exception when fail-on-conflict is true and export fails")
    void exportRunner_failsOnConflict_whenIoException() throws IOException {
        properties.setExportOnStartup(true);
        properties.setFailOnConflict(true);
        properties.setExportPath("target/invalid-path");

        when(exporter.exportToDirectory(any())).thenThrow(new IOException("Disk error"));

        assertThatThrownBy(() -> exportRunner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Master data baseline export failed");
    }

    @Test
    @DisplayName("AdminController audit endpoint delegates to auditor without mutation")
    void adminController_audit_delegatesToAuditor() {
        ExistingMasterDataAuditReport report = ExistingMasterDataAuditReport.builder()
                .categoriesTotal(10)
                .categoriesValid(10)
                .testsTotal(20)
                .testsValid(20)
                .parametersTotal(50)
                .parametersValid(50)
                .build();
        when(auditor.audit()).thenReturn(report);

        ApiResponse<ExistingMasterDataAuditReport> response = adminController.auditMasterData();

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(report);
        verify(auditor, times(1)).audit();
    }

    @Test
    @DisplayName("AdminController export endpoint delegates to exporter with configured path")
    void adminController_export_defaultPath() throws IOException {
        properties.setExportPath("target/exported-master-data");
        MasterDataBaselineExporter.ExportSummary summary =
                new MasterDataBaselineExporter.ExportSummary(3, 8, 25, Paths.get("target/exported-master-data"));
        when(exporter.exportToDirectory(Paths.get("target/exported-master-data"))).thenReturn(summary);

        ApiResponse<MasterDataBaselineExporter.ExportSummary> response = adminController.exportMasterData(null);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().categoriesExported()).isEqualTo(3);
        verify(exporter, times(1)).exportToDirectory(Paths.get("target/exported-master-data"));
    }

    @Test
    @DisplayName("AdminController export endpoint delegates with custom target directory")
    void adminController_export_customPath() throws IOException {
        String customPath = "custom/export/dir";
        MasterDataBaselineExporter.ExportSummary summary =
                new MasterDataBaselineExporter.ExportSummary(1, 2, 3, Paths.get(customPath));
        when(exporter.exportToDirectory(Paths.get(customPath))).thenReturn(summary);

        ApiResponse<MasterDataBaselineExporter.ExportSummary> response = adminController.exportMasterData(customPath);

        assertThat(response.getData()).isSameAs(summary);
        verify(exporter, times(1)).exportToDirectory(Paths.get(customPath));
    }

    @Test
    @DisplayName("AdminController seed endpoint triggers seeder and returns summary")
    void adminController_seed_delegatesToSeeder() {
        MasterDataSeedSummary summary = MasterDataSeedSummary.builder()
                .validationPassed(true)
                .build();
        when(seeder.seed()).thenReturn(summary);

        ApiResponse<MasterDataSeedSummary> response = adminController.seedMasterData();

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(summary);
        verify(seeder, times(1)).seed();
    }
}

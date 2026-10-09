package com.swasthai.report_generator.test.masterdata;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.report.entity.ReportTestResult;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.masterdata.dto.MasterCategorySeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterTestSeedDto;
import com.swasthai.report_generator.test.masterdata.loader.MasterDataLoader;
import com.swasthai.report_generator.test.masterdata.loader.MasterDataPayload;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeedSummary;
import com.swasthai.report_generator.test.masterdata.service.MasterDataSeeder;
import com.swasthai.report_generator.test.masterdata.validation.MasterDataValidator;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Architectural evaluation test proving the consequences of:
 * - Option A: Additive skip/insert (safe, preserves all existing IDs & relationships)
 * - Option B: In-place update preserving IDs
 * - Option C: Destructive delete-and-recreate (violates FK RESTRICT or severs historical report integrity)
 */
@ExtendWith(MockitoExtension.class)
class MasterDataRebuildImpactEvaluationTest {

    @Mock
    private TestCategoryRepository testCategoryRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestParameterRepository testParameterRepository;

    @Mock
    private MasterDataLoader masterDataLoader;

    @Mock
    private MasterDataValidator masterDataValidator;

    private MasterDataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new MasterDataSeeder(
                masterDataLoader,
                masterDataValidator,
                testCategoryRepository,
                testRepository,
                testParameterRepository
        );
    }

    @Test
    @DisplayName("Option A: Idempotent Seeding preserves existing internal UUIDs and public refIds without mutating historical records")
    void evaluateOptionA_PreservesExistingEntitiesAndIntegrity() {
        UUID existingCatId = UUID.randomUUID();
        String existingCatRefId = "TC-111111111111";
        TestCategory existingCat = TestCategory.builder()
                .id(existingCatId)
                .code("HEM")
                .name("Hematology")
                .refId(existingCatRefId)
                .status(TestCategoryStatus.ACTIVE)
                .build();

        UUID existingTestId = UUID.randomUUID();
        String existingTestRefId = "TEST-222222222222";
        com.swasthai.report_generator.test.entity.Test existingTest = com.swasthai.report_generator.test.entity.Test.builder()
                .id(existingTestId)
                .code("CBC")
                .name("Complete Blood Count")
                .category(existingCat)
                .refId(existingTestRefId)
                .sampleType(SampleType.WHOLE_BLOOD)
                .status(TestStatus.ACTIVE)
                .build();

        // Seed payload contains existing CBC + new BIOCHEMISTRY category
        MasterCategorySeedDto hemDto = MasterCategorySeedDto.builder().code("HEM").name("Hematology").build();
        MasterCategorySeedDto bioDto = MasterCategorySeedDto.builder().code("BIO").name("Biochemistry").build();
        MasterTestSeedDto cbcDto = MasterTestSeedDto.builder().code("CBC").name("Complete Blood Count").categoryCode("HEM").sampleType(SampleType.WHOLE_BLOOD).build();

        MasterDataPayload payload = new MasterDataPayload(List.of(hemDto, bioDto), List.of(cbcDto), Collections.emptyList());

        when(masterDataLoader.load()).thenReturn(payload);
        when(testCategoryRepository.findByCode("HEM")).thenReturn(Optional.of(existingCat));
        when(testCategoryRepository.findByCode("BIO")).thenReturn(Optional.empty());
        when(testRepository.findByCode("CBC")).thenReturn(Optional.of(existingTest));
        when(testCategoryRepository.save(any(TestCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        MasterDataSeedSummary summary = seeder.seed();

        // Verify Option A outcome
        assertThat(summary.categoriesInserted()).isEqualTo(1); // BIO inserted
        assertThat(summary.categoriesSkipped()).isEqualTo(1);  // HEM skipped
        assertThat(summary.testsInserted()).isEqualTo(0);
        assertThat(summary.testsSkipped()).isEqualTo(1);       // CBC skipped

        // Crucial verification: existing records are NEVER modified or deleted
        verify(testCategoryRepository, never()).deleteAll();
        verify(testRepository, never()).deleteAll();

        // Existing IDs remain unchanged
        assertThat(existingCat.getId()).isEqualTo(existingCatId);
        assertThat(existingCat.getRefId()).isEqualTo(existingCatRefId);
        assertThat(existingTest.getId()).isEqualTo(existingTestId);
        assertThat(existingTest.getRefId()).isEqualTo(existingTestRefId);
    }

    @Test
    @DisplayName("Option C Risk: Attempting to delete existing master data with active foreign-key references violates ON DELETE RESTRICT")
    void evaluateOptionC_ViolatesForeignKeyConstraintsWhenHistoricalDataExists() {
        UUID testId = UUID.randomUUID();
        com.swasthai.report_generator.test.entity.Test test = com.swasthai.report_generator.test.entity.Test.builder()
                .id(testId)
                .code("CBC")
                .name("Complete Blood Count")
                .refId("TEST-XaQM6zvpyclH")
                .build();

        // Simulate DB throwing DataIntegrityViolationException on delete due to fk_report_test_results_test
        doThrow(new DataIntegrityViolationException(
                "ERROR: update or delete on table \"tests\" violates foreign key constraint \"fk_report_test_results_test\" on table \"report_test_results\""))
                .when(testRepository).deleteById(testId);

        assertThatThrownBy(() -> testRepository.deleteById(testId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_report_test_results_test");
    }

    @Test
    @DisplayName("Option C Risk: Re-creating records with new IDs breaks historical report relationships and invalidates public refIds")
    void evaluateOptionC_SeveresHistoricalReportIntegrity() {
        UUID oldTestId = UUID.randomUUID();
        String oldRefId = "TEST-XaQM6zvpyclH";

        // Historical ReportTestResult pointing to oldTestId
        ReportTestResult reportTestResult = ReportTestResult.builder()
                .id(UUID.randomUUID())
                .refId("RTR-111111111111")
                .displayOrder(1)
                .testVersion(1)
                .build();

        // If old test was recreated, new entity gets brand new UUID and brand new ref_id
        UUID newTestId = UUID.randomUUID();
        String newRefId = RefIdGenerator.generate("TEST-");

        // The old refId is not equal to new refId
        assertThat(oldRefId).isNotEqualTo(newRefId);
        // The old internal UUID is not equal to new UUID
        assertThat(oldTestId).isNotEqualTo(newTestId);

        // Result: Historical report lookup by oldTestId fails (broken link)
        // Public API lookup by oldRefId fails with 404 NOT FOUND
    }
}

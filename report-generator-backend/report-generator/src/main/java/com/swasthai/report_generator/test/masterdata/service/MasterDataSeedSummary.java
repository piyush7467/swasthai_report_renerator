package com.swasthai.report_generator.test.masterdata.service;

import com.swasthai.report_generator.test.masterdata.validation.MasterDataConflict;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

/**
 * Summary metrics of a master data seed execution.
 */
@Builder
public record MasterDataSeedSummary(
        int categoriesFound,
        int categoriesInserted,
        int categoriesSkipped,

        int testsFound,
        int testsInserted,
        int testsSkipped,

        int parametersFound,
        int parametersInserted,
        int parametersSkipped,

        List<MasterDataConflict> conflicts,
        boolean validationPassed
) {
    public int conflictCount() {
        return conflicts != null ? conflicts.size() : 0;
    }

    public List<MasterDataConflict> getConflicts() {
        return conflicts != null ? conflicts : Collections.emptyList();
    }

    public String toFormattedSummary() {
        return "\n==================================================\n" +
                "       MASTER DATA SEEDING SUMMARY                \n" +
                "==================================================\n" +
                String.format(" Validation:  %s\n", validationPassed ? "PASSED" : "FAILED") +
                "--------------------------------------------------\n" +
                " Categories:\n" +
                String.format("   Found:     %d\n", categoriesFound) +
                String.format("   Inserted:  %d\n", categoriesInserted) +
                String.format("   Skipped:   %d\n", categoriesSkipped) +
                " Tests:\n" +
                String.format("   Found:     %d\n", testsFound) +
                String.format("   Inserted:  %d\n", testsInserted) +
                String.format("   Skipped:   %d\n", testsSkipped) +
                " Parameters:\n" +
                String.format("   Found:     %d\n", parametersFound) +
                String.format("   Inserted:  %d\n", parametersInserted) +
                String.format("   Skipped:   %d\n", parametersSkipped) +
                " Conflicts:\n" +
                String.format("   Detected:  %d\n", conflictCount()) +
                "==================================================";
    }
}

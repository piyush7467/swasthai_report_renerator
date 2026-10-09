package com.swasthai.report_generator.test.masterdata.audit;

import lombok.Builder;

import java.util.Collections;
import java.util.List;

/**
 * Audit metrics and issues discovered in the existing database master data.
 */
@Builder
public record ExistingMasterDataAuditReport(
        // Categories
        int categoriesTotal,
        int categoriesValid,
        int categoriesMissingCode,
        int categoriesDuplicateCode,
        int categoriesInvalidRefId,
        int categoriesConflicts,

        // Tests
        int testsTotal,
        int testsValid,
        int testsMissingCode,
        int testsDuplicateCode,
        int testsInvalidCategoryRelationship,
        int testsInvalidRefId,
        int testsConflicts,

        // Parameters
        int parametersTotal,
        int parametersValid,
        int parametersMissingCode,
        int parametersDuplicateNaturalKey,
        int parametersInvalidTestRelationship,
        int parametersInvalidCalculation,
        int parametersMissingDependency,
        int parametersCircularDependency,
        int parametersConflicts,

        List<MasterDataAuditIssue> issues
) {
    public List<MasterDataAuditIssue> getIssues() {
        return issues != null ? issues : Collections.emptyList();
    }

    public boolean hasIssues() {
        return issues != null && !issues.isEmpty();
    }

    public String toFormattedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==================================================\n");
        sb.append("        EXISTING MASTER DATA AUDIT REPORT         \n");
        sb.append("==================================================\n");
        sb.append("Categories:\n");
        sb.append(String.format("   Total:                        %d\n", categoriesTotal));
        sb.append(String.format("   Valid:                        %d\n", categoriesValid));
        sb.append(String.format("   Missing Code:                 %d\n", categoriesMissingCode));
        sb.append(String.format("   Duplicate Code:               %d\n", categoriesDuplicateCode));
        sb.append(String.format("   Invalid RefId:                %d\n", categoriesInvalidRefId));
        sb.append(String.format("   Conflicts:                    %d\n", categoriesConflicts));
        sb.append("--------------------------------------------------\n");
        sb.append("Tests:\n");
        sb.append(String.format("   Total:                        %d\n", testsTotal));
        sb.append(String.format("   Valid:                        %d\n", testsValid));
        sb.append(String.format("   Missing Code:                 %d\n", testsMissingCode));
        sb.append(String.format("   Duplicate Code:               %d\n", testsDuplicateCode));
        sb.append(String.format("   Invalid Category Rel:         %d\n", testsInvalidCategoryRelationship));
        sb.append(String.format("   Invalid RefId:                %d\n", testsInvalidRefId));
        sb.append(String.format("   Conflicts:                    %d\n", testsConflicts));
        sb.append("--------------------------------------------------\n");
        sb.append("Parameters:\n");
        sb.append(String.format("   Total:                        %d\n", parametersTotal));
        sb.append(String.format("   Valid:                        %d\n", parametersValid));
        sb.append(String.format("   Missing Code:                 %d\n", parametersMissingCode));
        sb.append(String.format("   Duplicate Natural Key:        %d\n", parametersDuplicateNaturalKey));
        sb.append(String.format("   Invalid Test Rel:             %d\n", parametersInvalidTestRelationship));
        sb.append(String.format("   Invalid Calculation:          %d\n", parametersInvalidCalculation));
        sb.append(String.format("   Missing Dependency:           %d\n", parametersMissingDependency));
        sb.append(String.format("   Circular Dependency:          %d\n", parametersCircularDependency));
        sb.append(String.format("   Conflicts:                    %d\n", parametersConflicts));
        sb.append("==================================================\n");

        if (hasIssues()) {
            sb.append("\nISSUES REQUIRING MANUAL MIGRATION / RESOLUTION:\n");
            for (int i = 0; i < issues.size(); i++) {
                sb.append(String.format("\n[%d] %s\n", i + 1, issues.get(i).toFormattedIssue()));
            }
            sb.append("==================================================\n");
        } else {
            sb.append("STATUS: ALL EXISTING RECORDS ARE VALID AND PRODUCTION-READY.\n");
            sb.append("==================================================\n");
        }

        return sb.toString();
    }
}

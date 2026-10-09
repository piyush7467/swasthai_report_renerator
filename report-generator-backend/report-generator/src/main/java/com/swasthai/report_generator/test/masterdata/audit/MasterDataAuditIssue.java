package com.swasthai.report_generator.test.masterdata.audit;

import lombok.Builder;

/**
 * Represents a problem or discrepancy identified during audit of existing database master data.
 */
@Builder
public record MasterDataAuditIssue(
        String entityType,
        String code,
        String refId,
        String relatedEntity,
        String problem,
        String recommendedAction
) {
    public String toFormattedIssue() {
        return String.format(
                "[%s] RefId: '%s'%s%s\n  Problem: %s\n  Action:  %s",
                entityType,
                refId != null ? refId : "N/A",
                code != null ? ", Code: '" + code + "'" : "",
                relatedEntity != null ? ", Related: '" + relatedEntity + "'" : "",
                problem,
                recommendedAction
        );
    }
}

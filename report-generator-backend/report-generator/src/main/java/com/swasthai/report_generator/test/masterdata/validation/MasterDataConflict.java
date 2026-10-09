package com.swasthai.report_generator.test.masterdata.validation;

import lombok.Builder;

/**
 * Represents a detected difference between an existing database record
 * and a corresponding master data JSON seed definition with the same code.
 */
@Builder
public record MasterDataConflict(
        String entityType,
        String code,
        String relatedCode,
        String field,
        Object existingValue,
        Object seedValue
) {
    @Override
    public String toString() {
        return String.format(
                "[%s Code: '%s'%s] Field '%s' differs -> Existing DB: '%s', Seed JSON: '%s'",
                entityType,
                code,
                relatedCode != null ? " (Parent: " + relatedCode + ")" : "",
                field,
                existingValue,
                seedValue
        );
    }
}

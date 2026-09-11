package com.swasthai.report_generator.report.service;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface ReportDeletionBatchExecutor {

    int softDeleteSelectedBatch(
            Collection<String> reportRefIds,
            UUID organizationId,
            UUID deletedByUserId,
            Instant deletionTime,
            Instant eligibilityCutoff
    );

    int softDeleteDateRangeBatch(
            UUID organizationId,
            UUID deletedByUserId,
            Instant startInstant,
            Instant effectiveEndInstant,
            Instant deletionTime,
            int batchSize
    );
}
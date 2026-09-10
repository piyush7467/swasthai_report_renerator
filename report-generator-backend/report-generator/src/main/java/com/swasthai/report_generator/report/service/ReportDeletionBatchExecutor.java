package com.swasthai.report_generator.report.service;

import com.swasthai.report_generator.user.entity.User;

import java.time.Instant;
import java.util.Collection;

public interface ReportDeletionBatchExecutor {

    int softDeleteSelectedBatch(
            Collection<String> reportRefIds,
            User currentUser,
            Instant deletionTime,
            Instant eligibilityCutoff
    );

    int softDeleteDateRangeBatch(
            User currentUser,
            Instant startInstant,
            Instant effectiveEndInstant,
            Instant deletionTime,
            int batchSize
    );
}
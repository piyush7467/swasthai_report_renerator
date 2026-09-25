package com.swasthai.report_generator.user.service;

import java.time.Instant;

public interface StaffCleanupBatchExecutor {

    int cleanupBatch(Instant cutoff, int batchSize);
}

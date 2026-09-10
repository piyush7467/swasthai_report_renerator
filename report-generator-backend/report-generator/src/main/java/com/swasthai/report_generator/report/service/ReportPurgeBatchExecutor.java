package com.swasthai.report_generator.report.service;

import java.time.Instant;

public interface ReportPurgeBatchExecutor {

    int purgeBatch(Instant purgeCutoff, int batchSize);
}

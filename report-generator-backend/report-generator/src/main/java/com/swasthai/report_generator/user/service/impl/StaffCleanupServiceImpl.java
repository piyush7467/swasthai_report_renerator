package com.swasthai.report_generator.user.service.impl;

import com.swasthai.report_generator.user.config.StaffCleanupProperties;
import com.swasthai.report_generator.user.service.StaffCleanupBatchExecutor;
import com.swasthai.report_generator.user.service.StaffCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffCleanupServiceImpl implements StaffCleanupService {

    private final StaffCleanupProperties properties;
    private final StaffCleanupBatchExecutor batchExecutor;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int purgeEligibleInactiveStaff() {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            log.info("Staff cleanup scheduler is disabled via configuration.");
            return 0;
        }

        Instant cutoff = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
        int totalPurged = 0;
        int maxBatches = 100;

        for (int i = 0; i < maxBatches; i++) {
            int batchPurged = batchExecutor.cleanupBatch(cutoff, properties.getBatchSize());
            totalPurged += batchPurged;
            if (batchPurged < properties.getBatchSize()) {
                break;
            }
        }

        return totalPurged;
    }
}

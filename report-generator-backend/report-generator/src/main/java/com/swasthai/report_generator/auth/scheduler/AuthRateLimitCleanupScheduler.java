package com.swasthai.report_generator.auth.scheduler;

import com.swasthai.report_generator.auth.config.AuthRateLimitProperties;
import com.swasthai.report_generator.common.security.DistributedRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled job to prune stale rate limit attempts from PostgreSQL.
 * Reuses the distributed advisory-lock architecture to ensure single-instance execution
 * across application clusters without long table locks or transaction bloat.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRateLimitCleanupScheduler {

    /**
     * PostgreSQL advisory lock key distinct from ReportPurgeScheduler (73918427651984321L).
     */
    private static final long RATE_LIMIT_CLEANUP_LOCK_KEY = 84920518392817263L;

    private final DistributedRateLimiter distributedRateLimiter;
    private final AuthRateLimitProperties rateLimitProperties;
    private final JdbcTemplate jdbcTemplate;

    private final AtomicBoolean isCleanupRunning = new AtomicBoolean(false);

    @Scheduled(
            cron = "${security.rate-limit.cleanup-cron:0 0 * * * *}"
    )
    @Transactional
    public void cleanupStaleRateLimits() {

        if (Boolean.FALSE.equals(rateLimitProperties.getCleanupEnabled())) {
            log.debug("Auth rate limit cleanup is disabled by configuration");
            return;
        }

        if (!isCleanupRunning.compareAndSet(false, true)) {
            log.warn("Rate limit cleanup skipped: previous execution still running in this JVM");
            return;
        }

        try {
            boolean lockAcquired = acquireLock();
            if (!lockAcquired) {
                log.info("Rate limit cleanup skipped: another application instance currently owns the lock");
                return;
            }

            long now = Instant.now().getEpochSecond();
            long staleCutoff = now - rateLimitProperties.getCleanupStaleAfterSeconds();
            int batchSize = rateLimitProperties.getCleanupBatchSize();

            int totalDeleted = 0;
            while (true) {
                int deletedInBatch = distributedRateLimiter.purgeStaleRecords(staleCutoff, batchSize);
                if (deletedInBatch == 0) {
                    break;
                }
                totalDeleted += deletedInBatch;
            }

            if (totalDeleted > 0) {
                log.info("Completed automated auth rate limit cleanup. Purged {} stale records", totalDeleted);
            }

        } catch (Exception ex) {
            log.error("Automated auth rate limit cleanup encountered an error", ex);
        } finally {
            isCleanupRunning.set(false);
        }
    }

    private boolean acquireLock() {
        Boolean acquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(?)",
                Boolean.class,
                RATE_LIMIT_CLEANUP_LOCK_KEY
        );
        return Boolean.TRUE.equals(acquired);
    }
}

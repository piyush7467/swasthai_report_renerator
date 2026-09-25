package com.swasthai.report_generator.user.scheduler;

import com.swasthai.report_generator.user.service.StaffCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaffCleanupScheduler {

    /*
     * PostgreSQL advisory-lock key dedicated for the inactive staff cleanup job.
     */
    private static final long STAFF_CLEANUP_LOCK_KEY = 84920516372849102L;

    private final StaffCleanupService staffCleanupService;
    private final JdbcTemplate jdbcTemplate;

    /*
     * Protects against overlapping executions inside the SAME JVM.
     */
    private final AtomicBoolean isCleanupRunning = new AtomicBoolean(false);

    /**
     * Automated cleanup of 10-day-old inactive staff accounts.
     * Dual protection:
     * 1. AtomicBoolean local JVM lock.
     * 2. PostgreSQL transaction-level advisory lock (pg_try_advisory_xact_lock) for cross-instance safety.
     */
    @Scheduled(
            cron = "${staff.cleanup.cron:0 0 2 * * *}",
            zone = "${staff.cleanup.time-zone:Asia/Kolkata}"
    )
    @Transactional
    public void cleanupInactiveStaff() {
        if (!isCleanupRunning.compareAndSet(false, true)) {
            log.warn("Staff cleanup execution skipped: previous cleanup job is still running in this JVM");
            return;
        }

        try {
            boolean lockAcquired = acquireCleanupLock();
            if (!lockAcquired) {
                log.info("Staff cleanup skipped: another application instance is currently executing the cleanup job");
                return;
            }

            log.info("Starting automated permanent cleanup of 10-day-old inactive lab staff accounts");
            int purged = staffCleanupService.purgeEligibleInactiveStaff();
            log.info("Automated staff cleanup completed. Purged {} inactive accounts", purged);

        } catch (Exception e) {
            log.error("Automated staff cleanup job failed", e);
        } finally {
            isCleanupRunning.set(false);
        }
    }

    private boolean acquireCleanupLock() {
        Boolean acquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(?)",
                Boolean.class,
                STAFF_CLEANUP_LOCK_KEY
        );
        return Boolean.TRUE.equals(acquired);
    }
}

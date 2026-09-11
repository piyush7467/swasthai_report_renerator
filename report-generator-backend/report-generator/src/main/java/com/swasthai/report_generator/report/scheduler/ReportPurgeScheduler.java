package com.swasthai.report_generator.report.scheduler;

import com.swasthai.report_generator.report.service.ReportService;
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
public class ReportPurgeScheduler {

    /*
     * PostgreSQL advisory-lock key.
     *
     * This value is only an identifier for the report-purge job.
     * It does not represent a database ID or application entity ID.
     *
     * All application instances must use the same key so that only
     * one instance can execute the purge job at a time.
     */
    private static final long PURGE_LOCK_KEY = 73918427651984321L;

    private final ReportService reportService;
    private final JdbcTemplate jdbcTemplate;

    /*
     * Protects against overlapping executions inside the SAME JVM.
     *
     * PostgreSQL advisory locking additionally protects against
     * overlapping executions across DIFFERENT application instances.
     */
    private final AtomicBoolean isPurgeRunning = new AtomicBoolean(false);

    /**
     * Automated report purge.
     *
     * Two levels of protection are intentionally used:
     *
     * 1. AtomicBoolean
     *    Prevents duplicate execution inside one JVM.
     *
     * 2. PostgreSQL transaction-level advisory lock
     *    Prevents duplicate execution across multiple JVMs / servers.
     *
     * The advisory lock is transaction-scoped, so it is automatically
     * released when this transaction commits or rolls back.
     */
    @Scheduled(
            cron = "${report.retention.purge-cron}",
            zone = "${report.retention.time-zone}"
    )
    @Transactional
    public void purgeExpiredReports() {

        /*
         * Fast local check.
         *
         * If another purge is already running in this JVM, don't even
         * contact PostgreSQL for the advisory lock.
         */
        if (!isPurgeRunning.compareAndSet(false, true)) {
            log.warn(
                    "Purge execution skipped: previous purge job is still running"
            );
            return;
        }

        try {

            /*
             * IMPORTANT:
             *
             * pg_try_advisory_xact_lock() is transaction-scoped.
             *
             * Because this scheduler method is @Transactional, the
             * advisory lock remains held for the entire scheduler
             * transaction.
             *
             * If another application instance tries to acquire the same
             * lock while this transaction is active, PostgreSQL returns
             * false immediately instead of waiting.
             */
            boolean lockAcquired = acquirePurgeLock();

            if (!lockAcquired) {
                log.info(
                        "Report purge skipped: another application instance " +
                        "is currently executing the purge job"
                );
                return;
            }

            log.info(
                    "Starting automated purge of expired soft-deleted reports"
            );

            /*
             * ReportService.purgeExpiredReports() is intentionally
             * NOT_SUPPORTED.
             *
             * Therefore Spring suspends this scheduler transaction while
             * the actual purge work executes.
             *
             * The PostgreSQL advisory transaction lock remains held by
             * the suspended scheduler transaction and therefore continues
             * protecting the job.
             */
            int purged = reportService.purgeExpiredReports();

            log.info(
                    "Automated report purge completed successfully. " +
                    "Purged {} reports",
                    purged
            );

        } catch (Exception exception) {

            /*
             * Never allow an exception to terminate the scheduler.
             *
             * The surrounding transaction will roll back automatically.
             * Because the advisory lock is transaction-scoped, PostgreSQL
             * will release it automatically during rollback.
             */
            log.error(
                    "Automated report purge failed",
                    exception
            );

        } finally {

            /*
             * Release only the JVM-local guard.
             *
             * We deliberately DO NOT call pg_advisory_unlock().
             *
             * pg_try_advisory_xact_lock() created a transaction-scoped
             * lock, so PostgreSQL releases it automatically when this
             * transaction completes.
             */
            isPurgeRunning.set(false);
        }
    }

    /**
     * Attempts to acquire the PostgreSQL transaction-level advisory lock.
     *
     * Returns:
     *   true  -> this instance owns the purge lock
     *   false -> another instance currently owns the purge lock
     *
     * Fail-closed behavior:
     * if PostgreSQL cannot execute the lock query, the exception is
     * propagated and the purge does NOT run without distributed locking.
     */
    private boolean acquirePurgeLock() {

        Boolean acquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(?)",
                Boolean.class,
                PURGE_LOCK_KEY
        );

        return Boolean.TRUE.equals(acquired);
    }
}
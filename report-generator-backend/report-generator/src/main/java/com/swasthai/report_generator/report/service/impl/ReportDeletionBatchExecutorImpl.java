package com.swasthai.report_generator.report.service.impl;

import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.service.ReportDeletionBatchExecutor;
import com.swasthai.report_generator.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportDeletionBatchExecutorImpl
        implements ReportDeletionBatchExecutor {

    /**
     * Maximum number of attempts for one infrastructure batch.
     *
     * Attempt 1 = normal execution
     * Attempt 2 = retry after optimistic-lock conflict
     * Attempt 3 = final retry
     *
     * This is intentionally bounded so that persistent conflicts
     * cannot cause an infinite loop.
     */
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final ReportRepository reportRepository;
    private final EntityManager entityManager;
    private final PlatformTransactionManager transactionManager;

    // ============================================================
    // SELECTED REPORT DELETION
    // ============================================================

    @Override
    public int softDeleteSelectedBatch(
            Collection<String> reportRefIds,
            UUID organizationId,
            UUID deletedByUserId,
            Instant deletionTime,
            Instant eligibilityCutoff
    ) {
        Objects.requireNonNull(
                reportRefIds,
                "reportRefIds must not be null"
        );

        Objects.requireNonNull(
                organizationId,
                "organizationId must not be null"
        );

        Objects.requireNonNull(
                deletedByUserId,
                "deletedByUserId must not be null"
        );

        Objects.requireNonNull(
                deletionTime,
                "deletionTime must not be null"
        );

        Objects.requireNonNull(
                eligibilityCutoff,
                "eligibilityCutoff must not be null"
        );

        if (reportRefIds.isEmpty()) {
            return 0;
        }

        /*
         * Normalize the IDs defensively here as well.
         *
         * The service should already normalize them, but this executor
         * is an infrastructure boundary and should not depend entirely
         * on upstream normalization.
         */
        List<String> normalizedRefIds = reportRefIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(refId -> !refId.isEmpty())
                .distinct()
                .toList();

        if (normalizedRefIds.isEmpty()) {
            return 0;
        }

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {

            try {
                int deletedCount = executeSelectedDeletionAttempt(
                        normalizedRefIds,
                        organizationId,
                        deletedByUserId,
                        deletionTime,
                        eligibilityCutoff
                );

                if (attempt > 1) {
                    log.info(
                            "Selected report deletion batch succeeded after retry. " +
                            "attempt={}, deletedCount={}",
                            attempt,
                            deletedCount
                    );
                }

                return deletedCount;

            } catch (OptimisticLockingFailureException |
                     OptimisticLockException exception) {

                log.warn(
                        "Optimistic-lock conflict during selected report deletion " +
                        "batch. attempt={}/{}, reportCount={}, organizationId={}",
                        attempt,
                        MAX_RETRY_ATTEMPTS,
                        normalizedRefIds.size(),
                        organizationId
                );

                if (attempt == MAX_RETRY_ATTEMPTS) {
                    log.error(
                            "Selected report deletion batch failed after {} attempts. " +
                            "organizationId={}",
                            MAX_RETRY_ATTEMPTS,
                            organizationId,
                            exception
                    );

                    /*
                     * Re-throw after bounded retries.
                     *
                     * This is important: the caller can still distinguish
                     * a genuine persistent concurrency problem from a
                     * successful partial operation.
                     */
                    throw exception;
                }
            }
        }

        /*
         * Defensive fallback. The loop either returns or throws.
         */
        return 0;
    }

    /**
     * Executes exactly ONE selected-deletion attempt inside a brand-new
     * REQUIRES_NEW transaction.
     *
     * IMPORTANT:
     * Do not add retry logic inside this transaction.
     *
     * If optimistic locking fails, the transaction must roll back completely.
     * The caller then starts a completely new transaction and re-reads the
     * current database state.
     */
    private int executeSelectedDeletionAttempt(
            List<String> reportRefIds,
            UUID organizationId,
            UUID deletedByUserId,
            Instant deletionTime,
            Instant eligibilityCutoff
    ) {
        TransactionTemplate transactionTemplate =
                createRequiresNewTransactionTemplate();

        Integer result = transactionTemplate.execute(status -> {

            User deletedBy =
                    entityManager.getReference(
                            User.class,
                            deletedByUserId
                    );

            /*
             * Re-read the reports on EVERY attempt.
             *
             * This is what makes the retry concurrency-safe.
             *
             * A report deleted by another transaction will now have
             * deletedAt != null and therefore disappear from this query.
             */
            List<Report> reports =
                    reportRepository
                            .findAllByOrganization_IdAndRefIdInAndDeletedAtIsNull(
                                    organizationId,
                                    reportRefIds
                            );

            if (reports.isEmpty()) {
                return 0;
            }

            /*
             * Retention eligibility is checked server-side.
             *
             * createdAt must be strictly before the cutoff.
             */
            List<Report> eligibleReports = reports.stream()
                    .filter(report ->
                            report.getCreatedAt() != null
                                    && report.getCreatedAt()
                                    .isBefore(eligibilityCutoff)
                    )
                    .toList();

            if (eligibleReports.isEmpty()) {
                return 0;
            }

            /*
             * Only server-controlled fields are modified.
             *
             * organization, createdBy, status, version, etc.
             * are never changed by this operation.
             */
            for (Report report : eligibleReports) {
                report.setDeletedAt(deletionTime);
                report.setDeletedBy(deletedBy);
            }

            /*
             * @Version causes Hibernate to detect a concurrent update.
             *
             * If another transaction changed one of these reports after
             * this transaction loaded it, flush() can throw an optimistic
             * locking exception.
             *
             * The surrounding TransactionTemplate then rolls back this
             * entire attempt.
             */
            reportRepository.saveAll(eligibleReports);
            reportRepository.flush();

            /*
             * Clear only after a successful flush.
             *
             * This prevents stale entities from being reused by later work.
             */
            entityManager.clear();

            log.debug(
                    "Selected report deletion batch committed successfully. " +
                    "deletedCount={}, organizationId={}",
                    eligibleReports.size(),
                    organizationId
            );

            return eligibleReports.size();
        });

        return result == null ? 0 : result;
    }

    // ============================================================
    // DATE-RANGE REPORT DELETION
    // ============================================================

    @Override
    public int softDeleteDateRangeBatch(
            UUID organizationId,
            UUID deletedByUserId,
            Instant startInstant,
            Instant effectiveEndInstant,
            Instant deletionTime,
            int batchSize
    ) {
        Objects.requireNonNull(
                organizationId,
                "organizationId must not be null"
        );

        Objects.requireNonNull(
                deletedByUserId,
                "deletedByUserId must not be null"
        );

        Objects.requireNonNull(
                startInstant,
                "startInstant must not be null"
        );

        Objects.requireNonNull(
                effectiveEndInstant,
                "effectiveEndInstant must not be null"
        );

        Objects.requireNonNull(
                deletionTime,
                "deletionTime must not be null"
        );

        if (batchSize < 1) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than zero"
            );
        }

        if (!startInstant.isBefore(effectiveEndInstant)) {
            return 0;
        }

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {

            try {
                int deletedCount = executeDateRangeDeletionAttempt(
                        organizationId,
                        deletedByUserId,
                        startInstant,
                        effectiveEndInstant,
                        deletionTime,
                        batchSize
                );

                if (attempt > 1) {
                    log.info(
                            "Date-range report deletion batch succeeded after retry. " +
                            "attempt={}, deletedCount={}, organizationId={}",
                            attempt,
                            deletedCount,
                            organizationId
                    );
                }

                return deletedCount;

            } catch (OptimisticLockingFailureException |
                     OptimisticLockException exception) {

                log.warn(
                        "Optimistic-lock conflict during date-range report deletion " +
                        "batch. attempt={}/{}, organizationId={}",
                        attempt,
                        MAX_RETRY_ATTEMPTS,
                        organizationId
                );

                if (attempt == MAX_RETRY_ATTEMPTS) {
                    log.error(
                            "Date-range report deletion batch failed after {} attempts. " +
                            "organizationId={}",
                            MAX_RETRY_ATTEMPTS,
                            organizationId,
                            exception
                    );

                    throw exception;
                }
            }
        }

        return 0;
    }

    /**
     * Executes exactly ONE date-range deletion attempt inside a new transaction.
     */
    private int executeDateRangeDeletionAttempt(
            UUID organizationId,
            UUID deletedByUserId,
            Instant startInstant,
            Instant effectiveEndInstant,
            Instant deletionTime,
            int batchSize
    ) {
        TransactionTemplate transactionTemplate =
                createRequiresNewTransactionTemplate();

        Integer result = transactionTemplate.execute(status -> {

            User deletedBy =
                    entityManager.getReference(
                            User.class,
                            deletedByUserId
                    );

            /*
             * Always request page 0.
             *
             * Deleted rows disappear from the active-only query.
             * Therefore the next call naturally gets the next remaining
             * batch.
             */
            Slice<Report> slice =
                    reportRepository.findEligibleForDateRangeDeletion(
                            organizationId,
                            startInstant,
                            effectiveEndInstant,
                            PageRequest.of(0, batchSize)
                    );

            List<Report> batch = slice.getContent();

            if (batch.isEmpty()) {
                return 0;
            }

            for (Report report : batch) {
                report.setDeletedAt(deletionTime);
                report.setDeletedBy(deletedBy);
            }

            /*
             * Optimistic locking is checked at flush().
             *
             * On conflict:
             * 1. transaction rolls back
             * 2. caller catches the exception
             * 3. caller starts a NEW transaction
             * 4. query sees the latest database state
             */
            reportRepository.saveAll(batch);
            reportRepository.flush();

            entityManager.clear();

            log.debug(
                    "Date-range report deletion batch committed successfully. " +
                    "deletedCount={}, organizationId={}",
                    batch.size(),
                    organizationId
            );

            return batch.size();
        });

        return result == null ? 0 : result;
    }

    // ============================================================
    // TRANSACTION HELPER
    // ============================================================

    /**
     * Creates a fresh transaction for every individual attempt.
     *
     * This is deliberately created outside the retry loop's transaction
     * body so that every retry gets a completely new transaction.
     */
    private TransactionTemplate createRequiresNewTransactionTemplate() {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(
                org.springframework.transaction.annotation
                        .Propagation
                        .REQUIRES_NEW
                        .value()
        );

        return transactionTemplate;
    }
}
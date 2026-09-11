package com.swasthai.report_generator.report.service.impl;

import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.service.ReportDeletionBatchExecutor;
import com.swasthai.report_generator.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    private final ReportRepository reportRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int softDeleteSelectedBatch(
            Collection<String> reportRefIds,
            UUID organizationId,
            UUID deletedByUserId,
            Instant deletionTime,
            Instant eligibilityCutoff
    ) {

        validateCommonArguments(
                organizationId,
                deletedByUserId,
                deletionTime
        );

        Objects.requireNonNull(
                reportRefIds,
                "reportRefIds must not be null"
        );

        Objects.requireNonNull(
                eligibilityCutoff,
                "eligibilityCutoff must not be null"
        );

        if (reportRefIds.isEmpty()) {
            return 0;
        }

        /*
         * Resolve the authenticated user INSIDE this REQUIRES_NEW
         * transaction.
         *
         * We intentionally do not pass the User entity from the
         * outer transaction/security context.
         */
        User deletedBy =
                entityManager.getReference(User.class, deletedByUserId);

        List<Report> reports =
                reportRepository
                        .findAllByOrganization_IdAndRefIdInAndDeletedAtIsNull(
                                organizationId,
                                reportRefIds
                        );

        if (reports.isEmpty()) {
            return 0;
        }

        List<Report> eligibleReports = reports.stream()
                .filter(report ->
                        report.getCreatedAt().isBefore(eligibilityCutoff)
                )
                .toList();

        if (eligibleReports.isEmpty()) {
            return 0;
        }

        for (Report report : eligibleReports) {
            report.setDeletedAt(deletionTime);
            report.setDeletedBy(deletedBy);
        }

        /*
         * Only save the reports that were actually modified.
         * Ineligible reports must remain completely untouched.
         */
        reportRepository.saveAll(eligibleReports);
        reportRepository.flush();

        entityManager.clear();

        log.debug(
                "Soft-deleted {} reports in independent selected-deletion transaction",
                eligibleReports.size()
        );

        return eligibleReports.size();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int softDeleteDateRangeBatch(
            UUID organizationId,
            UUID deletedByUserId,
            Instant startInstant,
            Instant effectiveEndInstant,
            Instant deletionTime,
            int batchSize
    ) {

        validateCommonArguments(
                organizationId,
                deletedByUserId,
                deletionTime
        );

        Objects.requireNonNull(
                startInstant,
                "startInstant must not be null"
        );

        Objects.requireNonNull(
                effectiveEndInstant,
                "effectiveEndInstant must not be null"
        );

        if (batchSize < 1) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than zero"
            );
        }

        if (!startInstant.isBefore(effectiveEndInstant)) {
            return 0;
        }

        User deletedBy =
                entityManager.getReference(User.class, deletedByUserId);

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

        reportRepository.saveAll(batch);
        reportRepository.flush();

        entityManager.clear();

        log.debug(
                "Soft-deleted {} reports in independent date-range transaction",
                batch.size()
        );

        return batch.size();
    }

    private void validateCommonArguments(
            UUID organizationId,
            UUID deletedByUserId,
            Instant deletionTime
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
                deletionTime,
                "deletionTime must not be null"
        );
    }
}
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
            User currentUser,
            Instant deletionTime,
            Instant eligibilityCutoff
    ) {

        Organization organization = currentUser.getOrganization();

        if (organization == null) {
            throw new IllegalStateException(
                    "Authenticated organization is required"
            );
        }

        List<Report> reports =
                reportRepository.findAllByOrganization_IdAndRefIdInAndDeletedAtIsNull(
                        organization.getId(),
                        reportRefIds
                );

        if (reports.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;

        for (Report report : reports) {

            if (report.getCreatedAt().isBefore(eligibilityCutoff)) {
                report.setDeletedAt(deletionTime);
                report.setDeletedBy(currentUser);
                deletedCount++;
            }
        }

        if (deletedCount > 0) {
            reportRepository.saveAll(reports);
            reportRepository.flush();
        }

        entityManager.clear();

        log.debug(
                "Soft-deleted {} reports in independent selected-deletion transaction",
                deletedCount
        );

        return deletedCount;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int softDeleteDateRangeBatch(
            User currentUser,
            Instant startInstant,
            Instant effectiveEndInstant,
            Instant deletionTime,
            int batchSize
    ) {

        Organization organization = currentUser.getOrganization();

        if (organization == null) {
            throw new IllegalStateException(
                    "Authenticated organization is required"
            );
        }

        Slice<Report> slice =
                reportRepository.findEligibleForDateRangeDeletion(
                        organization.getId(),
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
            report.setDeletedBy(currentUser);
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
}
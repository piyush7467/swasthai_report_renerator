package com.swasthai.report_generator.report.service.impl;

import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.service.ReportPurgeBatchExecutor;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportPurgeBatchExecutorImpl implements ReportPurgeBatchExecutor {

    private final ReportRepository reportRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeBatch(Instant purgeCutoff, int batchSize) {
        Slice<Report> slice = reportRepository.findEligibleForPurge(purgeCutoff, PageRequest.of(0, batchSize));
        List<Report> batch = slice.getContent();
        if (batch.isEmpty()) {
            return 0;
        }

        reportRepository.deleteAll(batch);
        reportRepository.flush();
        entityManager.clear();

        log.debug("Purged batch of {} expired reports in independent transaction", batch.size());
        return batch.size();
    }
}

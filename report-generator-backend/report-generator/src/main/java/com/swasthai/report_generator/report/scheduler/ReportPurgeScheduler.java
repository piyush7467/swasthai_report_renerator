package com.swasthai.report_generator.report.scheduler;

import com.swasthai.report_generator.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportPurgeScheduler {

    private final ReportService reportService;

    private final AtomicBoolean isPurgeRunning =
            new AtomicBoolean(false);

    @Scheduled(
            cron = "${report.retention.purge-cron}",
            zone = "${report.retention.time-zone}"
    )
    public void purgeExpiredReports() {

        if (!isPurgeRunning.compareAndSet(false, true)) {

            log.warn(
                    "Purge execution skipped: previous purge job is still running"
            );

            return;
        }

        try {

            log.info(
                    "Starting automated purge of expired soft-deleted reports"
            );

            int purged =
                    reportService.purgeExpiredReports();

            log.info(
                    "Automated purge completed successfully. Purged {} reports",
                    purged
            );

        } catch (Exception e) {

            log.error(
                    "Automated report purge failed",
                    e
            );

        } finally {

            isPurgeRunning.set(false);
        }
    }
}
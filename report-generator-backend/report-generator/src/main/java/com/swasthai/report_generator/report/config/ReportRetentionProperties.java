package com.swasthai.report_generator.report.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "report.retention")
public class ReportRetentionProperties {

    @Min(0)
    private long deleteAfterDays;

    @Min(0)
    private long purgeAfterDays;

    @Min(1)
    private int purgeBatchSize;

    @Min(1)
    private int bulkBatchSize = 500;

    @NotNull
    private Boolean purgeEnabled;

    @NotBlank
    private String purgeCron;

    @NotBlank
    private String timeZone;

    @PostConstruct
    public void validate() {
        if (timeZone == null || timeZone.isBlank()) {
            throw new IllegalArgumentException("report.retention.time-zone must not be blank");
        }
        try {
            ZoneId.of(timeZone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid report.retention.time-zone: " + timeZone, e);
        }

        if (purgeCron == null || purgeCron.isBlank()) {
            throw new IllegalArgumentException("report.retention.purge-cron must not be blank");
        }
        try {
            CronExpression.parse(purgeCron);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid report.retention.purge-cron expression: " + purgeCron, e);
        }

        if (purgeBatchSize < 1) {
            throw new IllegalArgumentException("report.retention.purge-batch-size must be at least 1");
        }

        if (bulkBatchSize < 1) {
            throw new IllegalArgumentException("report.retention.bulk-batch-size must be at least 1");
        }

        if (deleteAfterDays < 0) {
            throw new IllegalArgumentException("report.retention.delete-after-days cannot be negative");
        }

        if (purgeAfterDays < 0) {
            throw new IllegalArgumentException("report.retention.purge-after-days cannot be negative");
        }
    }
}
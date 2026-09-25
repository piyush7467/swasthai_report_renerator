package com.swasthai.report_generator.user.config;

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
@ConfigurationProperties(prefix = "staff.cleanup")
public class StaffCleanupProperties {

    @NotNull
    private Boolean enabled = true;

    @NotBlank
    private String cron = "0 0 2 * * *"; // Daily at 2:00 AM

    @NotBlank
    private String timeZone = "Asia/Kolkata";

    @Min(1)
    private int retentionDays = 10;

    @Min(1)
    private int batchSize = 50;

    @PostConstruct
    public void validate() {
        if (timeZone == null || timeZone.isBlank()) {
            throw new IllegalArgumentException("staff.cleanup.time-zone must not be blank");
        }
        try {
            ZoneId.of(timeZone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid staff.cleanup.time-zone: " + timeZone, e);
        }

        if (cron == null || cron.isBlank()) {
            throw new IllegalArgumentException("staff.cleanup.cron must not be blank");
        }
        try {
            CronExpression.parse(cron);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid staff.cleanup.cron expression: " + cron, e);
        }

        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException("staff.cleanup.batch-size must be between 1 and 1000");
        }

        if (retentionDays < 1) {
            throw new IllegalArgumentException("staff.cleanup.retention-days must be at least 1");
        }
    }
}

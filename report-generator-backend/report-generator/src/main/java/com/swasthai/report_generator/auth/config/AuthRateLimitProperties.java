package com.swasthai.report_generator.auth.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.rate-limit")
public class AuthRateLimitProperties {

    @NotNull(message = "security.rate-limit.max-attempts must not be null")
    @Min(value = 1, message = "security.rate-limit.max-attempts must be at least 1")
    @Max(value = 100, message = "security.rate-limit.max-attempts cannot exceed 100")
    private Integer maxAttempts = 5;

    @NotNull(message = "security.rate-limit.window-seconds must not be null")
    @Min(value = 1, message = "security.rate-limit.window-seconds must be at least 1")
    @Max(value = 86400, message = "security.rate-limit.window-seconds cannot exceed 86400")
    private Long windowSeconds = 60L;

    @NotNull(message = "security.rate-limit.lock-seconds must not be null")
    @Min(value = 1, message = "security.rate-limit.lock-seconds must be at least 1")
    @Max(value = 86400, message = "security.rate-limit.lock-seconds cannot exceed 86400")
    private Long lockSeconds = 300L;

    @NotNull(message = "security.rate-limit.cleanup-enabled must not be null")
    private Boolean cleanupEnabled = true;

    @jakarta.validation.constraints.NotBlank(message = "security.rate-limit.cleanup-cron must not be blank")
    private String cleanupCron = "0 0 * * * *";

    @NotNull(message = "security.rate-limit.cleanup-batch-size must not be null")
    @Min(value = 1, message = "security.rate-limit.cleanup-batch-size must be at least 1")
    @Max(value = 5000, message = "security.rate-limit.cleanup-batch-size cannot exceed 5000")
    private Integer cleanupBatchSize = 500;

    @NotNull(message = "security.rate-limit.cleanup-stale-after-seconds must not be null")
    @Min(value = 60, message = "security.rate-limit.cleanup-stale-after-seconds must be at least 60")
    @Max(value = 2592000, message = "security.rate-limit.cleanup-stale-after-seconds cannot exceed 2592000 (30 days)")
    private Long cleanupStaleAfterSeconds = 86400L;

    @PostConstruct
    public void validate() {
        if (maxAttempts == null || maxAttempts <= 0) {
            throw new IllegalArgumentException("security.rate-limit.max-attempts must be positive");
        }
        if (windowSeconds == null || windowSeconds <= 0) {
            throw new IllegalArgumentException("security.rate-limit.window-seconds must be positive");
        }
        if (lockSeconds == null || lockSeconds <= 0) {
            throw new IllegalArgumentException("security.rate-limit.lock-seconds must be positive");
        }
        if (cleanupBatchSize == null || cleanupBatchSize < 1 || cleanupBatchSize > 5000) {
            throw new IllegalArgumentException("security.rate-limit.cleanup-batch-size must be between 1 and 5000");
        }
        if (cleanupStaleAfterSeconds == null || cleanupStaleAfterSeconds < 60) {
            throw new IllegalArgumentException("security.rate-limit.cleanup-stale-after-seconds must be at least 60 seconds");
        }
        if (cleanupCron == null || cleanupCron.isBlank()) {
            throw new IllegalArgumentException("security.rate-limit.cleanup-cron must not be blank");
        }
        try {
            org.springframework.scheduling.support.CronExpression.parse(cleanupCron);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid security.rate-limit.cleanup-cron expression: " + cleanupCron, e);
        }
    }
}

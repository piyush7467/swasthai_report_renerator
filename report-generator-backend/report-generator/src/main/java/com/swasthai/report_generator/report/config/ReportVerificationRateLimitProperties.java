package com.swasthai.report_generator.report.config;

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
@ConfigurationProperties(prefix = "security.rate-limit.verification")
public class ReportVerificationRateLimitProperties {

    @NotNull(message = "security.rate-limit.verification.max-attempts must not be null")
    @Min(value = 1, message = "security.rate-limit.verification.max-attempts must be at least 1")
    @Max(value = 1000, message = "security.rate-limit.verification.max-attempts cannot exceed 1000")
    private Integer maxAttempts = 30;

    @NotNull(message = "security.rate-limit.verification.window-seconds must not be null")
    @Min(value = 1, message = "security.rate-limit.verification.window-seconds must be at least 1")
    @Max(value = 86400, message = "security.rate-limit.verification.window-seconds cannot exceed 86400")
    private Long windowSeconds = 60L;

    @NotNull(message = "security.rate-limit.verification.lock-seconds must not be null")
    @Min(value = 1, message = "security.rate-limit.verification.lock-seconds must be at least 1")
    @Max(value = 86400, message = "security.rate-limit.verification.lock-seconds cannot exceed 86400")
    private Long lockSeconds = 300L;

    @PostConstruct
    public void validate() {
        if (maxAttempts == null || maxAttempts <= 0) {
            throw new IllegalArgumentException("security.rate-limit.verification.max-attempts must be positive");
        }
        if (windowSeconds == null || windowSeconds <= 0) {
            throw new IllegalArgumentException("security.rate-limit.verification.window-seconds must be positive");
        }
        if (lockSeconds == null || lockSeconds <= 0) {
            throw new IllegalArgumentException("security.rate-limit.verification.lock-seconds must be positive");
        }
    }
}

package com.swasthai.report_generator.license.config;

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
@ConfigurationProperties(prefix = "license")
public class LicenseProperties {

    @NotNull(message = "license.duration-days must not be null")
    @Min(value = 1, message = "license.duration-days must be at least 1")
    @Max(value = 3650, message = "license.duration-days cannot exceed 3650")
    private Long durationDays = 365L;

    @PostConstruct
    public void validate() {
        if (durationDays == null || durationDays <= 0) {
            throw new IllegalArgumentException("license.duration-days must be a positive integer");
        }
    }
}

package com.swasthai.report_generator.storage;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    @NotBlank
    private String rootPath;

    @Min(1024)
    @Max(10_485_760)
    private long maxImageSizeBytes = 5_242_880;

    @PostConstruct
    void validate() {

        if (rootPath.isBlank()) {
            throw new IllegalStateException(
                    "storage.root-path must not be blank"
            );
        }
    }
}
package com.swasthai.report_generator.test.masterdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.swasthai.report_generator.test.entity.TestCategoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Seed DTO for laboratory test categories.
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = false)
public record MasterCategorySeedDto(

        @NotBlank(message = "Category code is required")
        @Size(max = 50, message = "Category code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Category description must not exceed 500 characters")
        String description,

        TestCategoryStatus status,

        String sourceFile
) {
    public TestCategoryStatus getEffectiveStatus() {
        return status != null ? status : TestCategoryStatus.ACTIVE;
    }
}

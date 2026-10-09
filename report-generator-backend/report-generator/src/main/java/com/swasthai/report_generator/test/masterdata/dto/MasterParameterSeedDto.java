package com.swasthai.report_generator.test.masterdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.TestParameterDataType;
import com.swasthai.report_generator.test.entity.TestParameterStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Seed DTO for master laboratory test parameters.
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = false)
public record MasterParameterSeedDto(

        @NotBlank(message = "Parameter code is required")
        @Size(max = 50, message = "Parameter code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Test code is required to establish parent relationship")
        @Size(max = 50, message = "Test code must not exceed 50 characters")
        String testCode,

        @NotBlank(message = "Parameter name is required")
        @Size(max = 150, message = "Parameter name must not exceed 150 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Data type is required")
        TestParameterDataType dataType,

        ParameterInputType inputType,

        CalculationType calculationType,

        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        Boolean required,

        @NotNull(message = "Display order is required")
        @Min(value = 1, message = "Display order must be at least 1")
        Integer displayOrder,

        @DecimalMin(value = "0", message = "Reference minimum cannot be negative")
        BigDecimal referenceMin,

        @DecimalMin(value = "0", message = "Reference maximum cannot be negative")
        BigDecimal referenceMax,

        @DecimalMin(value = "0", message = "Critical low cannot be negative")
        BigDecimal criticalLow,

        @DecimalMin(value = "0", message = "Critical high cannot be negative")
        BigDecimal criticalHigh,

        @Size(max = 500, message = "Report description must not exceed 500 characters")
        String reportDescription,

        String interpretationGuidance,

        TestParameterStatus status,

        String sourceFile
) {
    public ParameterInputType getEffectiveInputType() {
        return inputType != null ? inputType : ParameterInputType.MANUAL;
    }

    public CalculationType getEffectiveCalculationType() {
        return calculationType != null ? calculationType : CalculationType.NONE;
    }

    public boolean isEffectiveRequired() {
        return required == null || Boolean.TRUE.equals(required);
    }

    public TestParameterStatus getEffectiveStatus() {
        return status != null ? status : TestParameterStatus.ACTIVE;
    }
}

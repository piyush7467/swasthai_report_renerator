package com.swasthai.report_generator.test.dto.request;

import com.swasthai.report_generator.test.entity.TestParameterDataType;
import com.swasthai.report_generator.test.entity.TestParameterStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateTestParameterRequest(

        @Size(max = 50, message = "Parameter code must not exceed 50 characters")
        String code,

        @Size(max = 150, message = "Parameter name must not exceed 150 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        TestParameterDataType dataType,

        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        Boolean required,

        @Min(
                value = 1,
                message = "Display order must be at least 1"
        )
        Integer displayOrder,

        @DecimalMin(
                value = "0",
                message = "Reference minimum cannot be negative"
        )
        BigDecimal referenceMin,

        @DecimalMin(
                value = "0",
                message = "Reference maximum cannot be negative"
        )
        BigDecimal referenceMax,

        @DecimalMin(
                value = "0",
                message = "Critical low cannot be negative"
        )
        BigDecimal criticalLow,

        @DecimalMin(
                value = "0",
                message = "Critical high cannot be negative"
        )
        BigDecimal criticalHigh,

        @Size(max = 500, message = "Report description must not exceed 500 characters")
        String reportDescription,

        String interpretationGuidance,

        TestParameterStatus status
) {
}
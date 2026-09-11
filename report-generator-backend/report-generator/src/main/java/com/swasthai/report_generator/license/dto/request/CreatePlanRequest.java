package com.swasthai.report_generator.license.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePlanRequest(

        @NotBlank
        @Size(max = 50)
        @Pattern(
                regexp = "^[A-Za-z0-9_-]+$",
                message = "Plan code contains invalid characters"
        )
        String code,

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal annualPrice,

        @NotBlank
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Currency must be a 3-letter ISO-style code"
        )
        String currency,

        Boolean active
) {
}
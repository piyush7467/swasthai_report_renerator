package com.swasthai.report_generator.test.dto.request;

import jakarta.validation.constraints.Size;

public record ParameterValueInput(
        @Size(max = 50, message = "Parameter code must not exceed 50 characters")
        String parameterCode,

        @Size(max = 30, message = "Parameter refId must not exceed 30 characters")
        String parameterRefId,

        String value
) {
}

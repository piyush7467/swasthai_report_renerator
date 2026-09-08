package com.swasthai.report_generator.test.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Client input for a single test parameter result.
 *
 * Untrusted client input:
 * - Only parameterRefId or parameterCode and the raw value are accepted.
 * - Server-controlled fields (numericValue, flag, calculationType,
 *   calculationVersion, reference ranges, critical ranges, inputType,
 *   displayOrder) are NEVER accepted from client requests.
 */
public record TestParameterResultInput(
        @Size(max = 30, message = "Parameter refId must not exceed 30 characters")
        String parameterRefId,

        @Size(max = 50, message = "Parameter code must not exceed 50 characters")
        String parameterCode,

        @Size(max = 500, message = "Value must not exceed 500 characters")
        String value
) {
}
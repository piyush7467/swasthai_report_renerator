package com.swasthai.report_generator.test.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Request payload for creating a patient test result.
 *
 * Security:
 * - Every field is treated as untrusted.
 * - Server-controlled fields (organizationId, result refId, status, version,
 *   calculated values, flags, timestamps) are strictly rejected/not accepted.
 * - Uses public immutable refId values (testRefId, patientRefId, parameterRefId).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestResultRequest {

    @NotBlank(message = "Test reference ID is required")
    @Size(max = 30, message = "Test reference ID must not exceed 30 characters")
    private String testRefId;

    @NotBlank(message = "Patient reference ID is required")
    @Size(max = 50, message = "Patient reference ID must not exceed 50 characters")
    private String patientRefId;

    private Instant performedAt;

    @NotEmpty(message = "At least one parameter value is required")
    @Size(max = 100, message = "Cannot submit more than 100 parameters")
    private List<@Valid TestParameterResultInput> parameters;
}
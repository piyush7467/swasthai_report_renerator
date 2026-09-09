package com.swasthai.report_generator.test.dto.response;

import com.swasthai.report_generator.test.entity.PatientTestResultStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Public response DTO for a patient test result.
 *
 * Security & Design:
 * - Read-only representation.
 * - Uses immutable public refIds (refId, organizationRefId, testRefId, patientRefId)
 *   and NEVER leaks internal database UUIDs.
 * - Encapsulates full snapshot of parameter results.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultResponse {

    private String refId;

    // Organization details
    private String organizationRefId;
    private String organizationName;

    // Test details
    private String testRefId;
    private String testCode;
    private String testName;

    // Patient reference (external system reference)
    private String patientRefId;

    // Result status & metadata
    private PatientTestResultStatus status;
    private Integer resultVersion;

    private Instant performedAt;
    private Instant finalizedAt;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;

    // Parameter results
    private List<TestParameterResultResponse> parameters;
}
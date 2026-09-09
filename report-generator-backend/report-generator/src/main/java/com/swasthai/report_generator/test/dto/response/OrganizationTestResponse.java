package com.swasthai.report_generator.test.dto.response;

import com.swasthai.report_generator.test.entity.OrganizationTestStatus;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationTestResponse {

    private String refId;

    // Organization information
    private String organizationRefId;
    private String organizationName;

    // Test information
    private String testRefId;
    private String testCode;
    private String testName;
    private String testType;

    // Assignment
    private OrganizationTestStatus status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
}
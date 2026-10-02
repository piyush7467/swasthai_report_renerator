package com.swasthai.report_generator.organization.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgActivityItemResponse {
    private String id;
    private String type; // REPORT_CREATED, REPORT_FINALIZED, PATIENT_REGISTERED
    private String description;
    private String userName;
    private String time;
    private Instant createdAt;
}

package com.swasthai.report_generator.patient.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class PatientResponse {

    private String refId;

    private String patientCode;

    private String name;

    private LocalDate dateOfBirth;

    private com.swasthai.report_generator.patient.entity.Gender gender;

    private String phone;

    private String email;

    private String address;

    private String organizationRefId;

    private Instant createdAt;

    private Instant updatedAt;
}
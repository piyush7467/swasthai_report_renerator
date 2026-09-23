package com.swasthai.report_generator.patient.dto.response;

import com.swasthai.report_generator.patient.entity.AgeUnit;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Salutation;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class PatientResponse {

    private String refId;

    private String patientCode;

    private String salutation;

    private String name;

    private boolean dateOfBirthKnown;

    private LocalDate dateOfBirth;

    /*
     * Human-readable calculated/current age.
     *
     * Examples:
     * 25 Years
     * 10 Months
     * 2 Weeks
     * 5 Days
     */
    private Integer ageValue;

    private AgeUnit ageUnit;

    private Gender gender;

    private String phone;

    private String email;

    private String address;

    private BigDecimal weightKg;

    private String organizationRefId;

    private Long totalReports;

    private Instant lastReportDate;

    private Instant createdAt;

    private Instant updatedAt;
}
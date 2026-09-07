package com.swasthai.report_generator.test.dto.response;

import com.swasthai.report_generator.test.entity.SampleType;
import com.swasthai.report_generator.test.entity.TestStatus;
import com.swasthai.report_generator.test.entity.TestType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class TestResponse {

    private String refId;

    private String categoryRefId;
    private String categoryName;

    private String code;
    private String name;
    private String shortName;

    private TestType testType;
    private String description;

    // Sample information
    private SampleType sampleType;
    private String customSampleType;
    private String specimenContainer;
    private BigDecimal sampleVolume;
    private String sampleVolumeUnit;
    private boolean fastingRequired;
    private String patientPreparation;
    private String collectionInstructions;

    // Processing
    private Integer turnaroundTimeHours;
    private boolean prioritySupported;
    private boolean outsourced;
    private String laboratoryInstructions;

    // Reporting
    private String reportSection;
    private Integer displayOrder;
    private String reportDescription;
    private String interpretationGuidance;

    // Commercial
    private BigDecimal basePrice;
    private String currency;
    private String billingCode;

    // Lifecycle
    private TestStatus status;
    private Integer version;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;

    // Audit timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
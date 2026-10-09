package com.swasthai.report_generator.test.masterdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.swasthai.report_generator.test.entity.SampleType;
import com.swasthai.report_generator.test.entity.TestStatus;
import com.swasthai.report_generator.test.entity.TestType;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Seed DTO for master laboratory tests.
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = false)
public record MasterTestSeedDto(

        @NotBlank(message = "Test code is required")
        @Size(max = 50, message = "Test code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Test name is required")
        @Size(max = 150, message = "Test name must not exceed 150 characters")
        String name,

        @Size(max = 75, message = "Short name must not exceed 75 characters")
        String shortName,

        @NotBlank(message = "Category code is required to establish parent relationship")
        @Size(max = 50, message = "Category code must not exceed 50 characters")
        String categoryCode,

        TestType testType,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Sample type is required")
        SampleType sampleType,

        @Size(max = 100, message = "Custom sample type must not exceed 100 characters")
        String customSampleType,

        @Size(max = 150, message = "Specimen container must not exceed 150 characters")
        String specimenContainer,

        @DecimalMin(value = "0.01", message = "Sample volume must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Sample volume must have at most 8 integer digits and 2 decimal places")
        BigDecimal sampleVolume,

        @Size(max = 20, message = "Sample volume unit must not exceed 20 characters")
        String sampleVolumeUnit,

        Boolean fastingRequired,

        @Size(max = 1000, message = "Patient preparation must not exceed 1000 characters")
        String patientPreparation,

        @Size(max = 1500, message = "Collection instructions must not exceed 1500 characters")
        String collectionInstructions,

        @Min(value = 1, message = "Turnaround time must be at least 1 hour")
        Integer turnaroundTimeHours,

        Boolean prioritySupported,

        Boolean outsourced,

        @Size(max = 1500, message = "Laboratory instructions must not exceed 1500 characters")
        String laboratoryInstructions,

        @Size(max = 100, message = "Report section must not exceed 100 characters")
        String reportSection,

        @Min(value = 0, message = "Display order cannot be negative")
        Integer displayOrder,

        @Size(max = 1000, message = "Report description must not exceed 1000 characters")
        String reportDescription,

        @Size(max = 2000, message = "Interpretation guidance must not exceed 2000 characters")
        String interpretationGuidance,

        @DecimalMin(value = "0.00", message = "Base price cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Base price must have at most 10 integer digits and 2 decimal places")
        BigDecimal basePrice,

        @Size(min = 3, max = 3, message = "Currency must be a 3-character ISO currency code")
        String currency,

        @Size(max = 50, message = "Billing code must not exceed 50 characters")
        String billingCode,

        TestStatus status,

        String sourceFile
) {
    public TestType getEffectiveTestType() {
        return testType != null ? testType : TestType.INDIVIDUAL;
    }

    public TestStatus getEffectiveStatus() {
        return status != null ? status : TestStatus.ACTIVE;
    }

    public String getEffectiveCurrency() {
        return currency != null && !currency.isBlank() ? currency.trim().toUpperCase() : "INR";
    }

    public boolean isEffectiveFastingRequired() {
        return Boolean.TRUE.equals(fastingRequired);
    }

    public boolean isEffectivePrioritySupported() {
        return Boolean.TRUE.equals(prioritySupported);
    }

    public boolean isEffectiveOutsourced() {
        return Boolean.TRUE.equals(outsourced);
    }

    public int getEffectiveDisplayOrder() {
        return displayOrder != null ? displayOrder : 0;
    }
}

package com.swasthai.report_generator.report.pdf;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record PatientPdfSnapshot(
        String patientCode,
        String name,
        String salutation,
        String gender,
        Boolean dateOfBirthKnown,
        LocalDate dateOfBirth,
        Integer ageValue,
        String ageUnit,
        String phone,
        String email,
        String address,
        BigDecimal weightKg
) {
}
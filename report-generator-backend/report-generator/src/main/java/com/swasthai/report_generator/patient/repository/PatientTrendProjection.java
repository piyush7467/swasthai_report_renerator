package com.swasthai.report_generator.patient.repository;

import java.time.LocalDate;

public interface PatientTrendProjection {
    LocalDate getRegistrationDate();
    Long getPatientCount();
}

package com.swasthai.report_generator.patient.service;

import java.util.List;

import com.swasthai.report_generator.patient.dto.request.CreatePatientRequest;
import com.swasthai.report_generator.patient.dto.response.PatientResponse;

public interface PatientService {

    PatientResponse createPatient(CreatePatientRequest request);

    PatientResponse getPatient(String patientRefId);

    List<PatientResponse> getPatients();

    com.swasthai.report_generator.common.response.PagedResponse<PatientResponse> getPatients(
            int page,
            int size,
            String sortBy,
            String sortDirection
    );
}
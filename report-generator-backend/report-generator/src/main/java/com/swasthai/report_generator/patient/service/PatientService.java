package com.swasthai.report_generator.patient.service;

import com.swasthai.report_generator.common.response.PagedResponse;
import com.swasthai.report_generator.patient.dto.request.CreatePatientRequest;
import com.swasthai.report_generator.patient.dto.request.UpdatePatientRequest;
import com.swasthai.report_generator.patient.dto.response.PatientResponse;

public interface PatientService {

    PatientResponse createPatient(
            CreatePatientRequest request
    );

    PatientResponse getPatient(
            String patientRefId
    );

    PagedResponse<PatientResponse> getPatients(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            String search
    );

    PatientResponse updatePatient(
            String patientRefId,
            UpdatePatientRequest request
    );

    void deletePatient(
            String patientRefId
    );

    PatientResponse restorePatient(
            String patientRefId
    );
}
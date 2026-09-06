package com.swasthai.report_generator.patient.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.patient.dto.request.CreatePatientRequest;
import com.swasthai.report_generator.patient.dto.response.PatientResponse;
import com.swasthai.report_generator.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.swasthai.report_generator.common.response.PagedResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ORG_ADMIN', 'LAB_STAFF')")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            @Valid @RequestBody CreatePatientRequest request) {

        PatientResponse response = patientService.createPatient(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.<PatientResponse>builder()
                                .success(true)
                                .message("Patient created successfully.")
                                .data(response)
                                .build());
    }

    @GetMapping("/{patientRefId}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatient(
            @PathVariable String patientRefId) {

        PatientResponse response = patientService.getPatient(patientRefId);

        return ResponseEntity.ok(
                ApiResponse.<PatientResponse>builder()
                        .success(true)
                        .message("Patient retrieved successfully.")
                        .data(response)
                        .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PatientResponse>>> getPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {

        PagedResponse<PatientResponse> response =
                patientService.getPatients(page, size, sortBy, sortDirection);

        return ResponseEntity.ok(
                ApiResponse.<PagedResponse<PatientResponse>>builder()
                        .success(true)
                        .message("Patients retrieved successfully.")
                        .data(response)
                        .build());
    }

}
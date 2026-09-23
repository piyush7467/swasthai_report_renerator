package com.swasthai.report_generator.patient.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.common.response.PagedResponse;
import com.swasthai.report_generator.patient.dto.request.CreatePatientRequest;
import com.swasthai.report_generator.patient.dto.request.UpdatePatientRequest;
import com.swasthai.report_generator.patient.dto.response.PatientResponse;
import com.swasthai.report_generator.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final com.swasthai.report_generator.report.service.ReportService reportService;

    // ============================================================
    // CREATE
    // ============================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'LAB_STAFF')")
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            @Valid @RequestBody CreatePatientRequest request
    ) {

        PatientResponse response =
                patientService.createPatient(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.<PatientResponse>builder()
                                .success(true)
                                .message(
                                        "Patient created successfully."
                                )
                                .data(response)
                                .build()
                );
    }

    // ============================================================
    // GET ONE
    // ============================================================

    @GetMapping("/{patientRefId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'LAB_STAFF')")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatient(
            @PathVariable String patientRefId
    ) {

        PatientResponse response =
                patientService.getPatient(patientRefId);

        return ResponseEntity.ok(
                ApiResponse.<PatientResponse>builder()
                        .success(true)
                        .message(
                                "Patient retrieved successfully."
                        )
                        .data(response)
                        .build()
        );
    }

    // ============================================================
    // GET PATIENT REPORTS
    // ============================================================

    @GetMapping("/{patientRefId}/reports")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'LAB_STAFF')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<com.swasthai.report_generator.report.dto.response.ReportResponse>>> getPatientReports(
            @PathVariable String patientRefId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
        String safeSort = switch (sortBy) {
            case "createdAt", "updatedAt", "status" -> sortBy;
            default -> "createdAt";
        };
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, Math.min(Math.max(1, size), 100), org.springframework.data.domain.Sort.by(direction, safeSort));

        org.springframework.data.domain.Page<com.swasthai.report_generator.report.dto.response.ReportResponse> response =
                reportService.getPatientReports(patientRefId, pageable);

        return ResponseEntity.ok(
                ApiResponse.<org.springframework.data.domain.Page<com.swasthai.report_generator.report.dto.response.ReportResponse>>builder()
                        .success(true)
                        .message("Patient reports retrieved successfully.")
                        .data(response)
                        .build()
        );
    }

    // ============================================================
    // LIST / SEARCH
    // ============================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'LAB_STAFF')")
    public ResponseEntity<ApiResponse<PagedResponse<PatientResponse>>> getPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String search
    ) {

        PagedResponse<PatientResponse> response =
                patientService.getPatients(
                        page,
                        size,
                        sortBy,
                        sortDirection,
                        search
                );

        return ResponseEntity.ok(
                ApiResponse.<PagedResponse<PatientResponse>>builder()
                        .success(true)
                        .message(
                                "Patients retrieved successfully."
                        )
                        .data(response)
                        .build()
        );
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @PutMapping("/{patientRefId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable String patientRefId,
            @Valid @RequestBody UpdatePatientRequest request
    ) {

        PatientResponse response =
                patientService.updatePatient(
                        patientRefId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.<PatientResponse>builder()
                        .success(true)
                        .message(
                                "Patient updated successfully."
                        )
                        .data(response)
                        .build()
        );
    }

    // ============================================================
    // SOFT DELETE
    // ============================================================

    @DeleteMapping("/{patientRefId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePatient(
            @PathVariable String patientRefId
    ) {

        patientService.deletePatient(
                patientRefId
        );

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message(
                                "Patient deleted successfully."
                        )
                        .build()
        );
    }

    // ============================================================
    // RESTORE
    // ============================================================

    @PatchMapping("/{patientRefId}/restore")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> restorePatient(
            @PathVariable String patientRefId
    ) {

        PatientResponse response =
                patientService.restorePatient(
                        patientRefId
                );

        return ResponseEntity.ok(
                ApiResponse.<PatientResponse>builder()
                        .success(true)
                        .message(
                                "Patient restored successfully."
                        )
                        .data(response)
                        .build()
        );
    }
}
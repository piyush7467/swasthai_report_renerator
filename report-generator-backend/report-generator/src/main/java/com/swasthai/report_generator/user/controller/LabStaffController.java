package com.swasthai.report_generator.user.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.user.dto.request.CreateLabStaffRequest;
import com.swasthai.report_generator.user.dto.request.UpdateUserStatusRequest;
import com.swasthai.report_generator.user.dto.response.LabStaffDetailsResponse;
import com.swasthai.report_generator.user.dto.response.LabStaffSummaryResponse;
import com.swasthai.report_generator.user.dto.response.UserPageResponse;
import com.swasthai.report_generator.user.dto.response.UserResponse;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.service.LabStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/lab-staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class LabStaffController {

    private final LabStaffService labStaffService;

    // ============================================================
    // SUMMARY / LICENSE LIMIT
    // ============================================================

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<LabStaffSummaryResponse>> getLabStaffSummary() {
        LabStaffSummaryResponse summary = labStaffService.getLabStaffSummary();

        return ResponseEntity.ok(
                ApiResponse.<LabStaffSummaryResponse>builder()
                        .success(true)
                        .message("Lab staff summary retrieved successfully.")
                        .data(summary)
                        .build()
        );
    }

    // ============================================================
    // LIST
    // ============================================================

    @GetMapping
    public ResponseEntity<ApiResponse<UserPageResponse>> getLabStaffList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search
    ) {
        UserPageResponse response = labStaffService.getLabStaffList(
                page,
                size,
                sortBy,
                sortDirection,
                status,
                search
        );

        return ResponseEntity.ok(
                ApiResponse.<UserPageResponse>builder()
                        .success(true)
                        .message("Lab staff list retrieved successfully.")
                        .data(response)
                        .build()
        );
    }

    // ============================================================
    // GET DETAILS
    // ============================================================

    @GetMapping("/{refId}")
    public ResponseEntity<ApiResponse<LabStaffDetailsResponse>> getLabStaffDetails(
            @PathVariable String refId
    ) {
        LabStaffDetailsResponse response = labStaffService.getLabStaffDetails(refId);

        return ResponseEntity.ok(
                ApiResponse.<LabStaffDetailsResponse>builder()
                        .success(true)
                        .message("Lab staff details retrieved successfully.")
                        .data(response)
                        .build()
        );
    }

    // ============================================================
    // CREATE
    // ============================================================

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createLabStaff(
            @Valid @RequestBody CreateLabStaffRequest request
    ) {
        UserResponse response = labStaffService.createLabStaff(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("Lab staff account created successfully.")
                        .data(response)
                        .build()
        );
    }

    // ============================================================
    // UPDATE STATUS (ACTIVATE / DEACTIVATE)
    // ============================================================

    @PatchMapping("/{refId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateLabStaffStatus(
            @PathVariable String refId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        UserResponse response = labStaffService.updateLabStaffStatus(refId, request);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("Lab staff status updated successfully.")
                        .data(response)
                        .build()
        );
    }

    // ============================================================
    // DEACTIVATE (DEDICATED ENDPOINT)
    // ============================================================

    @PatchMapping("/{refId}/deactivate")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateLabStaff(
            @PathVariable String refId
    ) {
        UserResponse response = labStaffService.deactivateLabStaff(refId);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("Lab staff account deactivated successfully.")
                        .data(response)
                        .build()
        );
    }

    // ============================================================
    // DELETE
    // ============================================================

    @DeleteMapping("/{refId}")
    public ResponseEntity<ApiResponse<Void>> deleteLabStaff(
            @PathVariable String refId
    ) {
        labStaffService.deleteLabStaff(refId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Lab staff member deleted successfully.")
                        .build()
        );
    }
}

package com.swasthai.report_generator.organization.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.organization.dto.request.CreateOrganizationRequest;
import com.swasthai.report_generator.organization.dto.request.UpdateOrganizationRequest;
import com.swasthai.report_generator.organization.dto.request.UpdateOrganizationStatusRequest;
import com.swasthai.report_generator.organization.dto.response.OrganizationPageResponse;
import com.swasthai.report_generator.organization.dto.response.OrganizationResponse;
import com.swasthai.report_generator.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class OrganizationController {

    private final OrganizationService organizationService;

    // ============================================================
    // CREATE
    // ============================================================

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request
    ) {

        OrganizationResponse response =
                organizationService.createOrganization(request);

        ApiResponse<OrganizationResponse> apiResponse =
                ApiResponse.<OrganizationResponse>builder()
                        .success(true)
                        .message("Organization created successfully.")
                        .data(response)
                        .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(apiResponse);
    }

    // ============================================================
    // LIST
    // ============================================================

    @GetMapping
    public ResponseEntity<ApiResponse<OrganizationPageResponse>> getOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {

        OrganizationPageResponse response =
                organizationService.getOrganizations(
                        page,
                        size,
                        sortBy,
                        sortDirection
                );

        ApiResponse<OrganizationPageResponse> apiResponse =
                ApiResponse.<OrganizationPageResponse>builder()
                        .success(true)
                        .message("Organizations retrieved successfully.")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    // ============================================================
    // GET BY REF ID
    // ============================================================

    @GetMapping("/{refId}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(
            @PathVariable String refId
    ) {

        OrganizationResponse response =
                organizationService.getOrganizationByRefId(refId);

        ApiResponse<OrganizationResponse> apiResponse =
                ApiResponse.<OrganizationResponse>builder()
                        .success(true)
                        .message("Organization retrieved successfully.")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    // ============================================================
    // UPDATE DETAILS
    // ============================================================

    @PutMapping("/{refId}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
            @PathVariable String refId,
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {

        OrganizationResponse response =
                organizationService.updateOrganization(
                        refId,
                        request
                );

        ApiResponse<OrganizationResponse> apiResponse =
                ApiResponse.<OrganizationResponse>builder()
                        .success(true)
                        .message("Organization updated successfully.")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    // ============================================================
    // UPDATE STATUS
    // ============================================================

    @PatchMapping("/{refId}/status")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateStatus(
            @PathVariable String refId,
            @Valid @RequestBody UpdateOrganizationStatusRequest request
    ) {

        OrganizationResponse response =
                organizationService.updateOrganizationStatus(
                        refId,
                        request
                );

        ApiResponse<OrganizationResponse> apiResponse =
                ApiResponse.<OrganizationResponse>builder()
                        .success(true)
                        .message("Organization status updated successfully.")
                        .data(response)
                        .build();

        return ResponseEntity.ok(apiResponse);
    }
}
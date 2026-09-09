package com.swasthai.report_generator.organization.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.organization.dto.request.CreateOrganizationRequest;
import com.swasthai.report_generator.organization.dto.response.OrganizationResponse;
import com.swasthai.report_generator.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

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

    
}
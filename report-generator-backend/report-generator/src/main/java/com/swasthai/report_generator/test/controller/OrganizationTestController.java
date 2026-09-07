package com.swasthai.report_generator.test.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.test.dto.request.AssignTestRequest;
import com.swasthai.report_generator.test.dto.request.UpdateOrganizationTestRequest;
import com.swasthai.report_generator.test.dto.response.OrganizationTestResponse;
import com.swasthai.report_generator.test.service.OrganizationTestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organization-tests")
@RequiredArgsConstructor
@Validated
public class OrganizationTestController {

    private final OrganizationTestService organizationTestService;

    // ============================================================
    // SUPER_ADMIN
    // ============================================================

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<OrganizationTestResponse> assignTest(
            @Valid @RequestBody AssignTestRequest request
    ) {

        return ApiResponse.success(
                "Test assigned to organization successfully",
                organizationTestService.assignTest(request)
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Page<OrganizationTestResponse>>
    getAllAssignments(

            @RequestParam
            String organizationRefId,

            @RequestParam(required = false)
            String status,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "Page must be greater than or equal to 0"
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "Size must be at least 1"
            )
            @Max(
                    value = 100,
                    message = "Size must not exceed 100"
            )
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sort,

            @RequestParam(defaultValue = "desc")
            String direction
    ) {

        Pageable pageable = createPageable(
                page,
                size,
                sort,
                direction
        );

        return ApiResponse.success(
                "Organization test assignments retrieved successfully",
                organizationTestService.getAllAssignments(
                        organizationRefId,
                        status,
                        pageable
                )
        );
    }

    @GetMapping("/{refId}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'LAB_STAFF')"
    )
    public ApiResponse<OrganizationTestResponse>
    getAssignment(
            @PathVariable String refId
    ) {

        return ApiResponse.success(
                "Organization test assignment retrieved successfully",
                organizationTestService.getAssignment(refId)
        );
    }

    @PatchMapping("/{refId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<OrganizationTestResponse>
    updateAssignment(

            @PathVariable String refId,

            @Valid
            @RequestBody
            UpdateOrganizationTestRequest request
    ) {

        return ApiResponse.success(
                "Organization test assignment updated successfully",
                organizationTestService.updateAssignment(
                        refId,
                        request
                )
        );
    }

    @DeleteMapping("/{refId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> deactivateAssignment(
            @PathVariable String refId
    ) {

        organizationTestService.deactivateAssignment(refId);

        return ApiResponse.success(
                "Organization test assignment deactivated successfully",
                null
        );
    }

    // ============================================================
    // ORG_ADMIN / LAB_STAFF
    // ============================================================

    @GetMapping("/my")
    @PreAuthorize(
            "hasAnyRole('ORG_ADMIN', 'LAB_STAFF')"
    )
    public ApiResponse<Page<OrganizationTestResponse>>
    getMyOrganizationTests(

            @RequestParam(required = false)
            String status,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "Page must be greater than or equal to 0"
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "Size must be at least 1"
            )
            @Max(
                    value = 100,
                    message = "Size must not exceed 100"
            )
            int size,

            @RequestParam(defaultValue = "test.name")
            String sort,

            @RequestParam(defaultValue = "asc")
            String direction
    ) {

        Pageable pageable = createPageable(
                page,
                size,
                sort,
                direction
        );

        return ApiResponse.success(
                "Organization tests retrieved successfully",
                organizationTestService.getMyOrganizationTests(
                        status,
                        pageable
                )
        );
    }

    // ============================================================
    // SAFE SORTING
    // ============================================================

    private Pageable createPageable(
            int page,
            int size,
            String sort,
            String direction
    ) {

        String sortField =
                validateSortField(sort);

        Sort.Direction sortDirection =
                parseSortDirection(direction);

        return PageRequest.of(
                page,
                size,
                Sort.by(
                        sortDirection,
                        sortField
                )
        );
    }

    private String validateSortField(
            String sort
    ) {

        if (sort == null || sort.isBlank()) {
            return "createdAt";
        }

        return switch (sort.trim()) {

            case "createdAt" -> "createdAt";

            case "updatedAt" -> "updatedAt";

            case "effectiveFrom" ->
                    "effectiveFrom";

            case "effectiveUntil" ->
                    "effectiveUntil";

            case "status" ->
                    "status";

            /*
             * These are nested properties and Spring Data JPA
             * supports sorting through the entity relationship.
             */
            case "test.name" ->
                    "test.name";

            case "test.code" ->
                    "test.code";

            case "organization.name" ->
                    "organization.name";

            default -> throw new IllegalArgumentException(
                    "Invalid sort field: " + sort
            );
        };
    }

    private Sort.Direction parseSortDirection(
            String direction
    ) {

        if (direction == null || direction.isBlank()) {
            return Sort.Direction.ASC;
        }

        return switch (direction.trim().toLowerCase()) {

            case "asc" ->
                    Sort.Direction.ASC;

            case "desc" ->
                    Sort.Direction.DESC;

            default -> throw new IllegalArgumentException(
                    "Invalid sort direction. Use 'asc' or 'desc'"
            );
        };
    }
}
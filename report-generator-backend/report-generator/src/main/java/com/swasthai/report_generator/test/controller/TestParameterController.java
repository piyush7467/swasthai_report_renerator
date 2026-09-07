package com.swasthai.report_generator.test.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.test.dto.request.CreateTestParameterRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestParameterRequest;
import com.swasthai.report_generator.test.dto.response.TestParameterResponse;
import com.swasthai.report_generator.test.service.TestParameterService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class TestParameterController {

    private final TestParameterService testParameterService;

    // ============================================================
    // CREATE SINGLE PARAMETER
    // ============================================================

    @PostMapping("/tests/{testRefId}/parameters")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<TestParameterResponse> createParameter(
            @PathVariable String testRefId,
            @Valid @RequestBody CreateTestParameterRequest request
    ) {

        return ApiResponse.success(
                "Test parameter created successfully",
                testParameterService.createParameter(
                        testRefId,
                        request
                )
        );
    }

    // ============================================================
    // CREATE PARAMETERS IN BULK
    // ============================================================

    @PostMapping("/tests/{testRefId}/parameters/bulk")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<TestParameterResponse>> createParametersBulk(
            @PathVariable String testRefId,
            @RequestBody List<@Valid CreateTestParameterRequest> requests
    ) {

        return ApiResponse.success(
                "Test parameters created successfully",
                testParameterService.createParametersBulk(
                        testRefId,
                        requests
                )
        );
    }

    // ============================================================
    // GET ALL PARAMETERS FOR A TEST
    // ============================================================

    @GetMapping("/tests/{testRefId}/parameters")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'LAB_STAFF')"
    )
    public ApiResponse<Page<TestParameterResponse>> getTestParameters(

            @PathVariable String testRefId,

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

            @RequestParam(defaultValue = "displayOrder")
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
                "Test parameters retrieved successfully",
                testParameterService.getTestParameters(
                        testRefId,
                        status,
                        pageable
                )
        );
    }

    // ============================================================
    // GET SINGLE PARAMETER
    // ============================================================

    @GetMapping("/test-parameters/{parameterRefId}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'LAB_STAFF')"
    )
    public ApiResponse<TestParameterResponse> getParameter(
            @PathVariable String parameterRefId
    ) {

        return ApiResponse.success(
                "Test parameter retrieved successfully",
                testParameterService.getParameter(
                        parameterRefId
                )
        );
    }

    // ============================================================
    // UPDATE PARAMETER
    // ============================================================

    @PatchMapping("/test-parameters/{parameterRefId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<TestParameterResponse> updateParameter(

            @PathVariable String parameterRefId,

            @Valid
            @RequestBody
            UpdateTestParameterRequest request
    ) {

        return ApiResponse.success(
                "Test parameter updated successfully",
                testParameterService.updateParameter(
                        parameterRefId,
                        request
                )
        );
    }

    // ============================================================
    // SOFT DELETE
    // ============================================================

    @DeleteMapping("/test-parameters/{parameterRefId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> deactivateParameter(
            @PathVariable String parameterRefId
    ) {

        testParameterService.deactivateParameter(
                parameterRefId
        );

        return ApiResponse.success(
                "Test parameter deactivated successfully",
                null
        );
    }

    // ============================================================
    // SAFE PAGINATION + SORTING
    // ============================================================

    private Pageable createPageable(
            int page,
            int size,
            String sort,
            String direction
    ) {

        String sortField = validateSortField(sort);

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

    private String validateSortField(String sort) {

        if (sort == null || sort.isBlank()) {
            return "displayOrder";
        }

        return switch (sort.trim()) {

            case "displayOrder" ->
                    "displayOrder";

            case "code" ->
                    "code";

            case "name" ->
                    "name";

            case "dataType" ->
                    "dataType";

            case "status" ->
                    "status";

            case "version" ->
                    "version";

            case "createdAt" ->
                    "createdAt";

            case "updatedAt" ->
                    "updatedAt";

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
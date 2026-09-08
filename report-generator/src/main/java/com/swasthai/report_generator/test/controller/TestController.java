package com.swasthai.report_generator.test.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.test.dto.request.CreateTestRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestRequest;
import com.swasthai.report_generator.test.dto.response.TestResponse;
import com.swasthai.report_generator.test.service.TestService;
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
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
@Validated
public class TestController {

    private final TestService testService;

    /**
     * Create one master test.
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<TestResponse> createTest(
            @Valid @RequestBody CreateTestRequest request
    ) {

        TestResponse response = testService.createTest(request);

        return ApiResponse.success(
                "Test created successfully",
                response
        );
    }

    /**
     * Temporary bulk creation endpoint.
     *
     * Maximum 100 tests per request.
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<TestResponse>> createTestsBulk(
            @RequestBody List<@Valid CreateTestRequest> requests
    ) {

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one test is required"
            );
        }

        if (requests.size() > 100) {
            throw new IllegalArgumentException(
                    "Maximum 100 tests can be created in one request"
            );
        }

        List<TestResponse> response =
                testService.createTestsBulk(requests);

        return ApiResponse.success(
                "Tests created successfully",
                response
        );
    }

    /**
     * Get a single test by public reference ID.
     */
    @GetMapping("/{refId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<TestResponse> getTest(
            @PathVariable String refId
    ) {

        return ApiResponse.success(
                "Test retrieved successfully",
                testService.getTest(refId)
        );
    }

    /**
     * Search/filter/paginate master tests.
     *
     * Example:
     *
     * GET /api/v1/tests
     *
     * GET /api/v1/tests?search=blood
     *
     * GET /api/v1/tests?categoryRefId=TC-xxxx
     *
     * GET /api/v1/tests?status=ACTIVE
     *
     * GET /api/v1/tests?search=blood&status=ACTIVE
     *
     * GET /api/v1/tests?page=0&size=20&sort=name&direction=asc
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Page<TestResponse>> getTests(

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            String categoryRefId,

            @RequestParam(required = false)
            String status,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be greater than or equal to 0")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100")
            int size,

            @RequestParam(defaultValue = "name")
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
                "Tests retrieved successfully",
                testService.getTests(
                        search,
                        categoryRefId,
                        status,
                        pageable
                )
        );
    }

    /**
     * Update test.
     */
    @PatchMapping("/{refId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<TestResponse> updateTest(

            @PathVariable String refId,

            @Valid @RequestBody UpdateTestRequest request
    ) {

        return ApiResponse.success(
                "Test updated successfully",
                testService.updateTest(refId, request)
        );
    }

    /**
     * Soft delete test.
     */
    @DeleteMapping("/{refId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> deleteTest(
            @PathVariable String refId
    ) {

        testService.deleteTest(refId);

        return ApiResponse.success(
                "Test deleted successfully",
                null
        );
    }

    /**
     * Creates a safe Pageable.
     *
     * Only explicitly allowed database properties
     * can be used for sorting.
     */
    private Pageable createPageable(
            int page,
            int size,
            String sort,
            String direction
    ) {

        String sortField = validateSortField(sort);

        Sort.Direction sortDirection =
                parseSortDirection(direction);

        Sort sortObject =
                Sort.by(sortDirection, sortField);

        return PageRequest.of(
                page,
                size,
                sortObject
        );
    }

    /**
     * Prevents arbitrary property/path injection
     * through the sort parameter.
     */
    private String validateSortField(String sort) {

        if (sort == null || sort.isBlank()) {
            return "name";
        }

        return switch (sort.trim()) {

            case "name" -> "name";

            case "code" -> "code";

            case "shortName" -> "shortName";

            case "createdAt" -> "createdAt";

            case "updatedAt" -> "updatedAt";

            case "displayOrder" -> "displayOrder";

            case "basePrice" -> "basePrice";

            case "turnaroundTimeHours" ->
                    "turnaroundTimeHours";

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

            case "asc" -> Sort.Direction.ASC;

            case "desc" -> Sort.Direction.DESC;

            default -> throw new IllegalArgumentException(
                    "Invalid sort direction. Use 'asc' or 'desc'"
            );
        };
    }
}
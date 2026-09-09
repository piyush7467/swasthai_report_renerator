package com.swasthai.report_generator.test.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.common.response.PagedResponse;
import com.swasthai.report_generator.test.dto.request.CreateTestCategoryRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestCategoryRequest;
import com.swasthai.report_generator.test.dto.response.TestCategoryResponse;
import com.swasthai.report_generator.test.entity.TestCategoryStatus;
import com.swasthai.report_generator.test.service.TestCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test-categories")
@RequiredArgsConstructor
public class TestCategoryController {

    private final TestCategoryService testCategoryService;

    /**
     * Create a new test category.
     * Only SUPER_ADMIN can create master catalog categories.
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TestCategoryResponse>> createCategory(
            @Valid @RequestBody CreateTestCategoryRequest request) {

        TestCategoryResponse response = testCategoryService.createCategory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Test category created successfully",
                        response));
    }

    /**
     * Get test categories with filtering, search, sorting, and pagination.
     * Accessible to any authenticated user.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<TestCategoryResponse>>> getCategories(
            @RequestParam(required = false) TestCategoryStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        PagedResponse<TestCategoryResponse> response = testCategoryService.getCategories(
                status,
                search,
                page,
                size,
                sortBy,
                sortDirection
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Test categories retrieved successfully",
                        response));
    }

    /**
     * Get a category by public refId.
     */
    @GetMapping("/{refId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TestCategoryResponse>> getCategory(
            @PathVariable String refId) {

        TestCategoryResponse response = testCategoryService.getCategory(refId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Test category retrieved successfully",
                        response));
    }

    /**
     * Update a test category.
     * Only SUPER_ADMIN can modify master catalog categories.
     */
    @PatchMapping("/{refId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TestCategoryResponse>> updateCategory(
            @PathVariable String refId,
            @Valid @RequestBody UpdateTestCategoryRequest request) {

        TestCategoryResponse response = testCategoryService.updateCategory(refId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Test category updated successfully",
                        response));
    }

    /**
     * Soft-delete a test category (ACTIVE -> INACTIVE).
     * Only SUPER_ADMIN can delete master catalog categories.
     */
    @DeleteMapping("/{refId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable String refId) {

        testCategoryService.deleteCategory(refId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Test category deleted successfully",
                        null));
    }

    /**
     * Reactivate an inactive test category (INACTIVE -> ACTIVE).
     * Only SUPER_ADMIN can reactivate master catalog categories.
     */
    @PostMapping("/{refId}/reactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TestCategoryResponse>> reactivateCategory(
            @PathVariable String refId) {

        TestCategoryResponse response = testCategoryService.reactivateCategory(refId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Test category reactivated successfully",
                        response));
    }

}
package com.swasthai.report_generator.report.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.report.dto.request.AddReportTestRequest;
import com.swasthai.report_generator.report.dto.request.CreateReportRequest;
import com.swasthai.report_generator.report.dto.request.ReorderReportTestsRequest;
import com.swasthai.report_generator.report.dto.request.UpdateReportParametersRequest;
import com.swasthai.report_generator.report.dto.response.ReportResponse;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.service.ReportService;
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
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Validated
public class ReportController {

    private final ReportService reportService;

    // ============================================================
    // 1. CREATE DRAFT REPORT
    // ============================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'LAB_STAFF')")
    public ApiResponse<ReportResponse> createReport(
            @Valid @RequestBody CreateReportRequest request
    ) {
        return ApiResponse.success(
                "Report draft created successfully",
                reportService.createReport(request)
        );
    }

    // ============================================================
    // 2. GET REPORT BY REF ID
    // ============================================================

    @GetMapping("/{reportRefId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'LAB_STAFF')")
    public ApiResponse<ReportResponse> getReport(
            @PathVariable String reportRefId
    ) {
        return ApiResponse.success(
                "Report retrieved successfully",
                reportService.getReport(reportRefId)
        );
    }

    // ============================================================
    // 3. GET MY ORGANIZATION REPORTS
    // ============================================================

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'LAB_STAFF')")
    public ApiResponse<Page<ReportResponse>> getMyReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeSort = switch (sort) {
            case "createdAt", "updatedAt", "status", "reportVersion" -> sort;
            default -> "createdAt";
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, safeSort));
        return ApiResponse.success(
                "Reports retrieved successfully",
                reportService.getMyReports(status, pageable)
        );
    }

    // ============================================================
    // 4. ADD TEST TO REPORT DRAFT
    // ============================================================

    @PostMapping("/{reportRefId}/tests")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'LAB_STAFF')")
    public ApiResponse<ReportResponse> addTest(
            @PathVariable String reportRefId,
            @Valid @RequestBody AddReportTestRequest request
    ) {
        return ApiResponse.success(
                "Test added to report successfully",
                reportService.addTest(reportRefId, request)
        );
    }

    // ============================================================
    // 5. REMOVE TEST FROM REPORT DRAFT
    // ============================================================

    @DeleteMapping("/{reportRefId}/tests/{reportTestRefId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'LAB_STAFF')")
    public ApiResponse<ReportResponse> removeTest(
            @PathVariable String reportRefId,
            @PathVariable String reportTestRefId
    ) {
        return ApiResponse.success(
                "Test removed from report successfully",
                reportService.removeTest(reportRefId, reportTestRefId)
        );
    }

    // ============================================================
    // 6. UPDATE PARAMETER VALUES / AUTOSAVE
    // ============================================================

    @PatchMapping("/{reportRefId}/tests/{reportTestRefId}/parameters")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'LAB_STAFF')")
    public ApiResponse<ReportResponse> updateParameterValues(
            @PathVariable String reportRefId,
            @PathVariable String reportTestRefId,
            @Valid @RequestBody UpdateReportParametersRequest request
    ) {
        return ApiResponse.success(
                "Parameter values updated successfully",
                reportService.updateParameterValues(reportRefId, reportTestRefId, request)
        );
    }

    // ============================================================
    // 7. REORDER TESTS
    // ============================================================

    @PatchMapping("/{reportRefId}/tests/reorder")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'LAB_STAFF')")
    public ApiResponse<ReportResponse> reorderTests(
            @PathVariable String reportRefId,
            @Valid @RequestBody ReorderReportTestsRequest request
    ) {
        return ApiResponse.success(
                "Tests reordered successfully",
                reportService.reorderTests(reportRefId, request)
        );
    }

    // ============================================================
    // 8. FINALIZE REPORT
    // ============================================================

    @PostMapping("/{reportRefId}/finalize")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'LAB_STAFF')")
    public ApiResponse<ReportResponse> finalizeReport(
            @PathVariable String reportRefId
    ) {
        return ApiResponse.success(
                "Report finalized successfully",
                reportService.finalizeReport(reportRefId)
        );
    }
}
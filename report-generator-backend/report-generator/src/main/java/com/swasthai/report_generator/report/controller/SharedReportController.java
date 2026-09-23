package com.swasthai.report_generator.report.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.report.dto.response.SharedReportResponse;
import com.swasthai.report_generator.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shared/reports")
@RequiredArgsConstructor
public class SharedReportController {

    private final ReportService reportService;

    @GetMapping("/{shareToken}")
    public ApiResponse<SharedReportResponse> getSharedReport(
            @PathVariable String shareToken) {
        return ApiResponse.success(
                "Shared report retrieved successfully",
                reportService.getSharedReport(shareToken));
    }

    @GetMapping("/{shareToken}/pdf")
    public ResponseEntity<byte[]> getSharedReportPdf(
            @PathVariable String shareToken) {
        byte[] pdfBytes = reportService.getSharedReportPdf(shareToken);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"shared-report.pdf\"")
                .body(pdfBytes);
    }
}

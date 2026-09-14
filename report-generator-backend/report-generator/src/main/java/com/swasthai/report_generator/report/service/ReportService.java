package com.swasthai.report_generator.report.service;

import com.swasthai.report_generator.report.dto.request.AddReportTestRequest;
import com.swasthai.report_generator.report.dto.request.BulkDeleteReportsRequest;
import com.swasthai.report_generator.report.dto.request.CreateReportRequest;
import com.swasthai.report_generator.report.dto.request.DeleteReportsByDateRangeRequest;
import com.swasthai.report_generator.report.dto.request.ReorderReportTestsRequest;
import com.swasthai.report_generator.report.dto.request.UpdateReportParametersRequest;
import com.swasthai.report_generator.report.dto.response.BulkDeleteReportsResponse;
import com.swasthai.report_generator.report.dto.response.DeleteReportResponse;
import com.swasthai.report_generator.report.dto.response.ReportResponse;
import com.swasthai.report_generator.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    ReportResponse createReport(CreateReportRequest request);

    ReportResponse getReport(String reportRefId);

    Page<ReportResponse> getMyReports(ReportStatus status, Pageable pageable);

    ReportResponse addTest(String reportRefId, AddReportTestRequest request);

    ReportResponse removeTest(String reportRefId, String reportTestRefId);

    ReportResponse updateParameterValues(
            String reportRefId,
            String reportTestRefId,
            UpdateReportParametersRequest request);

    ReportResponse reorderTests(String reportRefId, ReorderReportTestsRequest request);

    ReportResponse finalizeReport(String reportRefId);

    DeleteReportResponse deleteReport(String reportRefId);

    BulkDeleteReportsResponse deleteReports(BulkDeleteReportsRequest request);

    BulkDeleteReportsResponse deleteReportsByDateRange(DeleteReportsByDateRangeRequest request);

    int purgeExpiredReports();

    ReportResponse breakGlassAccess(
            String reportRefId,
            com.swasthai.report_generator.report.dto.request.BreakGlassAccessRequest request,
            String clientIp);

    byte[] generateReportPdf(String reportRefId);

    com.swasthai.report_generator.report.pdf.ReportPdfData getReportPdfData(String reportRefId);
}
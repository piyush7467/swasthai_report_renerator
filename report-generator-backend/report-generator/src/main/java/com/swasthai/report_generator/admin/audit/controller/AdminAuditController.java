package com.swasthai.report_generator.admin.audit.controller;

import com.swasthai.report_generator.admin.audit.dto.SecurityAuditLogResponse;
import com.swasthai.report_generator.admin.audit.service.AdminAuditService;
import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping("/security")
    public ApiResponse<PageResponse<SecurityAuditLogResponse>> getSecurityAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetOrganizationRefId,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        return ApiResponse.success(
                "Security audit logs retrieved successfully",
                adminAuditService.getSecurityAuditLogs(
                        action,
                        targetOrganizationRefId,
                        success,
                        from,
                        to,
                        page,
                        size,
                        sort,
                        direction
                )
        );
    }
}
